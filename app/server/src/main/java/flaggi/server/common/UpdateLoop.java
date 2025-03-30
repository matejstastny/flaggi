/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 2/23/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.server.common;

import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;

public class UpdateLoop implements Runnable {

	private final int updateInterval;
	private final Updatable update;

	public UpdateLoop(int updateInterval, Updatable update) {
		this.updateInterval = updateInterval;
		this.update = update;
	}

	@Override
	public void run() {
		Logger.log(LogLevel.INFO, "Main update loop started");
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
