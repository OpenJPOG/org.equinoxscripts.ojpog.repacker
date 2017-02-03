
package org.equinoxscripts.ojpog.repacker.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class SharedUIOptions {
	private static SharedUIOptions options;

	public static SharedUIOptions instance() {
		if (options == null) {
			options = new SharedUIOptions();
			options.loadSafe();
		}
		return options;
	}

	private static File configLocation;

	private static File configLocation() {
		if (configLocation == null) {
			String userHome = System.getProperty("user.home");
			File rootConfigDir = null;
			if (userHome != null) {
				if (System.getProperty("os.name").toLowerCase().contains("win"))
					userHome += File.separator + "AppData" + File.separator + "Roaming";
				rootConfigDir = new File(userHome);
			}
			if (rootConfigDir == null || !rootConfigDir.canWrite())
				rootConfigDir = new File("./");
			if (!rootConfigDir.exists())
				rootConfigDir.mkdirs();
			configLocation = new File(rootConfigDir, "jpog_repacker.properties");
		}
		return configLocation;

	}

	public final Properties properties = new Properties();
	public static final String DEST_DIR = "destDir";
	public static final String SOURCE_DIR = "sourceDir";

	public void save() throws IOException {
		FileWriter writer = new FileWriter(configLocation());
		properties.store(writer, "JPOG Properties");
		writer.close();
	}

	public void load() throws IOException {
		File f = configLocation();
		if (!f.exists())
			return;
		FileReader reader = new FileReader(f);
		properties.clear();
		properties.load(reader);
		reader.close();
	}

	public void loadSafe() {
		try {
			load();
		} catch (IOException e) {
		}
	}

	public File file(String key) {
		String path = properties.getProperty(key);
		if (path != null)
			return new File(path);
		return null;
	}
}
