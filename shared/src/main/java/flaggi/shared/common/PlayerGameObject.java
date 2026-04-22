package flaggi.shared.common;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.PlayerAnimation;
import flaggi.proto.ServerMessages.PlayerSkin;
import flaggi.proto.ServerMessages.ServerGameObject;

public class PlayerGameObject extends GameObject {

    public static final double MAX_HP = 100.0;
    public static final double PLAYER_SPEED = 3.0; // pixels per tick
    public static final long SHOOT_COOLDOWN_MS = 400; // ms between shots

    private PlayerAnimation animation = PlayerAnimation.ANIM_IDLE;
    private boolean facingLeft = false;
    private boolean carryingFlag = false;
    private double spawnX, spawnY;
    private long lastShotTime = 0;
    private int flagCount = 0;
    private String username;
    private PlayerSkin skin;

    // Constructor --------------------------------------------------------------

    public PlayerGameObject(double x, double y, Hitbox hitbox, String username, PlayerSkin skin) {
        super(GameObjectType.PLAYER, x, y, hitbox);
        this.username = username;
        this.skin = skin;
        this.spawnX = x;
        this.spawnY = y;
        setHp(MAX_HP);
    }

    // Proto conversion ---------------------------------------------------------

    @Override
    public ServerGameObject toProto() {
        return ServerGameObject.newBuilder()
                .setType(GameObjectType.PLAYER)
                .setX(x())
                .setY(y())
                .setCollX(collision().getX())
                .setCollY(collision().getY())
                .setCollWidth(collision().getWidth())
                .setCollHeight(collision().getHeight())
                .setSkin(skin)
                .setAnimation(animation)
                .setFacingLeft(facingLeft)
                .setHp(hp())
                .setFlagCount(flagCount)
                .setUsername(username)
                .build();
    }

    // Accessors ----------------------------------------------------------------

    public String username() {
        return username;
    }

    public PlayerSkin skin() {
        return skin;
    }

    public PlayerAnimation animation() {
        return animation;
    }

    public boolean facingLeft() {
        return facingLeft;
    }

    public int flagCount() {
        return flagCount;
    }

    public double spawnX() {
        return spawnX;
    }

    public double spawnY() {
        return spawnY;
    }

    public boolean carryingFlag() {
        return carryingFlag;
    }

    public boolean canShoot() {
        return System.currentTimeMillis() - lastShotTime >= SHOOT_COOLDOWN_MS;
    }

    public boolean isAlive() {
        return hp() > 0;
    }

    // Modifiers ----------------------------------------------------------------

    public void setAnimation(PlayerAnimation animation) {
        this.animation = animation;
    }

    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    public void incrementFlagCount() {
        this.flagCount++;
    }

    public void setCarryingFlag(boolean carrying) {
        this.carryingFlag = carrying;
    }

    public void recordShot() {
        this.lastShotTime = System.currentTimeMillis();
    }

    /** Respawn this player at their spawn point with full HP and no flag. */
    public void respawn() {
        setX(spawnX);
        setY(spawnY);
        setHp(MAX_HP);
        setCarryingFlag(false);
        setAnimation(PlayerAnimation.ANIM_IDLE);
    }
}
