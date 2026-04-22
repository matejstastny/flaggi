// ------------------------------------------------------------------------------
// ZIndex.java - Z-index constants class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 07-25-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.client.constants;

/** The HIGHER the z-index, the HIGHER the layer */
public class ZIndex {

  // Private constructor to prevent instantiation
  private ZIndex() {
    throw new UnsupportedOperationException(
        "ZIndex is a constants class and cannot be instantiated.");
  }

  // Global -------------------------------------------------------------------

  public static final int BACKGROUND = 1;
  public static final int GAME = 2;
  public static final int MENU_SCREEN = 2;
  public static final int CONNECTION = 3;
  public static final int ENVIRONMENT_BOTTOM = 9;
  public static final int OTHER_PLAYERS = 10;
  public static final int PLAYER = 11;
  public static final int ENVIRONMENT_TOP = 12;
  public static final int HUD = 18;
  public static final int PAUSE_SCREEN = 20;
  public static final int TOAST = 30;
  public static final int SCREEN_TEST = 100;
}
