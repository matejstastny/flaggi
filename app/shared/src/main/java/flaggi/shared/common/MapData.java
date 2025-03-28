/*
 * Author: Matěj Šťastný
 * Date created: 3/2/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
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

package flaggi.shared.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Data class to hold all the data of the world map.
 *
 */
public class MapData {

	/////////////////
	// Variables
	////////////////

	private int objectIdCounter;
	private String name;
	private int width, height;
	private Spawnpoint spawnpoint;
	private List<ObjectData> gameObjects = new ArrayList<ObjectData>();

	/////////////////
	// Constructors
	////////////////

	/**
	 * Default constructor.
	 *
	 * @param name   - display name of the map.
	 * @param width  - width of the map.
	 * @param height - height of the map.
	 */
	public MapData(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.gameObjects = new ArrayList<ObjectData>();
		this.spawnpoint = new Spawnpoint();
		this.objectIdCounter = 0;
	}

	/**
	 * Empty constructor for Jackson serialization.
	 *
	 */
	public MapData() {
		this("Untitled Map", 1000, 1000);
	}

	/////////////////
	// Events
	////////////////

	/**
	 * Prints debug data about the map into the console.
	 *
	 */
	public void printData() {
		System.out.println("Map Name: " + this.name);
		System.out.println("Map Width: " + this.width);
		System.out.println("Map Height: " + this.height);
		System.out.println("Spawnpoint: " + this.spawnpoint);
		System.out.println("Game objects:");
		for (ObjectData o : this.gameObjects) {
			System.out.println("    " + o);
		}
	}

	/**
	 * Adds a new game object to the map.
	 *
	 * @param type - type of the game object.
	 * @param x    - X position.
	 * @param y    - Y position.
	 */
	public void newGameObject(ObjectType type, int x, int y) {
		validateCoordinates(x, y);
		int id = this.objectIdCounter;
		this.objectIdCounter++;
		this.gameObjects.add(new ObjectData(type, id, x, y));
	}

	/////////////////
	// Accesors
	////////////////

	public String getName() {
		return name;
	}

