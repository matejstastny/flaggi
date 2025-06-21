/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.client.ui;

import java.awt.Container;
import java.awt.Graphics2D;

import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;

public class GameRoom extends Renderable {

	// Constructor --------------------------------------------------------------

	public GameRoom(String mapJson) {
		super(ZIndex.GAME, PanelRegion.CENTER);
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, Container focusCycleRootAncestor) {

	}
}
