package flaggieditor;

import java.io.File;
import java.io.IOException;

import flaggi.shared.common.MapData;
import flaggi.shared.common.MapData.ObjectType;

public class App {
	public static void main(String[] args) throws IOException {
		MapData map = new MapData("My Awesome Map", 300, 600);
		map.setSpawnpoints(150, 10, 150, 590);
		map.newGameObject(ObjectType.TREE, 150, 300);
		File f = new File("~/sandbox.json");
		if (!f.exists()) {
			f.createNewFile();
		}
		map.saveToFile(f);
	}
}

// /*
// * Author: Matěj Šťastný
// * Date created: 2/2/2025
// * GitHub link: https://github.com/matysta/flaggi
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// */

// package flaggieditor;

// import java.awt.Dimension;
// import java.awt.Toolkit;
// import java.awt.event.KeyEvent;
// import java.awt.event.MouseEvent;
// import java.awt.event.MouseWheelEvent;
// import java.io.File;
// import java.io.IOException;
// import java.util.Scanner;

// import javax.swing.SwingUtilities;

// import flaggi.shared.common.GPanel;
// import flaggi.shared.common.GPanel.InteractableHandler;
// import flaggi.shared.common.MapData;
// import flaggi.shared.common.MapData.ObjectType;
// import flaggi.shared.common.PersistentValue;
// import flaggieditor.widgets.MapRender;

// /**
// * Main application class.
// *
// */
// public class App implements InteractableHandler {

// /////////////////
// // Variables
// ////////////////

// private GPanel gpanel;
// private PersistentValue<MapData> map;

// /////////////////
// // MM & Constr
// ////////////////

// /**
// * Main method.
// */
// public static void main(String[] args) {
// SwingUtilities.invokeLater(App::new);
// }

// /**
// * Constructor.
// */
// public App() {
// // ----- Variable init
// int[] windowSize = getScreenSize();
// Scanner console = new Scanner(System.in);

// // ----- Console
// this.map = setupMap(console);
// this.gpanel = new GPanel(windowSize[0], windowSize[1], false, "Flaggi
// Editor", this);
// this.gpanel.add(new MapRender(map.get()));
// console(console, this.map);
// console.close();
// }

// /////////////////
// // Helpers
// ////////////////

// /**
// * Gets the screen size.
// *
// * @return
// */
// private static int[] getScreenSize() {
// Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
// return new int[] { (int) screenSize.getWidth(), (int) screenSize.getHeight()
// };
// }

// /**
// * User input method.
// *
// * @param console - System in scanner.
// */
// private static void console(Scanner console, PersistentValue<MapData> map) {
// while (true) {
// System.out.print("\n --> Command: ");
// String command = console.nextLine();
// switch (command) {
// case "exit":
// console.close();
// try {
// map.save();
// System.out.println("Map saved successfully.");
// } catch (IOException e) {
// System.out.println("There was an error while saving the map. Map data might
// be lost.");
// }
// return;
// case "new":
// System.out.println("Creating new object...");
// System.out.println("Select the type:");
// int i = 1;
// for (ObjectType type : ObjectType.values()) {
// System.out.println(" " + i + " - " + type.name());
// i++;
// }
// int typeNum = console.nextInt();
// if (typeNum > i - 1 || typeNum < 0) {
// System.out.println("Invalid type number.");
// break;
// }
// ObjectType type = ObjectType.values()[typeNum - 1];
// System.out.print("Position (x y): ");
// int x = console.nextInt();
// int y = console.nextInt();
// map.get().newGameObject(type, x, y);
// System.out.println("Object created.");
// console.nextLine();
// break;
// case "help":
// System.out.println("Commands: exit, help, new");
// break;
// default:
// System.out.println("Commands: exit, help, new");
// System.out.println("Unknown command.");
// break;
// }
// }
// }

// /**
// * The setup of a new map data object
// *
// * @param console - System in scanner.
// * @return - a new {@code AdvancedVariable} object for the map data.
// */
// private static PersistentValue<MapData> setupMap(Scanner console) {
// PersistentValue<MapData> map;

// // Map file ---------------
// while (true) {
// System.out.print("Enter the path to the map file (relative to home
// directory): ");
// String mapPath = System.getenv("HOME") + File.separator + console.nextLine();
// File mapFile = new File(mapPath);
// try {
// mapFile.createNewFile();
// } catch (IOException e) {
// System.out.println("There was an error while creating the file.");
// continue;
// }
// map = new PersistentValue<MapData>(mapFile);
// break;
// }

// // Base map data -----------
// while (true) {
// System.out.print("Name of the map: ");
// String mapName = console.nextLine();
// System.out.print("Size of the map (width height): ");
// String[] mapSize = console.nextLine().split(" ");
// map.set(new MapData(mapName, Integer.parseInt(mapSize[0]),
// Integer.parseInt(mapSize[1])));
// break;
// }

// // Spawnpoint --------------
// while (true) {
// System.out.print("Spawnpoint (x1 y1 x2 y2): ");
// String[] spawnData = console.nextLine().split(" ");
// try {
// map.get().setSpawn(Integer.parseInt(spawnData[0]),
// Integer.parseInt(spawnData[1]), Integer.parseInt(spawnData[2]),
// Integer.parseInt(spawnData[3]));
// } catch (IllegalArgumentException e) {
// System.out.println("Position is outside the map borders.");
// continue;
// }
// break;
// }

// return map;
// }

// /////////////////
// // Interactions
// ////////////////

// @Override
// public void mouseDragged(MouseEvent e) {
// }

// @Override
// public void mouseMoved(MouseEvent e) {
// }

// @Override
// public void mouseClicked(MouseEvent e) {
// }

// @Override
// public void mousePressed(MouseEvent e) {
// }

// @Override
// public void mouseReleased(MouseEvent e) {
// }

// @Override
// public void mouseEntered(MouseEvent e) {
// }

// @Override
// public void mouseExited(MouseEvent e) {
// }

// @Override
// public void mouseWheelMoved(MouseWheelEvent e) {
// }

// @Override
// public void keyTyped(KeyEvent e) {
// }

// @Override
// public void keyPressed(KeyEvent e) {
// }

// @Override
// public void keyReleased(KeyEvent e) {
// }

// /////////////////
// // Tests
// ////////////////

// /**
// * Gets example map data.
// *
// * @return a placeholder {@code MapData} object.
// */
// public static MapData getPlaceholderMap() {
// MapData map = new MapData("My Awsome Map", 5_000, 10_000);

// map.setSpawn(100, 200, 670, 240);
// map.newGameObject(ObjectType.TREE, 500, 1000);
// map.newGameObject(ObjectType.TREE, 720, 976);

// return map;
// }
// }
