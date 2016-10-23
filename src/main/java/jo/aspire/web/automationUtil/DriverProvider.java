package jo.aspire.web.automationUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jbehave.web.selenium.DelegatingWebDriverProvider;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.appium.java_client.android.AndroidDriver;

public class DriverProvider extends DelegatingWebDriverProvider {
	private RemoteWebDriver driver;
	private static String browser;
	public static HashMap<Long, WebDriver> allThreads = new HashMap<Long, WebDriver>();
	private static JsonObject driverInfo;
	private DesiredCapabilities capabilities = new DesiredCapabilities();
	
	public static void setDriverToRun(JsonObject di) {
		driverInfo = di;
	}

	@Override
	public void initialize() {
		boolean isRemote = Boolean.parseBoolean(driverInfo.get("isRemote").getAsString());

		String remoteUrl = "";

		// set capabilities
		JsonObject capabilitiesObj = driverInfo.get("capabilities").getAsJsonObject();
		Set<Map.Entry<String, JsonElement>> capabilitiesSet = capabilitiesObj.entrySet();

		for (Map.Entry<String, JsonElement> entry : capabilitiesSet) {
			capabilities.setCapability(entry.getKey(), entry.getValue());
		}

		if (isRemote) {
			remoteUrl = driverInfo.get("remote").getAsJsonObject().get("remoteUrl").getAsString();

			try {
				this.driver = new RemoteWebDriver(new URL(remoteUrl), capabilities);
				this.driver.setFileDetector(new LocalFileDetector());
				allThreads.put(Thread.currentThread().getId(), this.driver);
			} catch (Exception e) {
				e.printStackTrace();
			}
			delegate.set((WebDriver) driver);
		} else {
			browser = driverInfo.get("name").getAsString().toLowerCase();
			WebDriver webDriver = createDriver();
			allThreads.put(Thread.currentThread().getId(), webDriver);
			delegate.set(webDriver);
		}

		// hash map for thread per driver

	}

	private WebDriver createDriver() {
		if (Boolean.parseBoolean(driverInfo.get("isMobile").getAsString())) {
			return createAndroidBrowser();
		} else {
			if (browser.equals("phantom")) {
				return createPhantomDrive();
				// } else if (browser.equals("htmlUnit")) {
				// return createHtmlUnitDriver();
			} else if (browser.equals("chrome")) {
				return createChromeDriver();
			} else if (browser.equals("ie32")) {
				return createInternetExplorerDriver32();
			} else if (browser.equals("ie64")) {
				return createInternetExplorerDriver64();
			} else if (browser.equals("safari")) {
				return createSafariDriver();
//			} else if (browser.equals("remote")) {
//				return createRemoteDriver();
			} else if (browser.equals("edge")) {
				return createEdgeDriver();
			} else {
				return createFirefoxDriver();
			}

		}
	}

	protected WebDriver createEdgeDriver() {
		System.setProperty("webdriver.edge.driver",
				System.getProperty("user.dir") + "/webdrivers/edge/MicrosoftWebDriver.exe");
		EdgeDriver driver = new EdgeDriver();

		return driver;
	}

	protected WebDriver createAndroidBrowser() {
		
		URL serverAddress = null;
		try {
			serverAddress = new URL("http://127.0.0.1:" + "4723" + "/wd/hub");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		AndroidDriver driver = new AndroidDriver(serverAddress, capabilities);
		return driver;
	}

//	protected RemoteWebDriver createRemoteDriver() {
//		DesiredCapabilities cap = null;
//		if (browser.equals("chrome")) {
//			cap = DesiredCapabilities.chrome();
//		} else {
//			cap = DesiredCapabilities.firefox();
//		}
//		try {
//			return new RemoteWebDriver(new URL(driverInfo.get("remote").getAsJsonObject().get("remoteUrl").getAsString()), cap);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	protected ChromeDriver createChromeDriver() {
		if (OSValidator.isMac()) {

			System.setProperty("webdriver.chrome.driver",
					System.getProperty("user.dir") + "/webdrivers/chrome/mac/chromedriver");
		} else if (OSValidator.isWindows()) {

			System.setProperty("webdriver.chrome.driver",
					System.getProperty("user.dir") + "/webdrivers/chrome/win/chromedriver.exe");
		} else if (OSValidator.isUnix()) {

			System.setProperty("webdriver.chrome.driver",
					System.getProperty("user.dir") + "/webdrivers/chrome/linux/chromedriver");
		}
		ChromeOptions options = new ChromeOptions();
		
		
		// set arguments
		if (!driverInfo.get("arguments").isJsonNull()){
			JsonArray arguments = driverInfo.get("arguments").getAsJsonArray();
			for (JsonElement arg : arguments) {
				options.addArguments(arg.getAsString());
			}
		}
		
		
		HashMap<String, Object> chromePrefs = new HashMap<String, Object>();

//		chromePrefs.put("profile.default_content_settings.popups", 0);
//		chromePrefs.put("profile.default_content_setting_values.notifications", 2);
		
		// set preferences
		if (!driverInfo.get("preferences").isJsonNull()){			
			JsonObject preferencesObj = driverInfo.get("preferences").getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> preferencesSet = preferencesObj.entrySet();

			for (Map.Entry<String, JsonElement> entry : preferencesSet) {
				chromePrefs.put(entry.getKey(), entry.getValue());
			}
		}
		
//		chromePrefs.put("download.default_directory", System.getProperty("user.dir") + File.separator + "Temp");
		
		options.setExperimentalOption("prefs", chromePrefs);
		
//		DesiredCapabilities cap = DesiredCapabilities.chrome();
		
		boolean isProxy = Boolean.parseBoolean(driverInfo.get("local").getAsJsonObject().get("isProxy").getAsString());
		
		if (isProxy) {
			String proxyHost = driverInfo.get("local").getAsJsonObject().get("proxy").getAsJsonObject().get("proxyHost").getAsString();
			Integer proxyPort = Integer.parseInt(driverInfo.get("local").getAsJsonObject().get("proxy").getAsJsonObject().get("proxyPort").getAsString());

			addProxyCapabilities(capabilities, proxyHost, proxyPort);
		}

//		cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
//		cap.setCapability(ChromeOptions.CAPABILITY, options);

		return new FixedChromeDriver(capabilities);
	}

