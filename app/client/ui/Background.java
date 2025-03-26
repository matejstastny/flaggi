/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/6/2024
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
 * Background widget.
 */
public class Background extends Renderable {

    public Background() {
        super(ZIndex.BACKGROUND, UiTags.MENU_ELEMENTS, UiTags.GAME_ELEMENTS);
    }

    @Override
    public void render(Graphics2D g, int[] size, int[] viewportOffset, Container focusCycleRootAncestor) {
        g.setColor(new Color(153, 192, 255));
        g.fillRect(0, 0, size[0], size[1]);
    }

}
