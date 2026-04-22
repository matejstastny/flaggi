package flaggi.shared.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;

public class VhGraphics {
    private final Graphics2D g;
    private final double pxPerVh;

    public VhGraphics(Graphics2D g, double pxPerVh) {
        this.g = g;
        this.pxPerVh = pxPerVh;
    }

    private int px(double vh) {
        return (int) (vh * pxPerVh);
    }

    public void drawRect(double x, double y, double w, double h) {
        g.drawRect(px(x), px(y), px(w), px(h));
    }

    public void fillRect(double x, double y, double w, double h) {
        g.fillRect(px(x), px(y), px(w), px(h));
    }

    public void drawString(String s, double x, double y) {
        g.drawString(s, px(x), px(y));
    }

    public void setFont(Font font, double sizevh) {
        g.setFont(font.deriveFont(Font.PLAIN, px(sizevh)));
    }

    public void setFont(Font font, int style, double sizevh) {
        g.setFont(font.deriveFont(style, px(sizevh)));
    }

    public void setStroke(int w) {
        g.setStroke(new BasicStroke(px(w)));
    }

    public void drawImage(Image img, double x, double y, double w, double h) {
        g.drawImage(img, px(x), px(y), px(w), px(h), null);
    }

    public void drawOval(double x, double y, double w, double h) {
        g.drawOval(px(x), px(y), px(w), px(h));
    }

    public Graphics2D raw() {
        return g;
    }

    public void drawRawShape(Shape s) {
        g.draw(s);
    }

    public void setColor(Color c) {
        g.setColor(c);
    }

    public void setStroke(Stroke s) {
        g.setStroke(s);
    }

    public RoundRectangle2D vhRoundRectangle(double x, double y, double w, double h, double rw, double rh) {
        return new RoundRectangle2D.Double(px(x), px(y), px(w), px(h), px(rw), px(rh));
    }
}
