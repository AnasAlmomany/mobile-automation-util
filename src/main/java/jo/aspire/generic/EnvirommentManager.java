package jo.aspire.generic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FilenameUtils;

public class EnvirommentManager {
	private Properties defaultProps = new Properties();
	private Properties appProps = null;
	public static String platformVersion = "5.0";

	private Hashtable<String, ArrayList<PropertyChangeListener>> listeners = null;
	private static Class<?> initialClass;
	private static Object lock = new Object();
	private static EnvirommentManager instance = null;
	private String PropertiesLocalisaztion = "";
	private EnvirommentManager() {
	}

	public static void setInitialClass(Class<?> parInitialClass) {
		initialClass = parInitialClass;
	}

	public static EnvirommentManager getInstance() {
		try {
			if (instance == null) {
				synchronized (lock) {
					if (instance == null) {
						instance = new EnvirommentManager();
						instance.loadProperties();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (instance);

	}
	
	private void LoadMainConfig(String path){		
		Properties prop = new Properties();
		InputStream MainProperty = null;		
		try {
			
			MainProperty = new FileInputStream(new File(path + File.separator
					+ "MainConfig.properties"));

			// load a properties file
			prop.load(MainProperty);

			// get the property value and print it out
			PropertiesLocalisaztion =prop.getProperty("local");		
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (MainProperty != null) {
				try {
					MainProperty.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
			
	}

	private void loadProperties() throws IOException {
		List<String> files = new ArrayList<String>();
		String path = System.getProperty("user.dir") + File.separator + "src"
				+ File.separator + "test" + File.separator + "resources"
				+ File.separator + "configs";
		LoadMainConfig(path);

		final File jarFile = new File(initialClass.getProtectionDomain()
				.getCodeSource().getLocation().getPath());
		System.out.println("Jar path is:" + jarFile);
		if (jarFile.isFile()) { // Run with JAR file
			System.out.println("iam jar");
			final JarFile jar = new JarFile(jarFile);
			final Enumeration<JarEntry> entries = jar.entries(); // gives ALL
																	// entries
																	// in jar

			while (entries.hasMoreElements()) {

				final String name = entries.nextElement().getName();
				if (name.toLowerCase().endsWith((PropertiesLocalisaztion != "" ? ".":"") + PropertiesLocalisaztion+".properties")
						&& name.toLowerCase().contains((PropertiesLocalisaztion != "" ? ".":"") +PropertiesLocalisaztion+".configs")) { // filter
																		// according
//					this.getClass().getClassLoader()
//					.getResourceAsStream(name)										// to
																		// the
																		// path
					String currenetName = name
							.substring(name.lastIndexOf(File.separator) + 1,
									name.length());
					defaultProps.load(this.getClass().getClassLoader()
							.getResourceAsStream(name));
					// create application properties with default
					appProps = new Properties(defaultProps);
				}
			}
			jar.close();
		} else { // Run with IDE
			System.out.println("iam not jar");
			File directory = new File(path);

			// get all the files from a directory
			File[] fList = directory.listFiles();
			for (File file : fList) {
				if (file.isFile()
						&& FilenameUtils.getName(file.getPath()).endsWith(
								(!PropertiesLocalisaztion.equals("") ?  ".": "") +PropertiesLocalisaztion+".properties")) {
					files.add(file.getPath());

					// create and load default properties
					FileInputStream in = new FileInputStream(
							file.getAbsolutePath());
					defaultProps.load(in);
					in.close();

					// create application properties with default
					appProps = new Properties(defaultProps);

					try {
						// user/application properties
						in = new FileInputStream(file.getAbsolutePath());
						appProps.load(in);
						in.close();
					} catch (Throwable th) {
					}
				}
			}
		}

	}

	public String getProperty(String key) {
		String val = null;
		if (key != null) {
			if (appProps != null)
				val = (String) appProps.getProperty(key);
			if (val == null) {
				val = defaultProps.getProperty(key);
			}
		}
		return (val);

	}
	
	public boolean getBoolean(String string) {
		return Boolean.parseBoolean(getProperty(string));
	}
	
	/**
	 * Sets Application/User String properties; default property values cannot
	 * be set.
	 */
	public void setProperty(String key, String val) {

		ArrayList<?> list = null;
		Object oldValue = null;

		oldValue = getProperty(key);

		appProps.setProperty(key, val);
		if (listeners.containsKey(key)) {
			list = (ArrayList<?>) listeners.get(key);
			int len = list.size();
			if (len > 0) {
				PropertyChangeEvent evt = new PropertyChangeEvent(this, key,
						oldValue, val);
				for (int i = 0; i < len; i++) {
					if (list.get(i) instanceof PropertyChangeListener)
						((PropertyChangeListener) list.get(i))
								.propertyChange(evt);
				}
			}
		}

	}

	public boolean addListener(String key, PropertyChangeListener listener) {
		boolean added = false;
		ArrayList<PropertyChangeListener> list = null;
		if (listeners == null)
			listeners = new Hashtable<String, ArrayList<PropertyChangeListener>>();

		if (!listeners.contains(key)) {
			list = new ArrayList<PropertyChangeListener>();
			added = list.add(listener);
			listeners.put(key, list);
		} else {
			list = (ArrayList<PropertyChangeListener>) listeners.get(key);
			added = list.add(listener);
		}
		return (added);
	}

	public void removeListener(PropertyChangeListener listener) {
		if (listeners != null && listeners.size() > 0)
			listeners.remove(listener);
	}
}
