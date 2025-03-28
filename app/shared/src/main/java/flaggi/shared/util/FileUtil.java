/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 12/2/2024
 * Github link: https://github.com/kireiiiiiiii
 */

package flaggi.shared.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * TODO header, when fully implemented.
 */
public class FileUtil {

	// Private constructor to prevent instantiation
	private FileUtil() {
		throw new UnsupportedOperationException("FontUtil is a utility class and cannot be instantiated.");
	}

	// Path fetchers -------------------------------------------------------------

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
