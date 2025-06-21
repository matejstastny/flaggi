/*
 * Author: Matěj Šťastný ala Kirei
 * Date created: 1/0/2024
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Interactable;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.util.FontUtil;

/**
 * A basic yes/no user confirmation window.
 */
public class ConfirmationWindow extends Renderable implements Interactable {

	private final Color accept = new Color(61, 255, 88);
	private final Color deny = new Color(238, 57, 57);

	private String question;
	private Runnable acceptAction;
	private Runnable denyAction;
	private boolean active;

	// Constructors -------------------------------------------------------------

	public ConfirmationWindow() {
		super(ZIndex.TOAST, PanelRegion.CENTER, UiTags.ALWAYS_VISIBLE);
		setVisibility(false);
	}

	public void newConfirmation(String question, Runnable acceptAction, Runnable denyAction) {
		this.acceptAction = acceptAction;
		this.denyAction = denyAction;
		this.question = question;
		active = true;
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, int[] viewportOffset, Container focusCycleRootAncestor) {
		if (!active) {
			return;
		}
		g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(5)));

		g.setColor(new Color(152, 152, 152));
		g.fill(getWindow());
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(px(0.5)));
		g.draw(getWindow());

		// Draw the question
		int[] position = FontUtil.getCenteredPosition(px(100), px(100), g.getFontMetrics(), question);
		g.drawString(question, position[0], px(40));

		// Draw the buttons
		g.setColor(deny);
		g.fill(getNoButton());
		g.setColor(accept);
		g.fill(getYesButton());
		g.setColor(Color.BLACK);
		g.draw(getYesButton());
		g.draw(getNoButton());

	}

	// Interaction --------------------------------------------------------------

	@Override
	public void interact(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (getYesButton().contains(x, y)) {
			acceptAction.run();
			active = false;
		} else if (getNoButton().contains(x, y)) {
			denyAction.run();
			active = false;
		}
	}

	@Override
	public boolean wasInteracted(MouseEvent e) {
		return active;
	}

	// Shapes -------------------------------------------------------------------

	private Shape getWindow() {
		return new RoundRectangle2D.Double(px(3), px(25), px(94), px(50), px(4), px(4));
	}

	private Shape getNoButton() {
		return new RoundRectangle2D.Double(px(10), px(60), px(30), px(10), px(3), px(3));
	}

	private Shape getYesButton() {
		return new RoundRectangle2D.Double(px(60), px(60), px(30), px(10), px(3), px(3));
	}

}
