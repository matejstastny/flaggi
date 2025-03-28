/*
 * Author: Matěj Šťastný
 * Date created: 12/1/2024
 * GitHub link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.shared.apiclients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GitHubApiClient is a utility class for interacting with the GitHub API. It
 * provides methods to fetch information about repositories, such as the latest
 * release tag.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * String latestTag = GitHubApiClient.getLatestReleaseTag("octocat", "Hello-World", "your_token_here");
 * }</pre>
 *
 * <p>
 * Note: If a GitHub personal access token is provided, it will be used for
 * authentication to increase the rate limit for API requests.
 * </p>
 *
 * <p>
 * Logging can be disabled by setting the {@code disableLogging} field to
 * {@code true}.
 * </p>
 *
 */
public class GitHubApiClient {

	// Private constructor to prevent instantiation
	private GitHubApiClient() {
		throw new UnsupportedOperationException("GitHubApiClient is a utility class and cannot be instantiated.");
	}

	// Variables -----------------------------------------------------------------

	public static boolean disableLogging = false;
	private static final Logger logger = Logger.getLogger(GitHubApiClient.class.getName());
	private static final String API_BASE_URL = "https://api.github.com/repos/";

	// Data fetching -------------------------------------------------------------

	/**
	 * Fetches the tag of the latest release (including pre-releases) for a given
	 * GitHub repository.
	 *
	 * @param owner the owner of the repository (e.g., "octocat").
	 * @param repo  the name of the repository (e.g., "Hello-World").
	 * @param token optional GitHub personal access token for authentication (can be
	 *              null).
	 * @return the tag name of the latest release, or null if not found.
	 * @throws Exception if an error occurs while fetching the release information.
	 */
	public static String getLatestReleaseTag(String owner, String repo, String token) throws Exception {
		String apiUrl = String.format("%s%s/%s/releases", API_BASE_URL, owner, repo);

		// Open the connection to the GitHub API
		URL url = new URL(apiUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		// Add authorization header if a token is provided
		if (token != null && !token.isEmpty()) {
			connection.setRequestProperty("Authorization", "token " + token);
		}

		// Check the response code
		int responseCode = connection.getResponseCode();
		if (responseCode != 200) {
			if (!disableLogging) {
				logger.log(Level.SEVERE, "Failed to fetch the releases. HTTP Response Code: {0}", responseCode);
			}
			throw new Exception("Failed to fetch latest release. HTTP Response Code: " + responseCode);
		}

		// Read the response
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();

		// Parse the JSON response to get the releases array
		JSONArray releases = new JSONArray(response.toString());

		// Iterate over releases and find the latest one, whether it’s a pre-release or
		// not
		for (int i = 0; i < releases.length(); i++) {
			JSONObject release = releases.getJSONObject(i);
			boolean isPreRelease = release.getBoolean("prerelease");

			// Return the tag name of the latest non-pre-release
			if (!isPreRelease) {
				return release.getString("tag_name");
			}
		}

		// If no non-prerelease found, check for pre-releases
		for (int i = 0; i < releases.length(); i++) {
			JSONObject release = releases.getJSONObject(i);
			if (release.getBoolean("prerelease")) {
				return release.getString("tag_name");
			}
		}

		if (!disableLogging) {
			logger.log(Level.WARNING, "No release tags found.");
		}
		return null;
	}

}
