/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/8/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;

import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.client.constants.UiTags;

/**
 * Widget displayed on server connection.
 *
 */
public class ConnectionWidget extends Renderable {

    private static final int RADIUS = 5;

    public ConnectionWidget() {
        super(ZIndex.CONNECTION, UiTags.GAME_ELEMENTS, UiTags.MENU_ELEMENTS);
    }

    @Override
    public void render(Graphics2D g, int[] size, int[] viewportOffset, Container focusCycleRootAncestor) {
        g.setColor(Color.GREEN);
        g.fillOval(size[0] - RADIUS * 3, RADIUS, RADIUS * 2, RADIUS * 2);
    }

}
