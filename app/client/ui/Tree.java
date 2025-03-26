/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/29/2024
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;
import java.util.Arrays;

import flaggi.client.common.Sprite;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Renderable;

/**
 * Tree game object widget.
 */
public class Tree extends Renderable {

    private int[] position;
    private Sprite sprite;

    public Tree(int[] position) {
        super(ZIndex.ENVIRONMENT_BOTTOM, UiTags.GAME_ELEMENTS, UiTags.ENVIRONMENT);
        this.position = position;
        this.sprite = new Sprite();
        this.sprite.addAnimation("tree", Arrays.asList("tree"));
        this.sprite.setAnimation("tree");
    }

    @Override
    public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
        this.sprite.render(g, this.position[0] + origin[0], this.position[1] + origin[1], focusCycleRootAncestor);
    }

}
