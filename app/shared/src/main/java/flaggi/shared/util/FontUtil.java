/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 10/29/2024
 * Github link: https://github.com/kireiiiiiiii
 */

package flaggi.shared.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.io.IOException;
import java.io.InputStream;

public final class FontUtil {

    // Private constructor to prevent instantiation
    private FontUtil() {
        throw new UnsupportedOperationException("FontUtil is a utility class and cannot be instantiated.");
    }

    // Font creation -------------------------------------------------------------

    /**
     * Creates a Font object from a resource file.
     *
     * @param resourcePath the path to the font resource file within the classpath
     * @return the created {@code Font} object
     * @throws IOException         if the font file is not found or cannot be read
     * @throws FontFormatException if the font file format is invalid
     */
    public static Font createFontFromResource(String resourcePath) throws IOException, FontFormatException {
        try (InputStream fontStream = FontUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                throw new IOException("Font file not found: " + resourcePath);
            }
            return Font.createFont(Font.TRUETYPE_FONT, fontStream);
        }
    }

    // Position calculation ------------------------------------------------------

    /**
     * Calculates the centered position of a text in a given area.
     *
     * @param width  The width of the rectangle to center within.
     * @param height The height of the rectangle to center within.
     * @param fm     The {@code FontMetrics} of the target font.
     * @param text   The target text.
     * @return An {@code int[]} where index 0 represents {@code x} and index 1
     *         represents {@code y} positions.
     */
    public static int[] getCenteredPosition(int width, int height, FontMetrics fm, String text) {
        return new int[] { (width - fm.stringWidth(text)) / 2, (height - fm.getHeight()) / 2 + fm.getAscent() };
    }

    /**
     * Calculates the text origin point based on alignment.
     *
     * @param fm         The {@code FontMetrics} object to get text dimensions.
     * @param text       The text to be rendered.
     * @param point      The reference point as an {@code int[]} of length 2.
     * @param alignRight If {@code true}, aligns text to the right; otherwise,
     *                   aligns to the left.
     * @return The calculated point for rendering the text.
     */
    private static int[] getTextOrigin(FontMetrics fm, String text, int[] point, boolean alignRight) {
        validatePoint(point);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        return new int[] { alignRight ? point[0] : point[0] - textWidth, point[1] + textHeight };
    }

    /**
     * Calculates the origin point to render the text bottom-right of the given
     * point.
     *
     * @param fm    The {@code FontMetrics} object to get text dimensions.
     * @param text  The text to be rendered.
     * @param point The reference point as an {@code int[]} of length 2.
     * @return The calculated point for rendering the text bottom-right.
     */
    public static int[] getBottomRightTextOrigin(FontMetrics fm, String text, int[] point) {
        return getTextOrigin(fm, text, point, true);
    }

    /**
     * Calculates the origin point to render the text bottom-left of the given
     * point.
     *
     * @param fm    The {@code FontMetrics} object to get text dimensions.
     * @param text  The text to be rendered.
     * @param point The reference point as an {@code int[]} of length 2.
     * @return The calculated point for rendering the text bottom-left.
     */
    public static int[] getBottomLeftOrigin(FontMetrics fm, String text, int[] point) {
        return getTextOrigin(fm, text, point, false);
    }

    // Private methods -----------------------------------------------------------

    /**
     * Validates that a given point array has at least two elements.
     *
     * @param point The point array to validate.
     * @throws IllegalArgumentException if the array is {@code null} or has less
     *                                  than two elements.
     */
    private static void validatePoint(int[] point) {
        if (point == null || point.length < 2) {
            throw new IllegalArgumentException("Point array must have at least two elements.");
        }
    }

}
