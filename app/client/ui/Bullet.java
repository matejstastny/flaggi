/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 12/6/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.client.common.Sprite;
import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;

/**
 * Bullet projectile UI widget.
 */
public class Bullet extends Renderable implements Runnable {

    private static final int TRAIL_LENGTH = 10;
    private static int BULLET_COUNT = 0; // Unique bullet ID

    private double[] direction, position;
    private int velocity, decayTime;
    private Sprite sprite;
    private boolean running;
    private Runnable afterDecay;
    private List<double[]> trail;
    private String creationData, bulletId;
    private Thread decayUpdateThread;

    // Constructors -------------------------------------------------------------

    /**
     * Default constructor.
     *
     * @param initialPosition - Initial position of the bullet [x, y].
     * @param targetPosition  - Target position the bullet heads to [x, y].
     * @param velocity        - Velocity in points per second.
     * @param decayTime       - Time (in ms) after which the bullet disappears.
     */
    public Bullet(int[] initialPosition, int[] targetPosition, int velocity, int decayTime, int clientId) {
        super(ZIndex.ENVIRONMENT_TOP, UiTags.GAME_ELEMENTS, UiTags.PROJECTILES);

        this.position = new double[] { initialPosition[0], initialPosition[1] };
        this.velocity = velocity;
        this.decayTime = decayTime;
        this.bulletId = clientId + "-" + BULLET_COUNT;
        this.trail = new LinkedList<>();
        this.sprite = createSprite();
        this.direction = calculateDirection(initialPosition, targetPosition);
        this.creationData = generateCreationData(initialPosition, targetPosition, decayTime, velocity);
        this.running = true;

        startDecayThread();

        BULLET_COUNT++;
    }

    /**
     * Enemy bullet projectiles. Doesn't increase the bullet ID.
     */
    public Bullet(int[] initialPosition, int[] targetPosition, int velocity, int decayTime, String bulletId) {
        this(initialPosition, targetPosition, velocity, decayTime, -1);
        this.bulletId = bulletId;
        BULLET_COUNT--;
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] viewportOffset, Container focusCycleRootAncestor) {
        // Draw the trail
        for (int i = 0; i < trail.size(); i++) {
            double[] pos = trail == null ? new double[] { 0, 0 } : trail.get(i);
            float alpha = (float) (1.0 - (i / (double) trail.size())); // Fade effect
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(255, 255, 255, (int) (alpha * 255))); // White trail
            g.fillOval((int) pos[0] + viewportOffset[0], (int) pos[1] + viewportOffset[1], 6, 6);
        }

        // Reset opacity for the sprite
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // Rotate sprite to face direction
        double angle = Math.atan2(direction[1], direction[0]);
        AffineTransform oldTransform = g.getTransform();
        g.translate(position[0] + viewportOffset[0], position[1] + viewportOffset[1]);
        g.rotate(angle);
        this.sprite.render(g, 0, -6, focusCycleRootAncestor, false);
        g.setTransform(oldTransform);

        // Debug: Show hitbox if enabled
        if (Constants.HITBOXES_ENABLED) {
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(1));
            Rectangle r = new Rectangle((int) position[0] + viewportOffset[0], (int) position[1] + viewportOffset[1], 5, 5);
            g.draw(r);
        }
    }

    // Accesors -----------------------------------------------------------------

    /**
     * Returns the player object ID in list [bulletId, clientId].
     *
     * @return - player object ID.
     */
    public String getObjectId() {
        return this.bulletId;
    }

    @Override
    public String toString() {
        return this.creationData;
    }

    // Modifiers ----------------------------------------------------------------

    /**
     * Sets the after decay runnable, ran after the bullet decays. Used for removing
     * the bullet from the player bullet list in App.
     *
     * @param afterDecay - {@code Runnable} to be ran after decay.
     */
    public void setPostDecayAction(Runnable afterDecay) {
        this.afterDecay = afterDecay;
    }

    // Private ------------------------------------------------------------------

    private static Sprite createSprite() {
        Sprite sprite = new Sprite();
        sprite.addAnimation("bullet", Arrays.asList("bullet"));
        sprite.setAnimation("bullet");
        return sprite;
    }

    private static double[] calculateDirection(int[] initialPosition, int[] targetPosition) {
        double dx = targetPosition[0] - initialPosition[0];
        double dy = targetPosition[1] - initialPosition[1];
        double magnitude = Math.sqrt(dx * dx + dy * dy);
        return new double[] { dx / magnitude, dy / magnitude };
    }

    private static String generateCreationData(int[] initialPosition, int[] targetPosition, int decayTime, int velocity) {
        return "bullet:" + BULLET_COUNT + ":" + initialPosition[0] + "&" + initialPosition[1] + ":" + targetPosition[0] + "&" + targetPosition[1] + ":" + decayTime + ":" + velocity;
    }

    private void startDecayThread() {
        this.decayUpdateThread = new Thread(this, "Bullet update thread for bullet: " + this.creationData.split(":")[1]);
        this.decayUpdateThread.start();
    }

    // Update logic -------------------------------------------------------------

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long lastUpdate = System.currentTimeMillis();

        while (running) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastUpdate;

            // Update position
            if (elapsedTime > 0) {
                double delta = (elapsedTime / 1000.0) * this.velocity;
                this.position[0] += this.direction[0] * delta;
                this.position[1] += this.direction[1] * delta;

                // Add position to trail
                trail.add(0, new double[] { position[0], position[1] });
                if (trail.size() > TRAIL_LENGTH) {
                    trail.remove(trail.size() - 1);
                }

                lastUpdate = currentTime;
            }

            // Check for decay
            if (currentTime - startTime >= this.decayTime) {
                if (this.afterDecay != null) {
                    this.afterDecay.run();
                }
                this.running = false;
                // TODO
            }

            try {
                Thread.sleep(16); // Approx. 60 updates per second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the bullet thread.
     */
    public void stop() {
        this.running = false;
    }

}
