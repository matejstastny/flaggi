/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 12/7/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.util.ImageUtil;

/**
 * Player HUD UI widget.
 */
public class HUD extends Renderable {

    private float health;
    private Image healthTexture, healthFillTexture;

    // Constructor --------------------------------------------------------------

    public HUD() {
        super(ZIndex.HUD, UiTags.GAME_ELEMENTS);
        this.health = 0;
        try {
            this.healthTexture = ImageUtil.scaleToWidth(ImageUtil.getImageFromFile("ui/spray-hp.png"), 100, false);
            this.healthFillTexture = ImageUtil.scaleToWidth(ImageUtil.getImageFromFile("ui/spray-hp-fill.png"), 100, false);
        } catch (IOException e) {
            System.out.println("HUD textures failed to load.");
        }
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {

        // Calculate healthbar data
        int heightDiff = 89;
        double filledPercent = health / 100.0;
        int usableHeight = this.healthTexture.getHeight(null) - heightDiff;
        int emptyHeight = (int) (usableHeight * (1.0 - filledPercent));
        int x = 30;
        int y = size[1] - 90 - this.healthTexture.getHeight(null);
        int fillY = y + heightDiff + emptyHeight;

        // Render the healthbar
        if (this.health > 0) {
            g.drawImage(ImageUtil.cropImage(this.healthFillTexture, 0, heightDiff + emptyHeight, this.healthTexture.getWidth(null), usableHeight - emptyHeight), x, fillY, focusCycleRootAncestor);
        }
        g.drawImage(this.healthTexture, x, y, focusCycleRootAncestor);

    }

    // Private ------------------------------------------------------------------

    public void setHealth(float health) {
        this.health = health;
    }

}