	protected PhantomJSDriver createPhantomDrive() {

//		DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
		if (OSValidator.isMac()) {
			capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
					System.getProperty("user.dir") + "/webdrivers/phantom/mac/phantomjs");
		} else if (OSValidator.isWindows()) {

			capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
					System.getProperty("user.dir") + "/webdrivers/phantom/win/phantomjs.exe");
		} else if (OSValidator.isUnix()) {
			capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
					System.getProperty("user.dir") + "/webdrivers/phantom/linux/phantomjs");
		}
		
//		String[] phantomArgs = new String[] { "--webdriver-loglevel=NONE" };
//		capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, phantomArgs);
		Logger.getLogger(PhantomJSDriverService.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(PhantomJSDriverService.class.getName()).setLevel(Level.OFF);
//		capabilities.setCapability("JavascriptEnabled", true);
//		capabilities.setCapability("cssSelectorsEnabled", true);
//		capabilities.setCapability("applicationCacheEnabled", false);
//		capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
//		capabilities.setCapability(CapabilityType.SUPPORTS_ALERTS, true);
//		capabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
		return new PhantomJSDriver(capabilities);
	}

	protected FirefoxDriver createFirefoxDriver() {
		FirefoxProfile firefoxProfile = new FirefoxProfile();

//		String path = System.getProperty("user.dir") + File.separator + "Temp";
//		firefoxProfile.setPreference("browser.download.folderList", 2);
//		firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
//		firefoxProfile.setPreference("browser.download.dir", path);
//		firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk",
//				"application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, text/comma-separated-values, text/csv, application/csv, application/excel, application/vnd.msexcel, text/anytext , application/x-xpinstall,application/x-zip,application/x-zip-compressed,application/octet-stream,application/zip,application/pdf,application/msword,text/plain,application/octet,text/html, application/x-csv, text/x-csv , application/msexcel,binary/octet-stream , text/html ,application/xhtml+xml,application/xml,application/json");
//		if (PlatformInformation.isProxy) {
//			firefoxProfile.setPreference("network.proxy.type", 1);
//			firefoxProfile.setPreference("network.proxy.http", PlatformInformation.proxyHost);
//			firefoxProfile.setPreference("network.proxy.http_port", PlatformInformation.proxyPort);
//			firefoxProfile.setPreference("network.proxy.ssl", PlatformInformation.proxyHost);
//			firefoxProfile.setPreference("network.proxy.ssl_port", PlatformInformation.proxyPort);
//		}
		
		// set preferences
		if (!driverInfo.get("preferences").isJsonNull()){			
			JsonObject preferencesObj = driverInfo.get("preferences").getAsJsonObject();
			Set<Map.Entry<String, JsonElement>> preferencesSet = preferencesObj.entrySet();
			for (Map.Entry<String, JsonElement> entry : preferencesSet) {
				firefoxProfile.setPreference(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		
		return new FixedFirefoxDriver(firefoxProfile);
	}

	protected InternetExplorerDriver createInternetExplorerDriver32() {
		System.setProperty("webdriver.ie.driver",
				System.getProperty("user.dir") + "/webdrivers/ie32/IEDriverServer.exe");
		return new InternetExplorerDriver();
	}

	protected InternetExplorerDriver createInternetExplorerDriver64() {
		System.setProperty("webdriver.ie.driver",
				System.getProperty("user.dir") + "/webdrivers/ie64/IEDriverServer.exe");

		return new InternetExplorerDriver();
	}

	protected SafariDriver createSafariDriver() {
		return new SafariDriver();
	}

	private DesiredCapabilities addProxyCapabilities(DesiredCapabilities capabilities, String zapProxyHost, int zapProxyPort) {
		Proxy proxy = new Proxy();
		proxy.setHttpProxy(zapProxyHost + ":" + zapProxyPort);
		proxy.setSslProxy(zapProxyHost + ":" + zapProxyPort);
		capabilities.setCapability("proxy", proxy);
		return capabilities;
	}

}
