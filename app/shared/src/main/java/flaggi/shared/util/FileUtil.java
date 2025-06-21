/*
 * Author: Matěj Šťastný aka matysta
 * Date created: 12/2/2024
 * GitHub link: https://github.com/matysta/flaggi
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
	 * Gets a list of all directories in the given path. The path must be a jar
	 * relative path.
	 *
	 * @param path - target path.
	 * @return - list of {@code String} dir names.
	 */
	public static String[] retrieveJarDirectoryList(String path) {
		if (!path.endsWith("/")) {
			path += "/";
		}

		List<String> directories = new ArrayList<>();
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
							if (entryName.startsWith(path) && entryName.endsWith("/") && !entryName.equals(path)) {
								directories.add(extractRelativeDirName(path, entryName));
							}

						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return directories.toArray(new String[0]);
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

	// Private methods -----------------------------------------------------------

	/**
	 * Extracts the relative directory name from the given path. For example, if the
	 * original path is "path/to/dir/" and the path is "path/to/dir/subdir/", the
	 * method will return "subdir".
	 *
	 * @see FileUtil#retrieveJarDirectoryList(String)
	 * @param parentDirPath - parent directory path (example: "path/to/dir/").
	 * @param fullPath      - full path (example: "path/to/dir/subdir/").
	 * @return relative directory name (example: "subdir").
	 */
	private static String extractRelativeDirName(String parentDirPath, String fullPath) {
		int frontCut = parentDirPath.length();
		return fullPath.substring(frontCut, fullPath.length() - 1);
	}

}
