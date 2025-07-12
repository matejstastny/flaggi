/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 6/21/2025
 * GitHub link: https://github.com/matysta/flaggi
 */

package flaggi.shared.common;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A serializable data class to hold map data
 */
public class MapData {

	private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private volatile int objectIdCounter;
	private volatile String name;
	private volatile int width, height;
	private volatile Spawnpoint spawnpoint;
	private final List<ObjectData> gameObjects = new CopyOnWriteArrayList<>();

	// Constructors -------------------------------------------------------------

	public MapData(String name, int width, int height) {
		this.name = name;
		this.width = width;
		this.height = height;
		this.objectIdCounter = 0;
		this.spawnpoint = new Spawnpoint();
	}

	// for JSON serialization
	@JsonCreator
	public MapData() {
		this("Untitled Map", 1000, 1000);
	}

	// Public -------------------------------------------------------------------

	public synchronized void newGameObject(ObjectType type, int x, int y) {
		isInBounds(x, y);
		int id = this.objectIdCounter++;
		this.gameObjects.add(new ObjectData(type, id, x, y));
	}

	public void logMapDetails() {
		System.out.println("Map Name: " + this.name);
		System.out.println("Map Width: " + this.width);
		System.out.println("Map Height: " + this.height);
		System.out.println("Spawnpoint: " + this.spawnpoint);
		System.out.println("Game objects:");
		this.gameObjects.forEach(o -> System.out.println("    " + o));
	}

	// JSON ---------------------------------------------------------------------

	public String serialize() throws IOException {
		return objectMapper.writeValueAsString(this);
	}

	public static MapData fromJson(String json) throws IOException {
		return objectMapper.readValue(json, MapData.class);
	}

	public void saveToFile(File file) throws IOException {
		objectMapper.writeValue(file, this);
	}

	public static MapData loadFromFile(File file) throws IOException {
		return objectMapper.readValue(file, MapData.class);
	}

	// Accesors -----------------------------------------------------------------

	public synchronized String getName() {
		return name;
	}

	public synchronized int getWidth() {
		return width;
	}

	public synchronized int getHeight() {
		return height;
	}

	public List<ObjectData> getGameObjects() {
		return gameObjects;
	}

	public synchronized Spawnpoint getSpawnpoint() {
		return this.spawnpoint;
	}

	// Modifiers ----------------------------------------------------------------

	public synchronized void setName(String name) {
		this.name = name;
	}

	public synchronized void setSpawnpoints(int spawnpoint1X, int spawnpoint1Y, int spawnpoint2X, int spawnpoint2Y) {
		isInBounds(spawnpoint1X, spawnpoint1Y);
		isInBounds(spawnpoint2X, spawnpoint2Y);
		this.spawnpoint = new Spawnpoint(spawnpoint1X, spawnpoint1Y, spawnpoint2X, spawnpoint2Y);
	}

	public synchronized void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	// Private ------------------------------------------------------------------

	private void isInBounds(int x, int y) {
		if (x < 0 || x > width || y < 0 || y > height) {
			throw new IllegalArgumentException(String.format("Coordinates (%d, %d) are outside map bounds (%d x %d)", x, y, width, height));
		}
	}

	// Object Type --------------------------------------------------------------

	@JsonDeserialize(using = ObjectTypeDeserializer.class)
	public enum ObjectType {

		TREE("tree"), //
		RED_FLAG("red_flag"), //
		BLUE_FLAG("blue_flag"); //

		private final String name;

		ObjectType(String name) {
			this.name = name;
		}

		@JsonValue
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return String.format("ObjectType{name='%s'}", name);
		}

		@JsonCreator
		public static ObjectType fromName(String name) {
			return Arrays.stream(values()).filter(type -> type.name.equalsIgnoreCase(name)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown ObjectType: " + name));
		}
	}

	// Object Data --------------------------------------------------------------

	public static class ObjectData {

		private ObjectType objectType;
		private int x, y, id;

		public ObjectData(ObjectType objectType, int id, int x, int y) {
			this.objectType = objectType;
			this.x = x;
			this.y = y;
			this.id = id;
		}

		// For JSON serialization
		public ObjectData() {
			this(null, -1, 0, 0);
		}

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

	// Deserialization ----------------------------------------------------------

	public static class ObjectTypeDeserializer extends JsonDeserializer<ObjectType> {
		@Override
		public ObjectType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			String name = p.getText();
			return ObjectType.fromName(name);
		}
	}

	// Spawnpoint ---------------------------------------------------------------

	public static class Spawnpoint {

		private int oneX, oneY, twoX, twoY;

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

		// JSON serialization
		public Spawnpoint() {
			this(0, 0, 0, 0);
		}

		public int getOneX() {
			return oneX;
		}

		public int getOneY() {
			return oneY;
		}

		public int getTwoX() {
			return twoX;
		}

		public int getTwoY() {
			return twoY;
		}

		@Override
		public String toString() {
			return String.format("Spawnpoint{oneX=%d, oneY=%d, twoX=%d, twoY=%d}", oneX, oneY, twoX, twoY);
		}
	}
}
