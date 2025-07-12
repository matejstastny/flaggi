/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 12/2/2024
 * GitHub link: https://github.com/my-daarlin/flaggi
 */

package flaggi.shared.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * TODO header, when fully implemented.
 */
public class FileUtil {

	// Private constructor to prevent instantiation
	private FileUtil() {
		throw new UnsupportedOperationException("FileUtil is a utility class and cannot be instantiated.");
	}

	// Path fetchers -------------------------------------------------------------

	public static String getJarExecDirectory() {
		try {
			return new File(FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
		} catch (Exception e) {
			throw new RuntimeException("Failed to get JAR directory", e);
		}
	}

	public static String getApplicationDataFolder() {
		String os = System.getProperty("os.name").toLowerCase();
		String appDataFolder = System.getenv("APPDATA");

		if (os.contains("mac")) {
			appDataFolder = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support";
		} else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			appDataFolder = System.getProperty("user.home") + File.separator + ".config";
		} else if (appDataFolder == null) {
			appDataFolder = File.separator;
		}

		File folder = new File(appDataFolder);
		if (!folder.exists() && !folder.mkdirs()) {
			throw new RuntimeException("Failed to create application data folder at: " + appDataFolder);
		}
		return appDataFolder;
	}

	// JAR resources -------------------------------------------------------------

	/**
	 * Lists either directories or files from a given path inside a JAR or classpath
	 * folder.
	 *
	 * @param path      The internal path (must be JAR-relative, e.g.
	 *                  "assets/sprites").
	 * @param extension Behavior is based on this: - "" (empty string) → list
	 *                  directories only - null → list all files - "ext" → list only
	 *                  files with that extension (e.g. "png")
	 * @return A list of matching directory or file names (just the base names).
	 */
	public static List<String> retrieveJarEntries(String path, String extension) {
		if (!path.endsWith("/")) {
			path += "/";
		}

		List<String> results = new ArrayList<>();

		try {
			Enumeration<URL> resources = FileUtil.class.getClassLoader().getResources(path);
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				if ("jar".equals(resource.getProtocol())) {
					JarURLConnection connection = (JarURLConnection) resource.openConnection();
					try (JarFile jarFile = connection.getJarFile()) {
						Enumeration<JarEntry> entries = jarFile.entries();
						while (entries.hasMoreElements()) {
							JarEntry entry = entries.nextElement();
							String entryName = entry.getName();

							if (entryName.startsWith(path) && !entryName.equals(path)) {
								String relativeName = entryName.substring(path.length());

								// Skip nested entries
								if (relativeName.contains("/"))
									continue;

								if (extension != null && extension.isEmpty()) {
									// Directories only
									if (entry.isDirectory()) {
										results.add(relativeName);
									}
								} else {
									// Files only
									if (!entry.isDirectory()) {
										if (extension == null || relativeName.endsWith("." + extension)) {
											results.add(relativeName);
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

	public static void copyResource(String resourcePath, String outputPath) throws IOException {
		Path outputFile = Paths.get(outputPath);
		if (Files.exists(outputFile)) {
			return; // Do nothing if the file already exists
		}

		try (InputStream inputStream = FileUtil.class.getResourceAsStream(resourcePath)) {
			if (inputStream == null) {
				throw new FileNotFoundException("Resource not found: " + resourcePath);
			}
			Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static void copyResourceDirectory(String resourceDir, String outputDir) throws IOException, URISyntaxException {
		File directory = new File(Objects.requireNonNull(FileUtil.class.getResource(resourceDir)).toURI());
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Resource path is not a directory: " + resourceDir);
		}

		Files.walk(directory.toPath()).forEach(source -> {
			Path destination = Paths.get(outputDir, directory.toPath().relativize(source).toString());
			try {
				if (Files.exists(destination)) {
					return; // Do nothing if the file or directory already exists
				}

				if (Files.isDirectory(source)) {
					Files.createDirectories(destination);
				} else {
					Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}
