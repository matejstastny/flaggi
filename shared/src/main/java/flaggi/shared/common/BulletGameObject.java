package flaggi.shared.common;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.ServerGameObject;

public class BulletGameObject extends GameObject {

  public static final double BULLET_SPEED = 8.0; // pixels per tick
  public static final double BULLET_DAMAGE = 25.0;
  public static final int MAX_LIFETIME_TICKS = 120; // 2 seconds at 60 ticks/s

  private final String ownerUuid;
  private final double vx, vy; // normalised velocity components * BULLET_SPEED
  private int lifetimeTicks = 0;

  // Constructor --------------------------------------------------------------

  /**
   * @param x spawn X (owner's world position)
   * @param y spawn Y
   * @param targetX mouse cursor X in world space
   * @param targetY mouse cursor Y in world space
   * @param ownerUuid UUID of the player who fired this bullet
   * @param hitbox bullet collision hitbox
   */
  public BulletGameObject(
      double x, double y, double targetX, double targetY, String ownerUuid, Hitbox hitbox) {
    super(GameObjectType.BULLET, x, y, hitbox);

    this.ownerUuid = ownerUuid;

    // Compute normalised direction vector
    double dx = targetX - x;
    double dy = targetY - y;
    double length = Math.sqrt(dx * dx + dy * dy);
    if (length == 0) {
      this.vx = BULLET_SPEED;
      this.vy = 0;
    } else {
      this.vx = (dx / length) * BULLET_SPEED;
      this.vy = (dy / length) * BULLET_SPEED;
    }
  }

  // Proto conversion ---------------------------------------------------------

  @Override
  public ServerGameObject toProto() {
    return ServerGameObject.newBuilder()
        .setType(GameObjectType.BULLET)
        .setX(x())
        .setY(y())
        .setCollX(collision().getX())
        .setCollY(collision().getY())
        .setCollWidth(collision().getWidth())
        .setCollHeight(collision().getHeight())
        .build();
  }

  // Public -------------------------------------------------------------------

  /** Advance bullet position by one tick. Returns true if the bullet is still alive. */
  public boolean tick(int mapWidth, int mapHeight) {
    setX(x() + vx);
    setY(y() + vy);
    lifetimeTicks++;

    // Out of bounds or expired
    return lifetimeTicks < MAX_LIFETIME_TICKS
        && x() >= 0
        && x() <= mapWidth
        && y() >= 0
        && y() <= mapHeight;
  }

  // Accessors ----------------------------------------------------------------

  public String ownerUuid() {
    return ownerUuid;
  }

  public int lifetimeTicks() {
    return lifetimeTicks;
  }

  /**
   * World-space axis-aligned bounding box for collision checks. The hitbox offsets
   * (collision().getX/Y()) are relative to the object origin.
   */
  public double worldCollX() {
    return x() + collision().getX();
  }

  public double worldCollY() {
    return y() + collision().getY();
  }

  public double worldCollRight() {
    return worldCollX() + collision().getWidth();
  }

  public double worldCollBottom() {
    return worldCollY() + collision().getHeight();
  }
}
