/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.shared.common;

public class UpdateLoop implements Runnable {

	private final int updateInterval;
	private final Updatable update;

	/**
	 * @param updateIntervalMs - the interval in milliseconds between updates.
	 * @param update           - the object that will be updated.
	 */
	public UpdateLoop(int updateIntervalMs, Updatable update) {
		this.updateInterval = updateIntervalMs;
		this.update = update;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			this.update.update();
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
