/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/25/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.network.TcpManager;
import flaggi.client.ui.LobbyUi;
import flaggi.client.ui.MenuBackground;
import flaggi.client.ui.MenuScreen;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.NetUtil;
import flaggi.shared.util.ScreenUtil;

public class App {

	private final GPanel gpanel;
	private TcpManager tcpManager;

	// Main ---------------------------------------------------------------------

	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	public App() {
		initializeLogger();
		this.gpanel = getDefaultGpanel();
		addDefaultWidgets();
		gotoMainMenu();
	}

	// Static -------------------------------------------------------------------

	public static void handleFatalError() {
		Logger.log(LogLevel.ERROR, "FATAL ERROR DETECTED! SHUTTING DOWN...");
		System.exit(1);
	}

	// Events -------------------------------------------------------------------

	public void gotoMainMenu() {
		toggleUi(UiTags.MAIN_MENU);
	}

	public void gotoLobby() {
		toggleUi(UiTags.LOBBY);

		Map<Integer, String> clients = new HashMap<Integer, String>();
		for (int i = 0; i <= 40; i++) {
			clients.put(i, "Client:" + i);
		}
		this.gpanel.getWidgetsOfClass(LobbyUi.class).forEach(x -> x.setClients(clients));
		this.gpanel.toggleWidgetsVisibility(false);
		this.gpanel.toggleTaggedWidgetsVisibility(UiTags.LOBBY, true);
	}

	// External events ----------------------------------------------------------

	public String joinServer(String name, String ip) {
		Logger.log(LogLevel.DEBUG, "Join server button pressed.");
		setConfigField("username", name);
		setConfigField("server.ip", ip);
		if (!NetUtil.isValidAddress(ip)) {
			return "Invalid IP address fotmat!";
		} else if (!TcpManager.isFlaggiServer(ip.split(":")[0], Integer.parseInt(ip.split(":")[1]))) {
			return "Flaggi server not running here!";
		}

		gotoLobby();
		return "Connecting...";
	}

	public void invitePlayer(String username, Integer id) {
		Logger.log(LogLevel.DEBUG, username + " " + id);
	}

	// Config handeling ---------------------------------------------------------

	private void setConfigField(String key, String val) {
		try {
			Constants.CONFIG.setField(key, val);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "An error occured while setting property value", e);
			handleFatalError();
		}
	}

	// Private ------------------------------------------------------------------

	private void initializeLogger() {
		Logger.setLogFile(Constants.LOG_FILE);
		Logger.setLogLevelsToIgnore(Constants.IGNORED_LOG_LEVES);
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
				new MenuBackground(), //
				new LobbyUi(this::invitePlayer));
		this.gpanel.toggleWidgetsVisibility(false);
	}

	private void toggleUi(String... tags) {
		this.gpanel.toggleWidgetsVisibility(false);
		for (String tag : tags) {
			this.gpanel.toggleTaggedWidgetsVisibility(tag, true);
		}
	}

}
