/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/25/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import flaggi.client.common.Global;
import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.network.TcpManager;
import flaggi.client.ui.LobbyUi;
import flaggi.client.ui.MenuBackground;
import flaggi.client.ui.MenuScreen;
import flaggi.client.ui.ToastManager;
import flaggi.client.ui.ToastManager.ToastCategory;
import flaggi.proto.ClientMessages.ClientCommandType;
import flaggi.proto.ServerMessages.ServerHello;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.util.NetUtil;
import flaggi.shared.util.ScreenUtil;

public class App implements Updatable {

	private final ExecutorService threads;
	private final UpdateLoop updateLoop;
	private final GPanel gpanel;
	private final ToastManager toasts;
	private TcpManager tcpManager;

	// Main ---------------------------------------------------------------------

	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::new);
	}

	public App() {
		initializeLogger();
		this.threads = Executors.newFixedThreadPool(3);
		this.updateLoop = new UpdateLoop(Constants.UPDATE_INTERVAL_MS);
		this.updateLoop.add(this);
		this.threads.execute(updateLoop);
		this.gpanel = getDefaultGpanel();
		this.toasts = new ToastManager();
		addDefaultWidgets();
		gotoMainMenu();
	}

	// Static -------------------------------------------------------------------

	public static void handleFatalError() {
		Logger.log(LogLevel.ERROR, "FATAL ERROR DETECTED! SHUTTING DOWN...");
		System.exit(1);
	}

	// Events -------------------------------------------------------------------

	public void shutdown() {
		Logger.log(LogLevel.INFO, "Shutting down...");
		if (tcpManager != null) {
			tcpManager.close();
		}
		Logger.log(LogLevel.INFO, "Shut down");
		System.exit(0);
	}

	public void gotoMainMenu() {
		toggleUi(UiTags.MAIN_MENU);
	}

	public void gotoLobby() {
		toggleUi(UiTags.LOBBY);
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		if (this.tcpManager != null) {
			processTcpMessages();
		}
	}

	// Server commands ----------------------------------------------------------

	public void refreshIdleClients() {
		this.tcpManager.sendCommandToServer(ClientCommandType.GET_IDLE_CLIENT_LIST);
	}

	// External events ----------------------------------------------------------

	public String joinServer(String name, String ipInput) {
		Logger.log(LogLevel.DEBUG, "Join server button pressed.");
		setConfigField("username", name);
		setConfigField("server.ip", ipInput);
		Entry<String, Integer> ip = verifyServerIp(ipInput);
		if (ip.getValue() == null) {
			Logger.log(LogLevel.ERROR, ip.getKey() + " for ip input '" + ipInput + "'");
			return ip.getKey();
		}
		this.tcpManager = new TcpManager(ip.getKey(), ip.getValue(), this::disconnectFromServer);
		this.tcpManager.connect(name);
		this.threads.execute(tcpManager);
		return "Connecting...";
	}

	public void disconnectFromServer() {
		if (this.tcpManager != null) {
			this.tcpManager.close();
			this.tcpManager = null;
		}
		gotoMainMenu();
		toasts.newToast(ToastCategory.ERROR, "Server shut down");
		this.gpanel.getWidgetsOfClass(MenuScreen.class).forEach(x -> x.reset());
	}

	public void invitePlayer(String username, String uuid) {
		Logger.log(LogLevel.DEBUG, username + " " + uuid);
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

	// TCP proccesing -----------------------------------------------------------

	private void processTcpMessages() {
		while (true) {
			ServerMessage message = this.tcpManager.poll();
			if (message == null) {
				break;
			} else if (message.hasPong()) {
				continue;
			} else if (message.hasServerHello()) {
				handleServerHello(message.getServerHello());
			} else if (message.hasServerGameJoin()) {
			} else if (message.hasServerCommand()) {
			} else if (message.hasIdleClientList()) {
				this.gpanel.getWidgetsOfClass(LobbyUi.class).forEach(x -> x.setClients(message.getIdleClientList().getClientListMap()));
			} else {
				Logger.log(LogLevel.WARN, "Polled an unknown message type: " + message);
			}
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
		gp.setExitOperation(this::shutdown);
		return gp;
	}

	private void addDefaultWidgets() {
		List<Updatable> updatableWidgets = Arrays.asList(new LobbyUi(this::invitePlayer, this::refreshIdleClients));
		this.gpanel.add( //
				new MenuScreen(Constants.MENU_NAME_FIELD, Constants.MENU_IP_FIELD, this::joinServer), //
				new MenuBackground(), //
				this.toasts);
		updatableWidgets.forEach(u -> this.gpanel.add((Renderable) u));
		updatableWidgets.forEach(u -> updateLoop.add(u));
		this.gpanel.toggleWidgetsVisibility(false);
	}

	private void toggleUi(String... tags) {
		this.gpanel.toggleWidgetsVisibility(false);
		for (String tag : tags) {
			this.gpanel.toggleTaggedWidgetsVisibility(tag, true);
		}
		this.gpanel.toggleTaggedWidgetsVisibility(UiTags.ALWAYS_VISIBLE, true);
	}

	private Entry<String, Integer> verifyServerIp(String ipInput) {
		if (!NetUtil.isValidAddress(ipInput))
			return new SimpleEntry<>("Invalid IP address format!", null);

		String[] parts = ipInput.split(":");
		if (parts.length != 2)
			return new SimpleEntry<>("Invalid IP address format!", null);

		String ip = parts[0].trim();
		int port;
		try {
			port = Integer.parseInt(parts[1].trim());
		} catch (NumberFormatException e) {
			return new SimpleEntry<>("Invalid port number!", null);
		}

		if (!Global.isFlaggiServer(ip, port))
			return new SimpleEntry<>("Flaggi server not running at the specified address!", null);

		return new SimpleEntry<>(ip, port);
	}

	private void handleServerHello(ServerHello msg) {
		Logger.log(LogLevel.DEBUG, "Received server hello message with uuid: " + msg.getUuid() + " and UDP port: " + msg.getUdpPort());
		this.tcpManager.setUuid(msg.getUuid());
		gotoLobby();
	}
}
