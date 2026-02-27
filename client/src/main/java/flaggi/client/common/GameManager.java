// ------------------------------------------------------------------------------
// GameManager.java - Manages game state and logic on the client side
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 2025-06-21 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.common;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.util.EnumSet;
import java.util.Set;

import flaggi.client.constants.UiTags;
import flaggi.client.network.UdpManager;
import flaggi.client.ui.DebugOverlay;
import flaggi.client.ui.GameUi;
import flaggi.proto.ClientMessages.ClientKey;
import flaggi.proto.ClientMessages.ClientKeyInput;
import flaggi.proto.ClientMessages.ClientMouseInput;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerJoinGame;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.ui.GPanel;
import flaggi.shared.ui.GPanel.AbstractInteractableHandler;

public class GameManager implements Closeable, Updatable {

	private final String uuid;
	private final String gameUuid;
	private final UdpManager udpManager;
	private final GPanel gpanel;

	// Player inputs
	private volatile Set<ClientKey> heldKeys = EnumSet.noneOf(ClientKey.class);
	private volatile boolean inputDirty = false;
	private volatile int mouseScreenX = 0;
	private volatile int mouseScreenY = 0;

	private DebugOverlay debugOverlay;
	private GameUi gameUi;

	// Constructor --------------------------------------------------------------

	public GameManager(ServerJoinGame message, UdpManager udpManager, String uuid, GPanel gpanel) {
		this.gameUuid = message.getGameUuid();
		this.udpManager = udpManager;
		this.gpanel = gpanel;
		this.uuid = uuid;
		addInteractHandler();
		setupUi(message.getRoomWidth(), message.getRoomHeight());
	}

	@Override
	public void close() {
		this.gpanel.toggleTaggedWidgetsVisibility(UiTags.GAME, false);
		this.gpanel.setInteractableHandler(null);
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ServerStateUpdate latest = udpManager.getLatestUpdate();
		if (latest != null && debugOverlay != null) {
			debugOverlay.update(latest);
		}
		if (latest != null && gameUi != null) {
			gameUi.update(latest);
		}

		if (!inputDirty) {
			return;
		}
		inputDirty = false;

		// TODO broken conversion
		int worldMouseX = mouseScreenX;
		int worldMouseY = mouseScreenY;
		if (latest != null && latest.hasMe()) {
			int screenCentreX = gpanel.getWidth() / 2;
			int screenCentreY = gpanel.getHeight() / 2;
			worldMouseX = (int) (latest.getMe().getX() + (mouseScreenX - screenCentreX));
			worldMouseY = (int) (latest.getMe().getY() + (mouseScreenY - screenCentreY));
		}

		Set<ClientKey> keySnapshot = heldKeys;
		ClientStateUpdate outgoing = ClientStateUpdate.newBuilder().setPlayerUuid(uuid).setGameUuid(gameUuid).setClientMouseInput(ClientMouseInput.newBuilder().setX(worldMouseX).setY(worldMouseY).build()).setClientKeyInput(ClientKeyInput.newBuilder().addAllHeldKeys(keySnapshot).build()).build();
		udpManager.send(outgoing);
	}

	// UI -----------------------------------------------------------------------

	private void setupUi(int width, int height) {
		this.gpanel.removeWidgetsOfClass(DebugOverlay.class);
		this.gpanel.removeWidgetsOfClass(GameUi.class);

		this.gameUi = new GameUi(new int[] { width, height });
		this.debugOverlay = new DebugOverlay();

		Logger.log(LogLevel.DBG, "Set up game UI with room size [" + width + ", " + height + "]");
		this.gpanel.add(gameUi, debugOverlay);
		this.gpanel.toggleTaggedWidgetsVisibility(UiTags.GAME, true);
	}

	// Input handeling ----------------------------------------------------------

	private void sendClientMouseUpdate(MouseEvent e) {
		mouseScreenX = e.getX();
		mouseScreenY = e.getY();
		inputDirty = true;
	}

	private void onKeyPressed(KeyEvent e) {
		ClientKey key = toClientKey(e.getKeyCode());
		if (key == null)
			return;

		Set<ClientKey> updated = EnumSet.copyOf(heldKeys.isEmpty() ? EnumSet.noneOf(ClientKey.class) : heldKeys);
		updated.add(key);
		heldKeys = updated;
		inputDirty = true;
	}

	private void onKeyReleased(KeyEvent e) {
		ClientKey key = toClientKey(e.getKeyCode());
		if (key == null)
			return;

		Set<ClientKey> updated = EnumSet.copyOf(heldKeys.isEmpty() ? EnumSet.noneOf(ClientKey.class) : heldKeys);
		updated.remove(key);
		heldKeys = updated;
		inputDirty = true;
	}

	private static ClientKey toClientKey(int keyCode) {
		return switch (keyCode) {
		case KeyEvent.VK_W, KeyEvent.VK_UP -> ClientKey.KEY_UP;
		case KeyEvent.VK_S, KeyEvent.VK_DOWN -> ClientKey.KEY_DOWN;
		case KeyEvent.VK_A, KeyEvent.VK_LEFT -> ClientKey.KEY_LEFT;
		case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> ClientKey.KEY_RIGHT;
		case KeyEvent.VK_SPACE, KeyEvent.VK_F -> ClientKey.KEY_SHOOT;
		default -> null;
		};
	}

	private void addInteractHandler() {
		this.gpanel.setInteractableHandler(new AbstractInteractableHandler() {

			@Override
			public void mouseMoved(MouseEvent e) {
				sendClientMouseUpdate(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				sendClientMouseUpdate(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				onKeyPressed(makeSyntheticShootEvent());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				onKeyReleased(makeSyntheticShootEvent());
			}

			@Override
			public void keyPressed(KeyEvent e) {
				onKeyPressed(e);
			}

			@Override
			public void keyReleased(KeyEvent e) {
				onKeyReleased(e);
			}
		});
	}

	private static KeyEvent makeSyntheticShootEvent() {
		return new KeyEvent(new java.awt.Component() {
		}, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_F, KeyEvent.CHAR_UNDEFINED);
	}
}
