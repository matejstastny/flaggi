/*
 * Author: Matěj Šťastný
 * Date created: 2/2/2025
 * Github link: https://github.com/kireiiiiiiii/Flaggi
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package flaggieditor.widgets;

import java.awt.Container;
import java.awt.Graphics2D;

import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.MapData.ObjectData;

/**
 * Map object to be rendered on the map.
 *
 */
public class GameObject extends Renderable {

	/////////////////
	// Variables
	////////////////

	private ObjectData data;

	/////////////////
	// Contructor
	////////////////

	/**
	 * Default constructor.
	 *
	 * @param data - data of the game object.
	 */
	public GameObject(ObjectData data) {
		super(2);
		this.data = data;
	}

	/////////////////
	// Rendering
	////////////////

	@Override
	public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
		// TODO rendering logic
	}

}
