// ------------------------------------------------------------------------------
// App.java - Main application class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 11-04-2024 (2.0: 02-25-2025) (YYYY-MM-DD)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client;

import flaggi.client.common.GameManager;
import flaggi.client.common.Global;
import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.network.TcpManager;
import flaggi.client.network.UdpManager;
import flaggi.client.ui.ConfirmationWindow;
import flaggi.client.ui.LobbyUi;
import flaggi.client.ui.MenuBackground;
import flaggi.client.ui.MenuScreen;
import flaggi.client.ui.ToastManager;
import flaggi.client.ui.ToastManager.ToastCategory;
import flaggi.proto.ClientMessages.ClientCommandType;
import flaggi.proto.ServerMessages.ServerHello;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerMessage;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.ui.GPanel;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.util.NetUtil;
import flaggi.shared.util.ScreenUtil;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

public class App implements Updatable {

  private final ExecutorService threads;
  private final UpdateLoop updateLoop;
  private final GPanel gpanel;
  private final ToastManager toasts;
  private final ConfirmationWindow confirmationWindow;
  private GameManager gameManager;
  private TcpManager tcpManager;
  private UdpManager udpManager;
  private String uuid;

  // Main ---------------------------------------------------------------------

  public static void main(String[] args) {
    try {
      SwingUtilities.invokeLater(App::new);
    } catch (Exception e) {
      handleFatalError();
    }
  }

  public App() {
    initializeLogger();
    this.threads = Executors.newFixedThreadPool(3);
    this.updateLoop = new UpdateLoop(Constants.UPDATE_INTERVAL_MS);
    this.updateLoop.add(this);
    this.threads.execute(updateLoop);
    this.gpanel = getDefaultGpanel();
    this.toasts = new ToastManager();
    this.confirmationWindow = new ConfirmationWindow();
    addDefaultWidgets();
    gotoMainMenu();
  }

  // Static -------------------------------------------------------------------

  public static void handleFatalError() {
    Logger.log(LogLevel.ERR, "FATAL ERROR DETECTED! SHUTTING DOWN...");
    System.exit(1);
  }

  public static boolean isDev() {
    String env = System.getenv("FLAGGI_DEV");
    return env != null && !env.isEmpty();
  }

  // Events -------------------------------------------------------------------

  public void shutdown() {
    Logger.log(LogLevel.INF, "Shutting down...");
    if (tcpManager != null) {
      tcpManager.close();
    }
    Logger.log(LogLevel.INF, "Shut down");
    System.exit(0);
  }

  public void gotoMainMenu() {
    toggleUi(UiTags.MAIN_MENU);
  }

  public void gotoLobby() {
    toggleUi(UiTags.LOBBY);
  }

  public void gotoGame(ServerJoinGame message) {
    if (this.gameManager == null) {
      updateLoop.remove(gameManager);
    }
    gameManager = new GameManager(message, udpManager, uuid, gpanel);
    updateLoop.add(gameManager);
    toggleUi(UiTags.GAME);
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
    this.tcpManager.sendCommand(ClientCommandType.GET_IDLE_CLIENT_LIST);
  }

  public void invitePlayer(String otherUuid) {
    Logger.log(LogLevel.DBG, "Inviting player: " + otherUuid);
    this.tcpManager.sendInvite(otherUuid);
  }

  public void respondToInvite(String otherUuid, boolean accept) {
    Logger.log(
        LogLevel.DBG,
        "Responding to invite from " + otherUuid + " with " + (accept ? "accept" : "deny"));
    this.tcpManager.respondToInvite(otherUuid, accept);
  }

  // External events ----------------------------------------------------------

  public String joinServer(String name, String ipInput) {
    Logger.log(LogLevel.DBG, "Join server button pressed.");
    setConfigField("username", name);
    setConfigField("server.ip", ipInput);
    Entry<String, Integer> ip = verifyServerIp(ipInput);
    if (ip.getValue() == null) {
      Logger.log(LogLevel.WRN, ip.getKey() + " for ip input '" + ipInput + "'");
      return ip.getKey();
    }
    this.tcpManager = new TcpManager(ip.getKey(), ip.getValue(), this::disconnectFromServer);
    this.udpManager = new UdpManager();
    this.tcpManager.connect(name, this.udpManager.listenerPort());
    this.threads.execute(tcpManager);
    this.threads.execute(udpManager);
    return "Connecting...";
  }

