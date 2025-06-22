/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.common;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.App;
import flaggi.client.network.UdpManager;
import flaggi.proto.ClientMessages.ClientInputType;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.MapData;
import flaggi.shared.common.GPanel.AbstractInteractableHandler;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;

public class GameManager implements Closeable, Updatable {

	private final BlockingQueue<ClientStateUpdate> outgoing = new LinkedBlockingQueue<>();
	private UdpManager udpManager;
	private MapData mapData;
	private GPanel gpanel;

	// Constructor --------------------------------------------------------------

	public GameManager(UdpManager udpManager, GPanel gpanel, String mapJson) {
		this.udpManager = udpManager;
		this.gpanel = gpanel;
		addInteractHandeler();
		try {
			this.mapData = MapData.fromJson(mapJson);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to parse map JSON recieved from server", e);
			App.handleFatalError();
		}
	}

	@Override
	public void close() {
		this.gpanel.setInteractableHandler(null);
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ServerStateUpdate update;
		while ((update = udpManager.poll()) != null) {

		}
		ClientStateUpdate outgoingUpdate;
		while ((outgoingUpdate = outgoing.poll()) != null) {
			udpManager.send(outgoingUpdate);
		}
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

	private void sendInputUpdate(ClientInputType type, int x, int y, int code) {
		ClientStateUpdate update = ClientStateUpdate.newBuilder().setInputType(type).setMouseX(x).setMouseY(y).setKeyCode(code).build();
		outgoing.offer(update);
	}
}
