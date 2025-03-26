/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 7/25/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;

public class ScreenTest extends Renderable {

    public ScreenTest() {
        super(ZIndex.SCREEN_TEST, UiTags.DEBUG);
    }

    @Override
    public void render(Graphics2D g, int[] size, int[] viewportOffset, Container focusCycleRootAncestor) {
        int rectWidth = 200;
        int rectHeight = 200;
        g.setStroke(new BasicStroke(10));

        // Top-left corner
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, rectWidth, rectHeight);
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, rectWidth, rectHeight);

        // Top-right corner
        g.setColor(Color.BLACK);
        g.drawRect(size[0] - rectWidth, 0, rectWidth, rectHeight);
        g.setColor(Color.BLUE);
        g.fillRect(size[0] - rectWidth, 0, rectWidth, rectHeight);

        // Bottom-left corner
        g.setColor(Color.BLACK);
        g.drawRect(0, size[1] - rectHeight, rectWidth, rectHeight);
        g.setColor(Color.BLUE);
        g.fillRect(0, size[1] - rectHeight, rectWidth, rectHeight);

        // Bottom-right corner
        g.setColor(Color.BLACK);
        g.drawRect(size[0] - rectWidth, size[1] - rectHeight, rectWidth, rectHeight);
        g.setColor(Color.BLUE);
        g.fillRect(size[0] - rectWidth, size[1] - rectHeight, rectWidth, rectHeight);
    }

}
