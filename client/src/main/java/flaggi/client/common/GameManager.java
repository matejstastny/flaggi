// ------------------------------------------------------------------------------
// GameManager.java - Manages game state and logic on the client side
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.common;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.network.UdpManager;
import flaggi.client.ui.DebugGame;
import flaggi.client.ui.DebugOverlay;
import flaggi.client.ui.GameUi;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.ui.GPanel;
import flaggi.shared.ui.GPanel.AbstractInteractableHandler;

public class GameManager implements Closeable, Updatable {

    private final BlockingQueue<ClientStateUpdate> outgoing = new LinkedBlockingQueue<>();
    private final String uuid;
    private final String gameUuid;
    private DebugOverlay debugOverlay;
    private UdpManager udpManager;
    private GameUi gameUi;
    private GPanel gpanel;

    // Constructor --------------------------------------------------------------

    public GameManager(ServerJoinGame message, UdpManager udpManager, String uuid, GPanel gpanel) {
        this.gameUuid = message.getGameUuid();
        this.udpManager = udpManager;
        this.gpanel = gpanel;
        this.uuid = uuid;
        addInteractHandeler();
        setupGameUi(message.getRoomWidth(), message.getRoomHeight());
        debugOverlay = new DebugOverlay();
        gpanel.add(debugOverlay);
    }

    @Override
    public void close() {
        this.gpanel.setInteractableHandler(null);
    }

    // Update -------------------------------------------------------------------

    @Override
    public void update() {
        ServerStateUpdate latest = udpManager.getLatestUpdate();
        this.debugOverlay.update(latest);
        // this.gameUi.updateGameUi(latest);

        // ClientStateUpdate outgoingUpdate;
        // while ((outgoingUpdate = outgoing.poll()) != null) {
        // udpManager.send(outgoingUpdate.toBuilder().setGameUuid(gameUuid).setPlayerUuid(uuid).build());
        // }
    }

    // UI -----------------------------------------------------------------------

    private void setupGameUi(int width, int height) {
        // this.gpanel.removeWidgetsOfClass(GameUi.class);
        // this.gameUi = new GameUi(new int[] { width, height });
        gpanel.add(new DebugGame());
        Logger.log(LogLevel.DBG, "Set up game UI with room size [" + width + ", " + height + "]");
        // this.gpanel.add(gameUi);
    }

    // Input handeling ----------------------------------------------------------

    private void sendClientMouseUpdate(MouseEvent e) {
        // TODO
    }

    private void sendClientKeyAction(KeyEvent e) {
        // TODO
    }

    private void addInteractHandeler() {
        this.gpanel.setInteractableHandler(new AbstractInteractableHandler() {
            @Override
            public void mouseReleased(MouseEvent e) {
                sendClientMouseUpdate(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendClientKeyAction(e);
            }
        });
    }
}
