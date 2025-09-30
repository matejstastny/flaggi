// ------------------------------------------------------------------------------
// Constants.java - Server constants class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 02-22-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.constants;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import flaggi.server.Server;
import flaggi.shared.common.ConfigManager;
import flaggi.shared.common.Logger;
import flaggi.shared.common.ConfigManager.FieldFormatException;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FileUtil;

/**
 * A constants class storing all constants for this project.
 */
public class Constants {

	// Private constructor to prevent instantiation
	private Constants() {
		throw new UnsupportedOperationException("Constants is a constants class and cannot be instantiated.");
	}

	// Paths --------------------------------------------------------------------

	public static final String APP_DATA_DIR_NAME = "kireiiiiiiii.flaggi.server";
	public static final String LOG_FILE = String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "logs", "latest.txt");
	public static final ConfigManager CONFIG = getConfigManager();

	/**
	 * Resources that will be created at server start in the directory the sever
	 * lives. The map is in "/resource/path" "new relative path"
	 */
	public static Map<String, String> SERVER_RESOURCES = getServerResources();

	// Resources ----------------------------------------------------------------

	public static final String MAPS_RES_DIR = "maps";

	// Debug --------------------------------------------------------------------

	public static LogLevel[] IGNORED_LOG_LEVES = { LogLevel.TCP };

	// Network ------------------------------------------------------------------

	public static final int TCP_PORT = getConfigIntValue("tcp.port");
	public static final int UDP_PORT = getConfigIntValue("udp.port");

	// Other --------------------------------------------------------------------

	public static final int UPDATE_INTERVAL_MS = 16;
	public static final int SERVER_SHUTDOWN_TIMEOUT_SEC = 5;

	// Internal -----------------------------------------------------------------

	private static ConfigManager getConfigManager() {
		try {
			return new ConfigManager(String.join(File.separator, FileUtil.getJarExecDirectory(), "config.properties"), "/configs/config.properties");
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "An IOException occured when initializing the ConfigManager", e);
			Server.handleFatalError();
			return null;
		}
	}

	private static int getConfigIntValue(String key) {
		int val = -1;
		try {
			val = CONFIG.getIntValue(key);
		} catch (FieldFormatException e) {
			Logger.log(LogLevel.ERROR, "Configuration error: The field \"" + key + "\" is assigned a value of the wrong type. Expected: Integer.");
			Server.handleFatalError();
		}
		return val;
	}

	public static Map<String, String> getServerResources() {
		String serverDir = FileUtil.getJarExecDirectory() + File.separator;
		Map<String, String> resources = new HashMap<String, String>();
		resources.put("/licenses/LICENSE", System.getProperty("os.name").toLowerCase().contains("win") ? serverDir + "LICENSE.txt" : serverDir + "LICENSE");
		resources.put("/docker/Dockerfile", serverDir + "Dockerfile");
		resources.put("/scripts/run_docker.sh", serverDir + "run-docker.sh");
		resources.put("/scripts/run.sh", serverDir + "run.sh");
		return resources;
	}
}
