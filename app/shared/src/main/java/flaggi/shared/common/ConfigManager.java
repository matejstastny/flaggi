/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/22/2025
 * Github link: https://github.com/kireiiiiiiii
 */

package flaggi.shared.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The {@code ConfigManager} class is responsible for loading and managing
 * application settings from a configuration file. It provides methods to
 * retrieve and set configuration values of various types such as integers,
 * booleans, strings, and doubles.
 * <p>
 * This class uses the {@link Properties} class to read key-value pairs from the
 * specified configuration file. If a key is not found or if there is an error
 * in parsing the value, an exception is thrown.
 * </p>
 * <p>
 * Example usage:
 *
 * <pre>
 * {@code
 * ConfigManager configManager = new ConfigManager("config.properties", "/configs/default-config.properties");
 * int timeout = configManager.getIntValue("timeout");
 * boolean isEnabled = configManager.getBooleanValue("enabled");
 * String username = configManager.getStringValue("username");
 * double threshold = configManager.getDoubleValue("threshold");
 * configManager.setField("timeout", "60");
 * }
 * </pre>
 * </p>
 * <p>
 * Note: This class is not thread-safe. If multiple threads access an instance
 * of this class concurrently, it must be synchronized externally.
 * </p>
 *
 */
public class ConfigManager {

	private final Properties properties;
	private final String configFilePath;
	private final String defaultConfigFile;

	// Constructor -------------------------------------------------------------

	public ConfigManager(String configFilePath, String defaultConfigFile) {
		this.configFilePath = configFilePath;
		this.defaultConfigFile = defaultConfigFile;
		properties = new Properties();
		try {
			loadOrInitializeConfigFile();
		} catch (IOException e) {
			System.err.println("Error handling configuration file: " + e.getMessage());
		}
	}

	// Nested Exception Class -------------------------------------------------

	public static class NonexistentFieldException extends RuntimeException {
		public NonexistentFieldException(String message) {
			super(message);
		}
	}

	// Accessors --------------------------------------------------------------

	public int getIntValue(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new NonexistentFieldException("Field not found: " + key);
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new NonexistentFieldException("Invalid integer format for key: " + key);
		}
	}

	public boolean getBooleanValue(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new NonexistentFieldException("Field not found: " + key);
		}
		return Boolean.parseBoolean(value);
	}

	public String getStringValue(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new NonexistentFieldException("Field not found: " + key);
		}
		return value;
	}

	public double getDoubleValue(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new NonexistentFieldException("Field not found: " + key);
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new NonexistentFieldException("Invalid double format for key: " + key);
		}
	}

	// Mutators ---------------------------------------------------------------

	public void setField(String key, String value) {
		properties.setProperty(key, value);
		try (FileOutputStream output = new FileOutputStream(this.configFilePath)) {
			properties.store(output, null);
		} catch (IOException e) {
			System.err.println("Error saving configuration file: " + e.getMessage());
		}
	}

	// Private Methods --------------------------------------------------------

	private void loadOrInitializeConfigFile() throws IOException {
		if (!loadConfigFile()) {
			createDefaultConfigFile();
		}
	}

	private boolean loadConfigFile() throws IOException {
		try (FileInputStream input = new FileInputStream(this.configFilePath)) {
			properties.load(input);
			return true;
		} catch (IOException e) {
			System.err.println("Configuration file not found, creating default configuration.");
			return false;
		}
	}

	private static void createConfFile(String path) throws IOException {
		File confFile = new File(path);
		if (!confFile.exists()) {
			new File(confFile.getParent()).mkdirs();
			confFile.createNewFile();
		}
	}

	private void createDefaultConfigFile() throws IOException {
		createConfFile(this.configFilePath);
		try (InputStream defaultConfigStream = getClass().getResourceAsStream(defaultConfigFile)) {
			if (defaultConfigStream == null) {
				System.err.println("Default configuration file not found in JAR.");
				return;
			}

			properties.load(defaultConfigStream);

			try (FileOutputStream output = new FileOutputStream(this.configFilePath)) {
				properties.store(output, null);
				System.out.println("Default configuration file created at " + this.configFilePath);
			}
		} catch (IOException e) {
			System.err.println("Error creating default configuration file at " + this.configFilePath);
			e.printStackTrace();
		}
	}

}
