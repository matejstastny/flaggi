// ------------------------------------------------------------------------------
// GitHubApiClient.java - GitHub API client
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 12-01-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.apiclients;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GitHubApiClient {

  private GitHubApiClient() {
    throw new UnsupportedOperationException(
        "GitHubApiClient is a utility class and cannot be instantiated.");
  }

  public static boolean disableLogging = false;
  private static final Logger logger = Logger.getLogger(GitHubApiClient.class.getName());
  private static final String API_BASE_URL = "https://api.github.com/repos/";

  /**
   * Fetches the tag of the latest release (including pre-releases) for a given GitHub repository.
   *
   * @param owner the owner of the repository (e.g., "octocat").
   * @param repo the name of the repository (e.g., "Hello-World").
   * @param token optional GitHub personal access token for authentication.
   * @return the tag name of the latest release, or null if not found.
   * @throws Exception if an error occurs while fetching the release information.
   */
  public static String getLatestReleaseTag(String owner, String repo, String token)
      throws Exception {
    String apiUrl = String.format("%s%s/%s/releases", API_BASE_URL, owner, repo);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(apiUrl)).GET();

    if (token != null && !token.isBlank()) {
      requestBuilder.header("Authorization", "token " + token);
    }

    HttpRequest request = requestBuilder.build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      if (!disableLogging) {
        logger.log(
            Level.SEVERE, "Failed to fetch releases. HTTP Status: {0}", response.statusCode());
      }
      throw new Exception("Failed to fetch latest release. HTTP Status: " + response.statusCode());
    }

    JSONArray releases = new JSONArray(response.body());

    // Find the first non-prerelease
    for (int i = 0; i < releases.length(); i++) {
      JSONObject release = releases.getJSONObject(i);
      if (!release.getBoolean("prerelease")) {
        return release.getString("tag_name");
      }
    }

    // If none found, return first prerelease
    for (int i = 0; i < releases.length(); i++) {
      JSONObject release = releases.getJSONObject(i);
      if (release.getBoolean("prerelease")) {
        return release.getString("tag_name");
      }
    }

    if (!disableLogging) {
      logger.warning("No release tags found.");
    }
    return null;
  }
}
