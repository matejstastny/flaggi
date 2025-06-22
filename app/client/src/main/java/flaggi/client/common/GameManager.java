/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.common;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import flaggi.client.App;
import flaggi.client.network.UdpManager;
import flaggi.proto.ClientMessages.ClientStateUpdate;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.MapData;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.common.UpdateLoop.Updatable;

public class GameManager implements Updatable {

	private final BlockingQueue<ClientStateUpdate> outgoing = new LinkedBlockingQueue<>();
	private UdpManager udpManager;
	private MapData mapData;
	private GPanel gpanel;

	// Constructor --------------------------------------------------------------

	public GameManager(UdpManager udpManager, GPanel gpanel, String mapJson) {
		this.udpManager = udpManager;
		try {
			this.mapData = MapData.fromJson(mapJson);
		} catch (IOException e) {
			Logger.log(LogLevel.ERROR, "Failed to parse map JSON recieved from server", e);
			App.handleFatalError();
		}
		this.gpanel = gpanel;
		this.mapData.logMapDetails();
	}

	// Update -------------------------------------------------------------------

	@Override
	public void update() {
		ServerStateUpdate update;
		while ((update = udpManager.poll()) != null) {
			// TODO Process the update
		}
		ClientStateUpdate outgoingUpdate;
		while ((outgoingUpdate = outgoing.poll()) != null) {
			udpManager.send(outgoingUpdate);
		}
		// send updates to server
		// send player movement update
	}
}
