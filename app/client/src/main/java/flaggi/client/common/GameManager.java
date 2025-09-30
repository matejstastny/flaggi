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
import flaggi.client.ui.GameUi;
import flaggi.proto.ClientMessages.ClientInputType;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.GPanel.AbstractInteractableHandler;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;

public class GameManager implements Closeable, Updatable {

	private final BlockingQueue<ClientStateUpdate> outgoing = new LinkedBlockingQueue<>();
	private final String uuid;
	private final String gameUuid;
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
	}

	@Override
	public void close() {
		this.gpanel.setInteractableHandler(null);
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ServerStateUpdate latest = udpManager.getLatestUpdate();
		this.gameUi.updateGameUi(latest);

		ClientStateUpdate outgoingUpdate;
		while ((outgoingUpdate = outgoing.poll()) != null) {
			udpManager.send(outgoingUpdate.toBuilder().setGameUuid(gameUuid).setPlayerUuid(uuid).build());
		}
	}

	// UI -----------------------------------------------------------------------

	private void setupGameUi(int width, int height) {
		this.gpanel.removeWidgetsOfClass(GameUi.class);
		this.gameUi = new GameUi(new int[] { width, height });
		Logger.log(LogLevel.DEBUG, "Set up game UI with room size [" + width + ", " + height + "]");
		this.gpanel.add(gameUi);
	}

	// Input handeling ----------------------------------------------------------

	private void addInteractHandeler() {
		this.gpanel.setInteractableHandler(new AbstractInteractableHandler() {
			@Override
			public void mousePressed(MouseEvent e) {
				sendInputUpdate(ClientInputType.MOUSE_PRESSED, e.getX(), e.getY(), -1);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				sendInputUpdate(ClientInputType.MOUSE_RELEASED, e.getX(), e.getY(), -1);
			}

			private long lastMouseMoveSent = 0;

			@Override
			public void mouseMoved(MouseEvent e) {
				long now = System.currentTimeMillis();
				if (now - lastMouseMoveSent > 50) {
					sendInputUpdate(ClientInputType.MOUSE_MOVED, e.getX(), e.getY(), -1);
					lastMouseMoveSent = now;
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				sendInputUpdate(ClientInputType.KEY_PRESSED, -1, -1, e.getKeyCode());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				sendInputUpdate(ClientInputType.KEY_RELEASED, -1, -1, e.getKeyCode());
			}
		});
	}

	private void sendInputUpdate(ClientInputType type, int mouseX, int mouseY, int code) {
		double vhX = mouseX == -1 ? -1 : this.gameUi.getViewHeight(mouseX);
		double vhY = mouseY == -1 ? -1 : this.gameUi.getViewHeight(mouseY);
		Logger.log(LogLevel.DEBUG, "Original mouse coords: [" + mouseX + "," + mouseY + "] -> View height coords: [" + vhX + "," + vhY + "]");
		ClientStateUpdate update = ClientStateUpdate.newBuilder().setInputType(type).setVhX(vhX).setVhY(vhY).setKeyCode(code).build();
		outgoing.offer(update);
	}
}