	public List<ObjectData> getGameObjects() {
		return gameObjects;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Spawnpoint getSpawnpoint() {
		return this.spawnpoint;
	}

	/////////////////
	// Modifiers
	////////////////

	public void setName(String name) {
		this.name = name;
	}

	public void setGameObjects(List<ObjectData> gameObjects) {
		this.gameObjects = gameObjects;
	}

	public void setSpawn(int spawnpoint1X, int spawnpoint1Y, int spawnpoint2X, int spawnpoint2Y) {
		validateCoordinates(spawnpoint1X, spawnpoint1Y);
		validateCoordinates(spawnpoint2X, spawnpoint2Y);
		this.spawnpoint = new Spawnpoint(spawnpoint1X, spawnpoint1Y, spawnpoint2X, spawnpoint2Y);
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public MapData scaleMap(int scale) {
		this.width *= scale;
		this.height *= scale;
		this.spawnpoint.oneX *= scale;
		this.spawnpoint.oneY *= scale;
		this.spawnpoint.twoX *= scale;
		this.spawnpoint.twoY *= scale;
		for (ObjectData o : this.gameObjects) {
			o.setX(o.getX() * scale);
			o.setY(o.getY() * scale);
		}
		return this;
	}

	/////////////////
	// Private Methods
	////////////////

	/**
	 * Validates the coordinates to ensure they are within the map boundaries.
	 *
	 * @param x - X coordinate.
	 * @param y - Y coordinate.
	 */
	private void validateCoordinates(int x, int y) {
		if (x > this.width) {
			throw new IllegalArgumentException("X coordinate is outside the map size.");
		}
		if (y > this.height) {
			throw new IllegalArgumentException("Y coordinate is outside the map size.");
		}
		if (x < 0) {
			throw new IllegalArgumentException("X coordinate is smaller than 0.");
		}
		if (y < 0) {
			throw new IllegalArgumentException("Y coordinate is smaller than 0.");
		}
	}

	/////////////////
	// Game Object data
	////////////////

	/**
	 * Data class for the individual game objects.
	 *
	 */
	public static class ObjectData {

		private ObjectType objectType;
		private int x, y, id;

		// ----------------------------------------------

		/**
		 * Default constructor.
		 *
		 * @param objectType - type of the game object.
		 * @param x          - X position.
		 * @param y          - Y position.
		 */
		public ObjectData(ObjectType objectType, int id, int x, int y) {
			this.objectType = objectType;
			this.x = x;
			this.y = y;
			this.id = id;
		}

		/**
		 * Empty constructor for Jackson serialization.
		 */
		public ObjectData() {
			this(null, -1, 0, 0);
		}

		// ----------------------------------------------

		public int getX() {
			return this.x;
		}

		public void setX(int x) {
			this.x = x;
		}

		public int getY() {
			return this.y;
		}

		public void setY(int y) {
			this.y = y;
		}

		public ObjectType getObjectType() {
			return this.objectType;
		}

		public void setObjectType(ObjectType objectType) {
			this.objectType = objectType;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return String.format("ObjectData{id=%d, objectType=%s, x=%d, y=%d}", id, objectType, x, y);
		}

	}

	/////////////////
	// Game object enum
	////////////////

	/**
	 * Enum class with constants for different game object types.
	 *
	 */
	@JsonDeserialize(using = ObjectTypeDeserializer.class)
	public enum ObjectType {

		TREE("tree", 0, 0, 0, 0), //
		RED_FLAG("red_flag", 0, 0, 0, 0), //
		BLUE_FLAG("blue_flag", 0, 0, 0, 0); //

		private final String name;
		private final int collisionX, collisionY, collisionWidth, collisionHeight;

		ObjectType(String name, int collisionX, int collisionY, int collisionWidth, int collisionHeight) {
			this.name = name;
			this.collisionX = collisionX;
			this.collisionY = collisionY;
			this.collisionWidth = collisionWidth;
			this.collisionHeight = collisionHeight;
		}

		// Returns name as JSON value
		@JsonValue
		public String getName() {
			return name;
		}

		// Accessor for collision position
		public int[] getCollisionPos() {
			return new int[] { collisionX, collisionY };
		}

		// Accessor for collision size
		public int[] getCollisionSize() {
			return new int[] { collisionWidth, collisionHeight };
		}

		// Custom toString method
		@Override
		public String toString() {
			return String.format("ObjectType{name='%s', collisionPos=[%d, %d], collisionSize=[%d, %d]}", name, collisionX, collisionY, collisionWidth, collisionHeight);
		}

		// Custom deserialization by name
		@JsonCreator
		public static ObjectType fromName(String name) {
			return Arrays.stream(values()).filter(type -> type.name.equalsIgnoreCase(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown ObjectType: " + name));
		}
	}

	/////////////////
	// Deserialization
	////////////////

	/**
	 * Deseriliazer for the {@code ObjectType} enum.
	 *
	 * @see ObjectType
	 */
	public static class ObjectTypeDeserializer extends JsonDeserializer<ObjectType> {
		@Override
		public ObjectType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			String name = p.getText();
			return ObjectType.fromName(name);
		}
	}

	/////////////////
	// Other structs
	////////////////

	/**
	 * Spawnpoint data class.
	 *
	 */
	public static class Spawnpoint {

		public int oneX, oneY, twoX, twoY;

		/**
		 * Default constructor.
		 *
		 * @param oneX - X coordinate of the first spawn point.
		 * @param oneY - Y coordinate of the first spawn point.
		 * @param twoX - X coordinate of the second spawn point.
		 * @param twoY - Y coordinate of the second spawn point.
		 */
		public Spawnpoint(int oneX, int oneY, int twoX, int twoY) {
			this.oneX = oneX;
			this.oneY = oneY;
			this.twoX = twoX;
			this.twoY = twoY;
		}

		/**
		 * Empty constructor for Jackson serialization.
		 */
		public Spawnpoint() {
			this(0, 0, 0, 0);
		}

		@Override
		public String toString() {
			return String.format("Spawnpoint{oneX=%d, oneY=%d, twoX=%d, twoY=%d}", oneX, oneY, twoX, twoY);
		}

	}

}
