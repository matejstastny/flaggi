package flaggi.shared.common;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.ServerGameObject;

public class FlagGameObject extends GameObject {

    public enum Team {
        RED,
        BLUE
    }

    public enum FlagState {
        AT_BASE, // resting at home base - can be stolen by enemy
        CARRIED, // picked up and held by a player
        DROPPED // dropped mid-field - either team can interact
    }

    private final Team team;
    private final double baseX, baseY; // where it respawns when returned
    private FlagState state = FlagState.AT_BASE;
    private String carrierUuid = null; // UUID of player currently carrying it

    // Constructor --------------------------------------------------------------

    public FlagGameObject(Team team, double x, double y, Hitbox hitbox) {
        super(GameObjectType.FLAG, x, y, hitbox);
        this.team = team;
        this.baseX = x;
        this.baseY = y;
    }

    // Proto conversion ---------------------------------------------------------

    @Override
    public ServerGameObject toProto() {
        return ServerGameObject.newBuilder()
                .setType(GameObjectType.FLAG)
                .setX(x())
                .setY(y())
                .setCollX(collision().getX())
                .setCollY(collision().getY())
                .setCollWidth(collision().getWidth())
                .setCollHeight(collision().getHeight())
                .build();
    }

    // Public -------------------------------------------------------------------

    /**
     * Pick up this flag by the given player. Only valid when AT_BASE or DROPPED.
     */
    public boolean pickUp(String playerUuid) {
        if (state == FlagState.CARRIED) return false;
        state = FlagState.CARRIED;
        carrierUuid = playerUuid;
        return true;
    }

    /** Drop the flag at the given world position (e.g. on carrier death). */
    public void drop(double x, double y) {
        setX(x);
        setY(y);
        state = FlagState.DROPPED;
        carrierUuid = null;
    }

    /**
     * Return the flag to its home base (called when captured or when a teammate
     * touches a dropped flag).
     */
    public void returnToBase() {
        setX(baseX);
        setY(baseY);
        state = FlagState.AT_BASE;
        carrierUuid = null;
    }

    /** Move the flag with its carrier each tick. */
    public void moveWithCarrier(double x, double y) {
        setX(x);
        setY(y);
    }

    // Accessors ----------------------------------------------------------------

    public Team team() {
        return team;
    }

    public FlagState state() {
        return state;
    }

    public String carrierUuid() {
        return carrierUuid;
    }

    public double baseX() {
        return baseX;
    }

    public double baseY() {
        return baseY;
    }

    public boolean isCarried() {
        return state == FlagState.CARRIED;
    }

    public boolean isAtBase() {
        return state == FlagState.AT_BASE;
    }

    /**
     * World-space collision bounds (used for proximity / overlap checks).
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
