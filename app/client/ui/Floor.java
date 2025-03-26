/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/6/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.util.ImageUtil;

/**
 * Tileable floor texture
 */
public class Floor extends Renderable {

    private BufferedImage texture;

    public Floor(int[] size) {
        super(ZIndex.FLOOR, UiTags.GAME_ELEMENTS);
        try {
            this.texture = ImageUtil.createRepeatedImage("sprites/floor-tile.png", size[0], size[1]);
        } catch (IOException e) {
            System.out.println("There was an error while converting the floor texture.");
        }
    }

    @Override
    public void render(Graphics2D g, int[] size, int[] viewportOffset, Container focusCycleRootAncestor) {
        // Floor texture
        g.drawImage(this.texture, viewportOffset[0], viewportOffset[1], focusCycleRootAncestor);

        // Border
        g.setStroke(new BasicStroke(5));
        g.setColor(Color.BLACK);
        g.drawRect(viewportOffset[0], viewportOffset[1], this.texture.getWidth(), this.texture.getHeight());
    }

}
