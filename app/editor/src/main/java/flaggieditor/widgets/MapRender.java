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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;

import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.MapData;

/**
 * UI widget for rendering the main map.
 *
 */
public class MapRender extends Renderable {

	/////////////////
	// Variables
	////////////////

	private final int grid = 10;
	private MapData map;

	/////////////////
	// Constructor
	////////////////

	/**
	 * Default constructor.
	 *
	 * @param map - data object of the map that is being rendered.
	 */
	public MapRender(MapData map) {
		super(0);
		this.map = map;
	}

	/////////////////
	// Modif & Accesors
	////////////////

	public MapData getMap() {
		return this.map;
	}

	public void setMap(MapData map) {
		this.map = map;
	}

	/////////////////
	// Helpers
	////////////////

	/**
	 * Gets map dimensions to render. The size of the map that is returned should be
	 * the biggest that can fit in the screen while maintaining aspect ratio of the
	 * map sides. It also gets the position the map needs to be drawn on the screen
	 * to be centered in the middle the screen.
	 *
	 * The returned array is in the following format:
	 *
	 * Index 0 - x pos Index 1 - y pos Index 2 - width Index 3 - height
	 *
	 * @param screenWidth  Width of the screen
	 * @param screenHeight Height of the screen
	 * @param mapWidth     Width of the map
	 * @param mapHeight    Height of the map
	 * @return An array containing [x position, y position, width, height]
	 */
	public static int[] getMapDrawScales(int screenWidth, int screenHeight, int mapWidth, int mapHeight) {
		int[] dimensions = new int[4];

		int originalWidth = screenWidth;
		int originalHeight = screenHeight;

		// Apply 10% padding around the screen
		screenWidth = (int) (screenWidth * 0.9);
		screenHeight = (int) (screenHeight * 0.9);

		// Calculate aspect ratios
		double screenRatio = (double) screenWidth / screenHeight;
		double mapRatio = (double) mapWidth / mapHeight;

		int drawWidth, drawHeight;

		// Determine the biggest size that fits while maintaining aspect ratio
		if (mapRatio > screenRatio) {
			drawWidth = screenWidth;
			drawHeight = (int) (screenWidth / mapRatio);
		} else {
			drawHeight = screenHeight;
			drawWidth = (int) (screenHeight * mapRatio);
		}

		// Calculate padding offsets (5% of original screen size)
		int paddingX = (int) (originalWidth * 0.05);
		int paddingY = (int) (originalHeight * 0.05);

		// Calculate x and y position to center the map
		int xPos = (originalWidth - drawWidth) / 2;
		int yPos = (originalHeight - drawHeight) / 2;

		// Apply padding offsets
		xPos = Math.max(xPos, paddingX);
		yPos = Math.max(yPos, paddingY);

		// TODO debug
		xPos = paddingX;
		yPos = paddingY;

		// Assign values to the array
		dimensions[0] = xPos;
		dimensions[1] = yPos;
		dimensions[2] = drawWidth;
		dimensions[3] = drawHeight;

		return dimensions;
	}

	/////////////////
	// Render
	////////////////

	@Override
	public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
		int[] mapDimensions = getMapDrawScales(size[0], size[1], this.map.getWidth(), this.map.getHeight());
		int mapPosX = mapDimensions[0];
		int mapPosY = mapDimensions[1];
		int mapWidth = mapDimensions[2];
		int mapHeight = mapDimensions[3];

		// Render
		g.setColor(Color.RED);
		BasicStroke stroke = new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g.setStroke(stroke);
		g.fillRect(mapPosX, mapPosY, mapWidth, mapHeight);
		g.setColor(Color.BLACK);
		g.drawRect(mapPosX, mapPosY, mapWidth, mapHeight);

	}

}
