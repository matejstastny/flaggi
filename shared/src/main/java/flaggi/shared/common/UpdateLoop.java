package flaggi.shared.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UpdateLoop implements Runnable {

  private final int updateInterval;
  private final List<Updatable> update;

  /**
   * @param updateIntervalMs - the interval in milliseconds between updates.
   * @param update - the object that will be updated.
   */
  public UpdateLoop(int updateIntervalMs) {
    this.updateInterval = updateIntervalMs;
    this.update = new CopyOnWriteArrayList<>();
  }

  public void add(Updatable updatable) {
    this.update.add(updatable);
  }

  public void remove(Updatable updatable) {
    this.update.remove(updatable);
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
