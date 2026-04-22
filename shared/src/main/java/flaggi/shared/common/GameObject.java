package flaggi.shared.common;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.ServerGameObject;

public class GameObject {

    private GameObjectType type;
    private Hitbox collision;
    private double x, y, hp;

    // Constructors -------------------------------------------------------------

    public GameObject(ServerGameObject o) {
        this(
                o.getType(),
                o.getX(),
                o.getY(),
                new Hitbox(o.getCollX(), o.getCollY(), o.getCollWidth(), o.getCollHeight()));
        this.hp = o.getHp();
    }

    public GameObject(GameObjectType type, double x, double y, Hitbox c) {
        this.collision = c;
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = 0;
    }

    // Public -------------------------------------------------------------------

    public ServerGameObject toProto() {
        return ServerGameObject.newBuilder()
                .setType(type)
                .setX(x)
                .setY(y)
                .setCollX(collision.getX())
                .setCollY(collision.getY())
                .setCollWidth(collision.getWidth())
                .setCollHeight(collision.getHeight())
                .setHp(hp)
                .build();
    }

    /**
     * Returns true if this object's world-space hitbox overlaps with {@code other}'s. Hitbox x/y are
     * offsets from the object's origin.
     */
    public boolean overlaps(GameObject other) {
        double ax1 = this.x + this.collision.getX();
        double ay1 = this.y + this.collision.getY();
        double ax2 = ax1 + this.collision.getWidth();
        double ay2 = ay1 + this.collision.getHeight();

        double bx1 = other.x + other.collision.getX();
        double by1 = other.y + other.collision.getY();
        double bx2 = bx1 + other.collision.getWidth();
        double by2 = by1 + other.collision.getHeight();

        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }

    /** Returns the world-space left edge of the hitbox. */
    public double worldLeft() {
        return x + collision.getX();
    }

    public double worldTop() {
        return y + collision.getY();
    }

    public double worldRight() {
        return worldLeft() + collision.getWidth();
    }

    public double worldBottom() {
        return worldTop() + collision.getHeight();
    }

    // Accessors ----------------------------------------------------------------

    public GameObjectType type() {
        return type;
    }

    public Hitbox collision() {
        return collision;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double hp() {
        return hp;
    }

    // Modifiers ----------------------------------------------------------------

    public void setType(GameObjectType type) {
        this.type = type;
    }

    public void setCollision(Hitbox c) {
        this.collision = c;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setHp(double hp) {
        this.hp = hp;
    }

    public void damage(double amount) {
        this.hp = Math.max(0, this.hp - amount);
    }
}
