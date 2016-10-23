package jo.aspire.test;

import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;


public class RemoteWebDriverTest {
	public static void main(String[] args){
		 DesiredCapabilities cap =null;
		// if(PlatformInformation.remoteBrowserName.equals("chrome")){
		 cap = DesiredCapabilities.chrome();
		 //}else {
			// cap = DesiredCapabilities.firefox();
		 //}
			 WebDriver driver = null;
		 try {
			 driver =new RemoteWebDriver(new URL("http://192.168.99.100:4444/wd/hub"),cap);
		} catch (Exception e) {
			e.printStackTrace();
		
		}
		 
		 driver.get("http://www.aspire.jo");
		System.out.println( driver.getPageSource());
		 driver.quit();
	}

}
