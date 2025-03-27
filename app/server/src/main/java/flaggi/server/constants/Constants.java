/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/22/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.constants;

import java.io.File;

import flaggi.shared.common.ConfigManager;
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
    public static final ConfigManager CONFIG = new ConfigManager(String.join(File.separator, FileUtil.getApplicationDataFolder(), Constants.APP_DATA_DIR_NAME, "configs", "config.properties"), "/configs/config.properties");

    // Network ------------------------------------------------------------------

    public static final int TCP_PORT = CONFIG.getIntValue("tcp.port");
    public static final int UDP_PORT = CONFIG.getIntValue("udp.port");

    // Update -------------------------------------------------------------------

    public static final int UPDATE_INTERVAL = 16;

}
