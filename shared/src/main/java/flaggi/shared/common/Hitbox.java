// ------------------------------------------------------------------------------
// Hitbox.java - Class that holds hitbox data
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 12/12/2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.common;

public class Hitbox {

  private double x, y, width, height;

  // Constructor --------------------------------------------------------------

  public Hitbox() {
    this(0, 0, 0, 0);
  }

  public Hitbox(double width, double height) {
    this(0, 0, width, height);
  }

  public Hitbox(double x, double y, double width, double height) {
    this.x = x;
    this.y = y;
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
  }

  public Hitbox(Hitbox hitbox) {
    this(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
  }

  // Accesors -----------------------------------------------------------------

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getWidth() {
    return width;
  }

  public double getHeight() {
    return height;
  }

  // Modifiers ----------------------------------------------------------------

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }

  public void setWidth(double width) {
    this.width = Math.max(0, width);
  }

  public void setHeight(double height) {
    this.height = Math.max(0, height);
  }

  public void setLocation(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public void setSize(double width, double height) {
    this.width = Math.max(0, width);
    this.height = Math.max(0, height);
  }

  // Public -------------------------------------------------------------------

  public boolean contains(double px, double py) {
    return px >= x && px < x + width && py >= y && py < y + height;
  }

  public boolean doubleersects(Hitbox other) {
    return other.x < this.x + this.width
        && other.x + other.width > this.x
        && other.y < this.y + this.height
        && other.y + other.height > this.y;
  }

  public Hitbox doubleersection(Hitbox other) {
    double newX = Math.max(this.x, other.x);
    double newY = Math.max(this.y, other.y);
    double newWidth = Math.min(this.x + this.width, other.x + other.width) - newX;
    double newHeight = Math.min(this.y + this.height, other.y + other.height) - newY;

    if (newWidth <= 0 || newHeight <= 0) {
      return new Hitbox();
    }

    return new Hitbox(newX, newY, newWidth, newHeight);
  }

  public boolean contains(Hitbox other) {
    return other.x >= this.x
        && other.y >= this.y
        && other.x + other.width <= this.x + this.width
        && other.y + other.height <= this.y + this.height;
  }

  @Override
  public String toString() {
    return "Rectangle[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
  }
}
