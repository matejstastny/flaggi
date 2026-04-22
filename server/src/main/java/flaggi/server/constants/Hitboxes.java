package flaggi.server.constants;

import flaggi.shared.common.Hitbox;

public class Hitboxes {

  // Private constructor to prevent instantiation
  private Hitboxes() {
    throw new UnsupportedOperationException(
        "Hitboxes is a constants class and cannot be instantiated.");
  }

  // Hitboxes -----------------------------------------------------------------

	/**
	 * Player hitbox - centred on the player origin (x, y). 5×5 unit square offset
	 * by -2.5 so the origin sits in the middle
	 */
	public static Hitbox player() {
		return new Hitbox(-2.5, -2.5, 5, 5);
	}

	/**
	 * Bullet hitbox - small 2×2 square, centred on the bullet origin
	 */
	public static Hitbox bullet() {
		return new Hitbox(-1, -1, 2, 2);
	}

	/**
	 * Flag hitbox - 6×6 square, centred on the flag origin. Slightly larger than a
	 * player so pickup is forgiving
	 */
	public static Hitbox flag() {
		return new Hitbox(-3, -3, 6, 6);
	}

  /** Obstacle hitbox. Adjust to match your art asset. Currently a 10×10 square centred on origin */
  public static Hitbox obstacle() {
    return new Hitbox(-5, -5, 10, 10);
  }
}
