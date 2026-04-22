// ------------------------------------------------------------------------------
// GameData.java - Stores and manages game instance state
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 10-01-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.common;

import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.common.BulletGameObject;
import flaggi.shared.common.FlagGameObject;
import flaggi.shared.common.GameObject;
import flaggi.shared.common.PlayerGameObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameData {

    private final Map<String, PlayerGameObject> players = new ConcurrentHashMap<>(); // string = uuid
    private final List<BulletGameObject> bullets = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final List<FlagGameObject> flags = new ArrayList<>(); // RED at 0 BLUE at 1
    private final List<GameObject> obstacles = new ArrayList<>();

    // New Objects --------------------------------------------------------------

    public void addPlayer(PlayerGameObject player, String uuid) {
        players.put(uuid, player);
        scores.put(uuid, 0);
    }

    public void addFlag(FlagGameObject flag) {
        flags.add(flag);
    }

    public void addTree(GameObject tree) {
        obstacles.add(tree);
    }

    public void addBullet(BulletGameObject bullet) {
        bullets.add(bullet);
    }

    // Public Accessors ---------------------------------------------------------

    public PlayerGameObject getPlayer(String uuid) {
        return players.get(uuid);
    }

    public Map<String, PlayerGameObject> getPlayers() {
        return players;
    }

    public List<BulletGameObject> getBullets() {
        return bullets;
    }

    public List<FlagGameObject> getFlags() {
        return flags;
    }

    public List<GameObject> getObstacles() {
        return obstacles;
    }

    public int getScore(String uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public void incrementScore(String uuid) {
        scores.merge(uuid, 1, Integer::sum);
    }

    // Bullet management --------------------------------------------------------

    /**
     * Advance all bullets one tick (move them). Dead bullets are removed and
     * returned so the caller can perform hit-testing before they disappear.
     *
     * @param mapWidth  world width in pixels (for out-of-bounds culling)
     * @param mapHeight world height in pixels
     * @return list of bullets whose lifetime expired this tick (already removed
     *         from live list)
     */
    public List<BulletGameObject> tickBullets(int mapWidth, int mapHeight) {
        List<BulletGameObject> expired = new ArrayList<>();
        Iterator<BulletGameObject> it = bullets.iterator();
        while (it.hasNext()) {
            BulletGameObject b = it.next();
            boolean alive = b.tick(mapWidth, mapHeight);
            if (!alive) {
                expired.add(b);
                bullets.remove(b);
            }
        }
        return expired;
    }

    public void removeBullet(BulletGameObject bullet) {
        bullets.remove(bullet);
    }

    // Proto serialisation ------------------------------------------------------

    /**
     * Build one {@link ServerStateUpdate} per player. Each update contains the
     * player's own object in {@code me} and every other visible object in
     * {@code other}.
     */
    public Map<String, ServerStateUpdate> getServerUpdateData() {
        Map<String, ServerStateUpdate> updates = new HashMap<>();

        // Collect all non-player objects once - they are the same for every player
        List<flaggi.proto.ServerMessages.ServerGameObject> sharedObjects = new ArrayList<>();

        for (BulletGameObject b : bullets) {
            sharedObjects.add(b.toProto());
        }
        for (FlagGameObject f : flags) {
            if (!f.isCarried()) {
                sharedObjects.add(f.toProto());
            }
        }
        for (GameObject t : obstacles) {
            sharedObjects.add(t.toProto());
        }

        for (Map.Entry<String, PlayerGameObject> entry : players.entrySet()) {
            String uuid = entry.getKey();
            PlayerGameObject me = entry.getValue();

            ServerStateUpdate.Builder msg = ServerStateUpdate.newBuilder().setMe(me.toProto());

            for (Map.Entry<String, PlayerGameObject> other : players.entrySet()) {
                if (!other.getKey().equals(uuid)) {
                    msg.addOther(other.getValue().toProto());
                }
            }

            sharedObjects.forEach(msg::addOther);

            updates.put(uuid, msg.build());
        }

        return updates;
    }
}
