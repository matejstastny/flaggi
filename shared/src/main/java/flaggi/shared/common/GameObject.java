// ------------------------------------------------------------------------------
// GameObject.java - Stores data about game objects
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 06-21-2025 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.common;

import flaggi.proto.ServerMessages.GameObjectType;
import flaggi.proto.ServerMessages.ServerGameObject;

public class GameObject {

	private GameObjectType type;
	private Hitbox collision;
	private double x, y, hp;

	// Constructors -------------------------------------------------------------

	public GameObject(ServerGameObject o) {
		this(o.getType(), o.getX(), o.getY(), new Hitbox(o.getCollX(), o.getCollY(), o.getCollWidth(), o.getCollHeight()));
	}

	public GameObject(GameObjectType type, double x, double y, Hitbox c) {
		this.collision = c;
		this.type = type;
		this.x = x;
		this.y = y;
	}

	// Public -------------------------------------------------------------------

	public ServerGameObject toProto() {
		return ServerGameObject.newBuilder().setType(type).setX(x).setY(y).setCollX(collision.getX()).setCollY(collision.getY()).setCollWidth(collision.getWidth()).setCollHeight(collision.getHeight()).build();
	}

	// Accesors -----------------------------------------------------------------

	public GameObjectType type() {
		return type;
	}

	public Hitbox collision() {
		return collision;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double hp() {
		return hp;
	}

	// Modifiers ----------------------------------------------------------------

	public void setType(GameObjectType type) {
		this.type = type;
	}

	public void setCollision(Hitbox c) {
		this.collision = c;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setHp(double hp) {

	}
}
