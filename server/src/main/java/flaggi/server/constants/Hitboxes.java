// ------------------------------------------------------------------------------
// Hitboxes.java - Constant hitbox values
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 12/12/2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.server.constants;

import flaggi.shared.common.Hitbox;

public class Hitboxes {

	// Private constructor to prevent instantiation
	private Hitboxes() {
		throw new UnsupportedOperationException("Hitboxes is a constants class and cannot be instantiated.");
	}

	// Hitboxes ----------------------------------------------------------------

	public static Hitbox player() {
		return new Hitbox(-2.5, -2.5, 5, 5);
	}

}
