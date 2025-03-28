/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/25/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client;

import javax.swing.SwingUtilities;

import flaggi.client.constants.Constants;
import flaggi.client.ui.MenuBackground;
import flaggi.client.ui.MenuScreen;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ScreenUtil;

public class App {

	private final GPanel gpanel;

	// Main ---------------------------------------------------------------------

	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	public App() {
		initializeLogger();
		this.gpanel = getDefaultGpanel();
		addDefaultWidgets();

		this.gpanel.toggleWidgetsVisibility(true); // TODO DEBUG
	}

	// Events -------------------------------------------------------------------

	public String joinServer(String name, String ip) {
		Logger.log(LogLevel.DEBUG, "Join server button pressed.");
		Constants.CONFIG.setField("username", name);
		Constants.CONFIG.setField("server.ip", ip);
		return "Connecting...";
	}

	// Private ------------------------------------------------------------------

	private void initializeLogger() {
		Logger.setLogFile(Constants.LOG_FILE);
		Logger.setLogLevelsToIgnore(LogLevel.DEBUG, LogLevel.TRACE);
		Logger.log(LogLevel.INFO, "Application start.");
		if (Constants.LOG_MEM_USAGE) {
			Logger.logMemoryUsage(Constants.MEM_LOG_INTERVAL_SEC);
		}
	}

	private GPanel getDefaultGpanel() {
		int[] screenSize = ScreenUtil.getScreenDimensions();
		GPanel gp = new GPanel(screenSize[0], screenSize[1], Constants.WINDOW_RESIZABLE, Constants.WINDOW_NAME);
		if (Constants.FRAMERATE >= 0) {
			gp.setFpsCap(Constants.FRAMERATE);
		}
		gp.setIconOSDependend(Constants.ICON_WIN, Constants.ICON_MAC, Constants.ICON_WIN, Constants.ICON_WIN);
		return gp;
	}

	private void addDefaultWidgets() {
		this.gpanel.add( //
				new MenuScreen(Constants.MENU_NAME_FIELD, Constants.MENU_IP_FIELD, this::joinServer), //
				new MenuBackground() //
		);
	}

}
