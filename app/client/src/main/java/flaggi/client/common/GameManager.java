/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.network.UdpManager;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.UpdateLoop.Updatable;

public class GameManager implements Updatable {

	private final BlockingQueue<ServerStateUpdate> outgoing = new LinkedBlockingQueue<>();
	private UdpManager udpManager;
	private GPanel gpanel;

	// Constructor --------------------------------------------------------------

	public GameManager(UdpManager udpManager, GPanel gpanel) {
		this.udpManager = udpManager;
		this.gpanel = gpanel;
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ServerStateUpdate update;
		while ((update = udpManager.poll()) != null) {
			// TODO Process the update
		}
		// send updates to server
		// send player movement update
	}
}
