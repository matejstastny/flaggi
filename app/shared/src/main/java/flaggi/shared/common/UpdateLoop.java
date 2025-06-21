/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 2/23/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.shared.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UpdateLoop implements Runnable {

	private final int updateInterval;
	private final List<Updatable> update;

	/**
	 * @param updateIntervalMs - the interval in milliseconds between updates.
	 * @param update           - the object that will be updated.
	 */
	public UpdateLoop(int updateIntervalMs) {
		this.updateInterval = updateIntervalMs;
		this.update = new CopyOnWriteArrayList<>();
	}

	public void add(Updatable updatable) {
		this.update.add(updatable);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			this.update.forEach(u -> u.update());
			try {
				Thread.sleep(this.updateInterval);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public interface Updatable {
		void update();
	}

}
