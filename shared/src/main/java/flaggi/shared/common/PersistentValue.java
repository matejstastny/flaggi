// ------------------------------------------------------------------------------
// PersistentValue.java - A JSON-serializable variable
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 05-05-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 * <h2>Persistent value</h2>
 *
 * A utility class for managing variables and persisting them to JSON files. This class allows
 * storing variables of any type and saving them to JSON files for later retrieval. <hr>
 *
 * <h3><b>Dependencies:</b></h3>
 *
 * <ul>
 *   <li><a href= "https://github.com/FasterXML/jackson">com.fasterxml.jackson</a>
 * </ul>
 *
 * <p><hr>
 *
 * @param <T> The type of the variable to be stored and managed.
 */
public class PersistentValue<T> {

  // Variables -----------------------------------------------------------------

  private static final Logger LOGGER = Logger.getLogger(PersistentValue.class.getName());
  private T storedValue;
  private File saveFile;
  private final ObjectMapper objectMapper;

  // Constructors --------------------------------------------------------------

  /** Internal constructor that initializes the object mapper. */
  private PersistentValue() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  /**
   * Constructor with a file reference, that will load the variable from the file if it
   *
   * @param saveFilePath The path to the file where the variable will be saved.
   * @throws IOException If the file cannot be created.
   */
  public PersistentValue(File saveFile, Class<T> type) throws IOException {
    this(saveFile);
    loadFromFile(type);
  }

  /**
   * Constructor with a file path, that will load the variable from the file if it
   *
   * @param saveFilePath The path to the file where the variable will be saved.
   * @throws IOException If the file cannot be created.
   */
  public PersistentValue(String saveFilePath, Class<T> type) throws IOException {
    this(new File(saveFilePath), type);
  }

  /**
   * Constructor with a file reference. Does not load the variable from the file.
   *
   * @param saveFile The file where the variable will be saved.
   */
  public PersistentValue(File saveFile) {
    this();
    this.saveFile = saveFile;
  }

  /**
   * Constructor with a file path. Does not load the variable from the file.
   *
   * @param saveFilePath The path to the file where the variable will be saved.
   */
  public PersistentValue(String saveFilePath) {
    this(new File(saveFilePath));
  }

  /**
   * Constructor with a file reference and an initial value.
   *
   * @param saveFile The file where the variable will be saved.
   * @param value The initial value of the variable.
   */
  public PersistentValue(File saveFile, T value) {
    this();
    this.saveFile = saveFile;
    this.set(value);
  }

  /**
   * Constructor with a file path and an initial value.
   *
   * @param saveFilePath The path to the file where the variable will be saved.
   * @param value The initial value of the variable.
   */
  public PersistentValue(String saveFilePath, T value) {
    this(new File(saveFilePath), value);
  }

  // Public --------------------------------------------------------------------

  public synchronized T get() {
    return storedValue;
  }

  public synchronized void set(T value) {
    this.storedValue = value;
  }

  public synchronized void setAndSave(T value) throws IOException {
    this.set(value);
    this.save();
  }

  public synchronized void save() throws IOException {
    objectMapper.writeValue(this.saveFile, storedValue);
  }

  public synchronized boolean loadFromFile(Class<T> valueType) {
    if (this.saveFile.exists()) {
      try {
        storedValue = this.objectMapper.readValue(this.saveFile, valueType);
        return true;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to load from file", e);
      }
    }
    return false;
  }

  // Static --------------------------------------------------------------------

  /**
   * Loads a JSON file and returns the specfied object.
   *
   * @param <T> The type of the object to be returned.
   * @param json The JSON string to be parsed.
   * @param valueType The class of the object to be returned.
   * @return The object parsed from the JSON string.
   */
  public static <T> T fromJson(String json, Class<T> valueType) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(json, valueType);
    } catch (JsonProcessingException e) {
      LOGGER.log(Level.SEVERE, "Failed to parse JSON", e);
      return null;
    }
  }

  /**
   * Converts an object to a single line JSON {@code String}.
   *
   * @param <T> - The type of the object to be converted.
   * @param object - The object to be converted.
   * @return The JSON {@code String} representation of the object.
   * @throws JsonProcessingException If the object cannot be converted to JSON.
   */
  public static <T> String toJson(T object) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(object);
  }

  /**
   * Reads the contents of a resource file bundled with the application (e.g., inside the resources
   * folder in a JAR) and returns it as a {@code String}.
   *
   * @param resourceName The name of the resource file.
   * @return The contents of the resource file.
   * @throws IOException If the resource file cannot be read.
   */
  public static String readResource(String resourceName) throws IOException {
    InputStream inputStream =
        PersistentValue.class.getClassLoader().getResourceAsStream(resourceName);
    if (inputStream == null) {
      return null;
    }

    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append(System.lineSeparator());
      }
    }
    return content.toString();
  }

  /**
   * Writes a {@code String} to a file.
   *
   * @param filePath The path to the file.
   * @param contents The contents to be written.
   * @throws IOException If the file cannot be written.
   */
  public static void writeToFile(String filePath, String contents) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(contents);
    }
  }
}
