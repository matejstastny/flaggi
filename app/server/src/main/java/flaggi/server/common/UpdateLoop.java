/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.common;

public class UpdateLoop implements Runnable {

	private final int updateInterval;
	private final Updatable update;

	public UpdateLoop(int updateInterval, Updatable update) {
		this.updateInterval = updateInterval;
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
