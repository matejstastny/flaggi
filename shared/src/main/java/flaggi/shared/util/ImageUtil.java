// ------------------------------------------------------------------------------
// ImageUtil.java - Image utility class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 05-16-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Utility class for handling image loading, manipulation, and scaling. */
public class ImageUtil {

  // Private constructor to prevent instantiation
  private ImageUtil() {
    throw new UnsupportedOperationException(
        "ImageUtil is a utility class and cannot be instantiated.");
  }

  // Create methods ------------------------------------------------------------

  /**
   * Loads an image from the resources folder and returns it as an Image object.
   *
   * @param imageName - file name of the image relative to the resources folder.
   * @return the loaded Image object, or {@code null} if an error occurs.
   * @throws IOException if there is an error reading the image.
   */
  public static Image getImageFromResource(String imageName) throws IOException {
    InputStream imageStream = getImageInputStream(imageName);
    if (imageStream == null) {
      return null;
    }
    return ImageIO.read(imageStream);
  }

  /**
   * Creates a repeated image by tiling the original image.
   *
   * @param imageName - the image name.
   * @param width - the width of the repeated image.
   * @param height - the height of the repeated image.
   * @return the repeated image.
   * @throws IOException if there is an error reading the image.
   */
  public static BufferedImage createTiledImage(Image image, int width, int height)
      throws IOException {
    BufferedImage originalImage = imageToBufferedImage(image);
    BufferedImage repeatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = repeatedImage.createGraphics();
    for (int y = 0; y < height; y += originalImage.getHeight()) {
      for (int x = 0; x < width; x += originalImage.getWidth()) {
        g2d.drawImage(originalImage, x, y, null);
      }
    }
    g2d.dispose();
    return repeatedImage;
  }

  // Scalling ------------------------------------------------------------------

  /**
   * Scales an {@code Image} object to the desired width and height.
   *
   * @param image - target {@code Image} object.
   * @param width - target width of the image.
   * @param height - target height of the image.
   * @param useSmoothScaling - whether to enable smooth scaling.
   * @return a scaled {@code Image} object.
   */
  public static Image scaleImage(Image image, int width, int height, boolean useSmoothScaling) {
    BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics2D = scaledImage.createGraphics();
    if (useSmoothScaling) {
      applyRenderingHints(graphics2D);
    }
    graphics2D.drawImage(image, 0, 0, width, height, null);
    graphics2D.dispose();
    return scaledImage;
  }

  /**
   * Scales an image to a desired width while maintaining aspect ratio.
   *
   * @param image - target {@code Image} object.
   * @param width - target width.
   * @param useSmoothScaling - whether to enable smooth scaling.
   * @return a scaled {@code Image} object.
   */
  public static Image scaleToWidth(Image image, int width, boolean useSmoothScaling) {
    int originalWidth = image.getWidth(null);
    int originalHeight = image.getHeight(null);
    int height = (int) ((double) originalHeight * width / originalWidth);
    return scaleImage(image, width, height, useSmoothScaling);
  }

  /**
   * Scales an image to a desired height while maintaining aspect ratio.
   *
   * @param image - target {@code Image} object.
   * @param height - target height.
   * @param useSmoothScaling - whether to enable smooth scaling.
   * @return a scaled {@code Image} object.
   */
  public static Image scaleToHeight(Image image, int height, boolean useSmoothScaling) {
    int originalWidth = image.getWidth(null);
    int originalHeight = image.getHeight(null);
    int width = (int) ((double) originalWidth * height / originalHeight);
    return scaleImage(image, width, height, useSmoothScaling);
  }

  /**
   * Scales an image to fit within the desired dimensions, maintaining aspect ratio.
   *
   * @param image - target image.
   * @param width - desired width.
   * @param height - desired height.
   * @param useSmoothScaling - whether to enable smooth scaling.
   * @return a scaled {@code Image} object.
   */
  public static Image scaleToFit(Image image, int width, int height, boolean useSmoothScaling) {
    int originalWidth = image.getWidth(null);
    int originalHeight = image.getHeight(null);
    double aspectRatio = (double) originalWidth / originalHeight;

    if (width / aspectRatio <= height) {
      return scaleToWidth(image, width, useSmoothScaling);
    } else {
      return scaleToHeight(image, height, useSmoothScaling);
    }
  }

  public static Image scaleToFill(Image image, int width, int height, boolean useSmoothScaling) {
    int originalWidth = image.getWidth(null);
    int originalHeight = image.getHeight(null);
    double aspectRatio = (double) originalWidth / originalHeight;

    if (width / aspectRatio <= height) {
      return scaleToHeight(image, height, useSmoothScaling);
    } else {
      return scaleToWidth(image, width, useSmoothScaling);
    }
  }

  /**
   * Flips an image vertically.
   *
   * @param originalImage - the image to flip.
   * @return the vertically flipped image.
   */
  public static Image flipImageVertically(Image originalImage) {
    BufferedImage bufferedImage = imageToBufferedImage(originalImage);
    BufferedImage flippedImage =
        new BufferedImage(
            bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getType());
    Graphics2D g2d = flippedImage.createGraphics();
    g2d.drawImage(
        bufferedImage,
        0,
        0,
        bufferedImage.getWidth(),
        bufferedImage.getHeight(),
        bufferedImage.getWidth(),
        0,
        0,
        bufferedImage.getHeight(),
        null);
    g2d.dispose();
    return flippedImage;
  }

  /**
   * Crops an image to the specified dimensions.
   *
   * @param image - the target image.
   * @param startX - the starting x-coordinate.
   * @param startY - the starting y-coordinate.
   * @param width - the width of the cropped image.
   * @param height - the height of the cropped image.
   * @return the cropped image.
   */
  public static BufferedImage cropImage(
      Image image, int startX, int startY, int width, int height) {
    BufferedImage bufferedImage = imageToBufferedImage(image);
    return bufferedImage.getSubimage(startX, startY, width, height);
  }

  // Convertion ----------------------------------------------------------------

  /**
   * Converts an Image to a BufferedImage.
   *
   * @param img - the Image to convert.
   * @return the BufferedImage representation of the Image.
   */
  public static BufferedImage imageToBufferedImage(Image img) {
    if (img instanceof BufferedImage) {
      return (BufferedImage) img;
    }
    BufferedImage bufferedImage =
        new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = bufferedImage.createGraphics();
    g2d.drawImage(img, 0, 0, null);
    g2d.dispose();
    return bufferedImage;
  }

  // Private -------------------------------------------------------------------

  /**
   * Loads an image as an InputStream from the resources folder.
   *
   * @param filePath - the path to the image.
   * @return an InputStream to the image file.
   * @throws IOException if the file does not exist.
   */
  private static InputStream getImageInputStream(String filePath) throws IOException {
    filePath = "/" + filePath;
    InputStream resourceStream = ImageUtil.class.getResourceAsStream(filePath);
    if (resourceStream == null) {
      throw new IOException("Image file not found: " + filePath);
    }
    return resourceStream;
  }

  /**
   * Applies rendering hints to improve image quality.
   *
   * @param graphics2D - the Graphics2D object to configure.
   * @param useSmoothScaling - whether to enable smooth scaling.
   */
  private static void applyRenderingHints(Graphics2D graphics2D) {
    graphics2D.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
  }
}
