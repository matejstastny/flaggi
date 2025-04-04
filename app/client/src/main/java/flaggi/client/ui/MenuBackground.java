/*
* Author: Matěj Šťastný aka Kirei
* Date created: 03/25/2025
* Github link: https://github.com/kireiiiiiiii/flaggi
*/

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.IOException;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.Logger;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.ImageUtil;

/**
 * Main menu background
 */
public class MenuBackground extends Renderable {

    public Image background;

    public MenuBackground() {
        super(ZIndex.BACKGROUND, PanelRegion.BACKGROUND, UiTags.MAIN_MENU);
        try {
            this.background = ImageUtil.getImageFromFile("ui/menu_screen.png");
        } catch (IOException e) {
            Logger.log(LogLevel.ERROR, "Failed to load background texture.");
        }
    }

    @Override
    public void render(Graphics2D g, int[] viewportOffset, Container focusCycleRootAncestor) {
        drawBackground(g, background, focusCycleRootAncestor);
    }

}