  public void disconnectFromServer() {
    if (this.tcpManager != null) {
      this.tcpManager.close();
      this.tcpManager = null;
    }
    if (this.udpManager != null) {
      this.udpManager.close();
      this.udpManager = null;
    }
    gotoMainMenu();
    toasts.newToast(ToastCategory.ERROR, "Server shut down");
    this.gpanel.getWidgetsOfClass(MenuScreen.class).forEach(x -> x.reset());
  }

  // Config handeling ---------------------------------------------------------

  private void setConfigField(String key, String val) {
    try {
      Constants.CONFIG.setField(key, val);
    } catch (IOException e) {
      Logger.log(LogLevel.ERR, "An error occured while setting property value", e);
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
      } else if (message.hasServerJoinGame()) {
        gotoGame(message.getServerJoinGame());
      } else if (message.hasServerCommand()) {
      } else if (message.hasServerInvite()) {
        Runnable acceptAction =
            () -> respondToInvite(message.getServerInvite().getInviteeUuid(), true);
        Runnable denyAction =
            () -> respondToInvite(message.getServerInvite().getInviteeUuid(), false);
        this.confirmationWindow.newConfirmation(
            "Invite from " + message.getServerInvite().getInviteeName(), acceptAction, denyAction);
      } else if (message.hasIdleClientList()) {
        this.gpanel
            .getWidgetsOfClass(LobbyUi.class)
            .forEach(x -> x.setClients(message.getIdleClientList().getClientListMap()));
      } else {
        Logger.log(LogLevel.WRN, "Polled an unknown message type: " + message);
      }
    }
  }

  // Private ------------------------------------------------------------------

  private void initializeLogger() {
    Logger.setLogFile(Constants.LOG_FILE);
    Logger.setLogLevelsToIgnore(Constants.IGNORED_LOG_LEVES);
    Logger.log(LogLevel.INF, "Application start");
    if (Constants.LOG_MEM_USAGE) {
      Logger.logMemoryUsage(Constants.MEM_LOG_INTERVAL_SEC);
    }
  }

  private GPanel getDefaultGpanel() {
    int[] screenSize = ScreenUtil.getScreenDimensions();
    if (isDev()) {
      screenSize[0] = (int) Math.round(screenSize[0] * 0.4);
      screenSize[1] = (int) Math.round(screenSize[1] * 0.4);
    }
    GPanel gp =
        new GPanel(screenSize[0], screenSize[1], Constants.WINDOW_RESIZABLE, Constants.WINDOW_NAME);
    if (Constants.FRAMERATE >= 0) {
      gp.setFpsCap(Constants.FRAMERATE);
    }
    gp.setIconOSDependend(
        Constants.ICON_WIN, Constants.ICON_MAC, Constants.ICON_WIN, Constants.ICON_WIN);
    gp.setExitOperation(this::shutdown);
    return gp;
  }

  private void addDefaultWidgets() {
    List<Updatable> updatableWidgets =
        Arrays.asList(new LobbyUi(this::invitePlayer, this::refreshIdleClients));
    this.gpanel.add( //
        new MenuScreen(Constants.MENU_NAME_FIELD, Constants.MENU_IP_FIELD, this::joinServer), //
        new MenuBackground(), //
        this.toasts, //
        this.confirmationWindow);
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
    if (!NetUtil.isValidAddress(ipInput)) {
      return new SimpleEntry<>("Invalid IP address format!", null);
    }

    String[] parts = ipInput.split(":");
    if (parts.length != 2) {
      return new SimpleEntry<>("Invalid IP address format!", null);
    }

    String ip = parts[0].trim();
    int port;
    try {
      port = Integer.parseInt(parts[1].trim());
    } catch (NumberFormatException e) {
      return new SimpleEntry<>("Invalid port number!", null);
    }

    if (!Global.isFlaggiServer(ip, port)) {
      return new SimpleEntry<>("Flaggi server not running at the specified address!", null);
    }

    return new SimpleEntry<>(ip, port);
  }

  private void handleServerHello(ServerHello msg) {
    this.uuid = msg.getUuid();
    Logger.log(LogLevel.DBG, "Received uuid from server: " + msg.getUuid());
    Logger.log(LogLevel.DBG, "Received UDP port from server: " + msg.getUdpPort());
    this.tcpManager.setUuid(uuid);
    this.udpManager.setAdress(tcpManager.getIP(), (int) msg.getUdpPort());
    gotoLobby();
  }
}
