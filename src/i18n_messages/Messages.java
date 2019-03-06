package i18n_messages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Messages {

	/* uncomment if need to use internal properties file
	 * private static final String BUNDLE_NAME = "i18n_messages.rcl_messages";
	 * 
	 * private static final ResourceBundle RESOURCE_BUNDLE =
	 * ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
	 * 
	 * private static RCLBundle bundle = new RCLBundle(RESOURCE_BUNDLE);
	 * 
	 * public static String get(String key, String... values) { return
	 * bundle.get(key, values); }
	 */

	// external jar proprieties file
	private static final String BUNDLE_NAME = "config\\rcl_messages_en.properties";
	private static ResourceBundle RESOURCE_BUNDLE;
	private static RCLBundle bundle;

	public static String get(String key, String... values) {

		// interal proprieties file
		// return bundle.get(key, values);

		// external proprieties file
		try {
			RESOURCE_BUNDLE = new PropertyResourceBundle(Files.newInputStream(Paths.get(BUNDLE_NAME)));
			bundle = new RCLBundle(RESOURCE_BUNDLE);
			return bundle.get(key, values);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
