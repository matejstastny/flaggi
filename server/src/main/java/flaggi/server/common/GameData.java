// ------------------------------------------------------------------------------
// GameData.java - Stores game instance data
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 10-01-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.GameObject;

public class GameData {

	private Map<String, GameObject> data = new ConcurrentHashMap<>();
	private List<String> uuids = new ArrayList<>();

	// New Objects -------------------------------------------------------------

	public void add(GameObject g, String uuid) {
		if (g.type() == GameObjectType.PLAYER) {
			uuids.add(uuid);
		}
		data.put(uuid, g);
	}

	// Public ------------------------------------------------------------------

	public Map<String, ServerStateUpdate> getServerUpdateData() {
		Map<String, ServerStateUpdate> updates = new HashMap<>();
		for (String uuid : uuids) {
			GameObject me = data.get(uuid);
			ServerStateUpdate.Builder msg = ServerStateUpdate.newBuilder().setMe(me.toProto());
			data.values().stream().filter(obj -> obj != me).map(obj -> obj.toProto()).forEach(obj -> msg.addOther(obj));
			updates.put(uuid, msg.build());
		}
		return updates;
	}
}
