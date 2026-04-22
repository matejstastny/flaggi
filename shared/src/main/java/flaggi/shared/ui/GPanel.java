// ------------------------------------------------------------------------------
// GPanel.java - Main graphics class
// ------------------------------------------------------------------------------
// Author: Matej Stastny
// Date: 07-23-2024 (MM-DD-YYYY)
// License: MIT
// Link: https://github.com/matejstastny/flaggi
// ------------------------------------------------------------------------------

package flaggi.shared.ui;

import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 *
 * <h2>GPanel</h2>
 *
 * A Swing-based JPanel with custom rendering, a managed rendering loop, and a flexible region-based
 * layout for UI elements.
 *
 * <h3>Features:</h3>
 *
 * <ul>
 *   <li>Custom rendering with adjustable FPS
 *   <li>Thread-safe widget management
 *   <li>Region-based UI layout for structured rendering
 *   <li>Event handling for mouse, keyboard, and scrolling
 *   <li>Automatic resizing with aspect-ratio constraints
 * </ul>
 *
 * @author Matěj Šťastný
 * @since 07-23-2024
 */
public class GPanel extends JPanel
    implements MouseListener, MouseMotionListener, KeyListener, MouseWheelListener {

  private final Map<PanelRegion, Rectangle> regions;
  private final RenderingEngine renderingEngine;
  private final List<Renderable> widgets;
  private InteractableHandler handler;
  private boolean isRendering;
  private JFrame appFrame;

  // Constructor ---------------------------------------------------------------

  /**
   * Constructs a GPanel with the specified parameters.
   *
   * @param windowWidth - window width.
   * @param windowHeight - window height.
   * @param resizable - if the panel is resizable by the user.
   * @param fullscreen - if the panel is fullscreen. Cannot be changed.
   * @param appTitle - title of the window (name of the app).
   * @param handeler - {@code InteractableHandeler} object, that will handle panel interaction.
   */
  public GPanel(int windowWidth, int windowHeight, boolean resizable, String appTitle) {
    this.renderingEngine = new RenderingEngine();
    this.widgets = new CopyOnWriteArrayList<Renderable>();
    this.regions = new EnumMap<>(PanelRegion.class);
    this.isRendering = false;
    this.handler = null;
    this.setPreferredSize(new Dimension(windowWidth, windowHeight));
    this.appFrame = getDefaultJFrame(resizable, appTitle);

    setupListeners();
    startRendering();
  }

  // Rendering -----------------------------------------------------------------

  public void startRendering() {
    if (!this.isRendering && (this.isRendering = true)) {
      this.renderingEngine.start();
    }
  }

  public void stopRendering() {
    if (this.isRendering && !(this.isRendering = false)) {
      this.renderingEngine.stop();
    }
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    Graphics2D g = (Graphics2D) graphics;
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    regions.forEach((region, bounds) -> renderRegion(g, region, bounds));
  }

  // Accesors -----------------------------------------------------------------cv

  public JFrame getAppFrame() {
    return this.appFrame;
  }

  public AtomicInteger getFps() {
    return this.renderingEngine.getFps();
  }

  public <T> ArrayList<T> getWidgetsOfClass(Class<T> targetClass) {
    synchronized (this.widgets) {
      ArrayList<T> list = new ArrayList<>();
      this.widgets.stream()
          .filter(targetClass::isInstance)
          .forEach(r -> list.add(targetClass.cast(r)));
      return list;
    }
  }

  public ArrayList<Renderable> getInteractables() {
    return getWidgetsByInterface(Interactable.class);
  }

  public ArrayList<Renderable> getTypables() {
    return getWidgetsByInterface(Typable.class);
  }

  public ArrayList<Renderable> getScrollables() {
    return getWidgetsByInterface(Scrollable.class);
  }

  // Modifiers ----------------------------------------------------------------

  /**
   * Clears the widgets list, and sets it to the one given as parameter.
   *
   * @param widgets - target widget list.
   */
  public void setWidgets(ArrayList<Renderable> widgets) {
    synchronized (this.widgets) {
      this.widgets.clear();
      this.add(widgets);
    }
  }

  public void add(Renderable... renderable) {
    synchronized (this.widgets) {
      for (Renderable r : renderable) {
        int value = r.getZIndex();
        int index = binarySearchInsertZIndex(value);
        this.widgets.add(index, r);
      }
    }
    updateRegions();
  }

  public void add(List<Renderable> widgets) {
    synchronized (this.widgets) {
      widgets.forEach(w -> this.widgets.add(w));
    }
  }

  public boolean remove(Renderable renderable) {
    synchronized (this.widgets) {
      return this.widgets.remove(renderable);
    }
  }

  public <T> void removeWidgetsOfClass(Class<T> c) {
    synchronized (this.widgets) {
      this.widgets.removeIf(r -> c.isInstance(r));
    }
  }

  public void removeWidgetsWithTag(String tag) {
    synchronized (this.widgets) {
      this.widgets.removeIf(r -> r.getTags().contains(tag));
    }
  }

  /**
   * Sets the maximum FPS of the rendering engine. If set to 0, the FPS will be unlimited.
   *
   * @param fps - target FPS.
   */
  public void setFpsCap(int fps) {
    this.renderingEngine.setFpsCap(fps);
  }

  /**
   * Changes the application icon. Works for MacOS dock icon too.
   *
   * @param path - path of the icon
   */
  public void setIcon(Image icon) {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      try {
        Taskbar.getTaskbar().setIconImage(icon);
      } catch (Exception e) {
        Logger.log(LogLevel.WRN, "Taskbar icon could not be set", e);
      }
    }
    this.appFrame.setIconImage(icon);
  }

  /**
   * OS specific icon setter, that sets a different icon depeending on the OS.
   *
   * @param winIcon - icon for Window OS.
   * @param macIcon - icon for MacOS.
   * @param linuxIcon - icon for Linux based OS.
   * @param other - other not common OS.
   */
  public void setIconOSDependend(Image winIcon, Image macIcon, Image linuxIcon, Image other) {
    String os = System.getProperty("os.name").toLowerCase();
    Image icon;
    if (os.contains("win")) {
      icon = winIcon;
    } else if (os.contains("mac")) {
      icon = macIcon;
    } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
      icon = linuxIcon;
    } else {
      icon = other;
    }
    setIcon(icon);
  }

  /**
   * Sets an action meant to be performed when the JPanel window is closed.
   *
   * @param operation - {@code Runnable} executed on window close.
   */
  public void setExitOperation(Runnable operation) {
    this.appFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.appFrame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            operation.run();
          }
        });
  }

  /**
   * Sets an on object that will handle the user interaction externally.
   *
   * @param handler
   */
  public void setInteractableHandler(InteractableHandler handler) {
    this.handler = handler;
  }

  // Widget visibility --------------------------------------------------------

  /** Sets the visibility of all widgets. */
  public void toggleWidgetsVisibility(boolean visible) {
    synchronized (this.widgets) {
      this.widgets.forEach(r -> r.setVisibility(visible));
    }
  }

  /** Sets the visibility of widgets with a specific tag. */
  public void toggleTaggedWidgetsVisibility(String tag, boolean visible) {
    synchronized (this.widgets) {
      this.widgets.stream()
          .filter(r -> r.getTags().contains(tag))
          .forEach(r -> r.setVisibility(visible));
    }
  }

  // Private ------------------------------------------------------------------

  /**
   * @see GPanel#GPanel(InteractableHandler, int, int, boolean, String, Color)
   */
  private JFrame getDefaultJFrame(boolean resizable, String appTitle) {
    JFrame frame = new JFrame(appTitle);
    frame.setBackground(java.awt.Color.BLACK);
    frame.setResizable(resizable);
    // frame.setUndecorated(true); Transparent window
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(this);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    updateRegions();
    return frame;
  }

  /**
   * @see GPanel#GPanel(InteractableHandler, int, int, boolean, String, Color)
   */
  private void setupListeners() {
    addMouseListener(this);
    addMouseMotionListener(this);
    addKeyListener(this);
    addMouseWheelListener(this);
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            updateRegions();
          }
        });
    requestFocusInWindow();
  }

  private void updateRegions() {
    int width = getWidth();
    int height = getHeight();
    int minSize = Math.min(width, height) / 2;

    regions.put(
        PanelRegion.FULLSCREEN,
        computeRegion(
            (Math.max(width, height) - width) / -2,
            (Math.max(width, height) - height) / -2,
            Math.max(width, height),
            Math.max(width, height)));
    regions.put(PanelRegion.BACKGROUND, computeRegion(0, 0, width, height));
    regions.put(
        PanelRegion.CENTER,
        computeRegion(
            (width - minSize * 2) / 2, (height - minSize * 2) / 2, minSize * 2, minSize * 2));
    regions.put(PanelRegion.TOP_LEFT, computeRegion(0, 0, minSize, minSize));
    regions.put(PanelRegion.TOP_RIGHT, computeRegion(width - minSize, 0, minSize, minSize));
    regions.put(PanelRegion.BOTTOM_LEFT, computeRegion(0, height - minSize, minSize, minSize));
    regions.put(
        PanelRegion.BOTTOM_RIGHT,
        computeRegion(width - minSize, height - minSize, minSize, minSize));

    double pxPerVh = minSize / 100.0;
    double centerPxPerVh = (minSize * 2) / 100.0;
    double fullscreenPxPerVh = Math.max(width, height) / 100;

    widgets.forEach(
        c -> {
          if (c.getRegion() == PanelRegion.CENTER) {
            c.setPxPerVh(centerPxPerVh);
          } else if (c.getRegion() == PanelRegion.FULLSCREEN
              || c.getRegion() == PanelRegion.BACKGROUND) {
            c.setPxPerVh(fullscreenPxPerVh);
          } else {
            c.setPxPerVh(pxPerVh);
          }
        });

    widgets.forEach(w -> w.setScreenSize(new int[] {width, height}));
  }

  private Rectangle computeRegion(int x, int y, int width, int height) {
    return new Rectangle(x, y, width, height);
  }

  private void renderRegion(Graphics2D g2, PanelRegion region, Rectangle bounds) {
    g2.setClip(bounds);
    g2.translate(bounds.x, bounds.y);

    getWidgetsForRegion(region).stream()
        .filter(Renderable::isVisible)
        .forEach(
            c -> {
              VhGraphics vhg = new VhGraphics(g2, c.getPxPerVh());
              c.render(vhg, this.appFrame.getFocusCycleRootAncestor());
            });

    g2.translate(-bounds.x, -bounds.y);
    g2.setClip(null);
  }

  private List<Renderable> getWidgetsForRegion(PanelRegion region) {
    return this.widgets.stream().filter(c -> c.getRegion() == region).collect(Collectors.toList());
  }

  /**
   * @see GPanel#getInteractables
   * @see GPanel#getTypables
   * @see GPanel#getScrollables
   */
  private <T> ArrayList<Renderable> getWidgetsByInterface(Class<T> targetInterface) {
    synchronized (this.widgets) {
      ArrayList<Renderable> list = new ArrayList<>();
      for (Renderable r : this.widgets) {
        if (targetInterface.isInstance(r)) {
          list.add(r);
        }
      }
      return list;
    }
  }

  /**
   * @see GPanel#add(Renderable)
   */
  private int binarySearchInsertZIndex(int zIndex) {
    int low = 0;
    int high = this.widgets.size() - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int midVal = this.widgets.get(mid).getZIndex();
      if (midVal < zIndex) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return low;
  }

  private Map<PanelRegion, MouseEvent> getRelativeMouseEvents(MouseEvent e) {
    Map<PanelRegion, MouseEvent> relative = new HashMap<PanelRegion, MouseEvent>();
    for (Entry<PanelRegion, Rectangle> entry : regions.entrySet()) {
      PanelRegion region = entry.getKey();
      Rectangle bounds = entry.getValue();
      relative.put(
          region,
          new MouseEvent(
              e.getComponent(),
              e.getID(),
              e.getWhen(),
              e.getModifiersEx(),
              e.getX() - bounds.x,
              e.getY() - bounds.y,
              e.getClickCount(),
              e.isPopupTrigger(),
              e.getButton()));
    }
    return relative;
  }

  private Entry<Interactable, MouseEvent> getTopmostInteractable(MouseEvent e) {
    Map<PanelRegion, MouseEvent> relative = getRelativeMouseEvents(e);

    for (int i = widgets.size() - 1; i >= 0; i--) {
      Renderable widget = widgets.get(i);
      if (widget.isVisible() && widget instanceof Interactable) {
        Interactable interactable = (Interactable) widget;
        MouseEvent relativeEvent = relative.get(widget.getRegion());
        if (relativeEvent != null && interactable.wasInteracted(relativeEvent)) {
          return new AbstractMap.SimpleEntry<>(interactable, relativeEvent);
        }
      }
    }
    return null;
  }

  // Render engine ------------------------------------------------------------

  /**
   * The RenderingEngine class is responsible for managing the rendering loop of the GPanel. It runs
   * in its own thread and continuously repaints the GPanel at max framerate.
   */
  private class RenderingEngine implements Runnable {
    private final AtomicInteger currentFPS = new AtomicInteger(0);
    private boolean running = false;
    private long lastFpsTime = 0;
    private int targetFPS = 0;

    public void start() {
      running = true;
      Thread renderThread =
          new Thread(
              this,
              GPanel.class.getSimpleName()
                  + ": "
                  + RenderingEngine.class.getSimpleName()
                  + ": Render Thread");
      renderThread.start();
    }

    public void stop() {
      running = false;
    }

    public void setFpsCap(int fps) {
      targetFPS = fps;
    }

    @Override
    public void run() {
      int frameCount = 0;
      lastFpsTime = System.nanoTime();

      while (running) {
        long startTime = System.nanoTime();
        render();
        frameCount++;

        long currentTime = System.nanoTime();
        if (currentTime - lastFpsTime >= 1_000_000_000) {
          currentFPS.set(frameCount);
          frameCount = 0;
          lastFpsTime = currentTime;
        }

        if (targetFPS > 0) {
          long frameDuration = 1_000_000_000 / targetFPS;
          long elapsedTime = System.nanoTime() - startTime;
          long sleepTime = frameDuration - elapsedTime;
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
    }

    private void render() {
      SwingUtilities.invokeLater(() -> GPanel.this.repaint());
    }

    public AtomicInteger getFps() {
      return currentFPS;
    }
  }

  // Renderable abs class -----------------------------------------------------

  public enum PanelRegion {
    FULLSCREEN,
    BACKGROUND,
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
  }

  /** Abstract base class for UI elements. */
  public abstract static class Renderable {
    private final AtomicBoolean visibility = new AtomicBoolean(true);
    private final List<String> tags = new ArrayList<>();
    private PanelRegion region;
    private int[] screenSize;
    private double pxPerVh;
    private int zIndex;

    public Renderable(int zIndex, PanelRegion region, String... initialTags) {
      this.zIndex = zIndex;
      this.region = region;
      if (initialTags != null) {
        tags.addAll(Arrays.asList(initialTags));
      }
    }

    public abstract void render(VhGraphics g, Container focusCycleRootAncestor);

    public int getZIndex() {
      return this.zIndex;
    }

    public void setZIndex(int zIndex) {
      this.zIndex = zIndex;
    }

    private void setScreenSize(int[] size) {
      assert size.length == 2;
      this.screenSize = size;
    }

    protected void drawBackground(Graphics2D g, Image image, Container focusCycleRootAncestor) {
      if (image == null) return;

      double scaleX = (double) screenSize[0] / image.getWidth(null);
      double scaleY = (double) screenSize[1] / image.getHeight(null);
      double scale = Math.max(scaleX, scaleY);

      int posX = (int) ((screenSize[0] - image.getWidth(null) * scale) / 2);
      int posY = (int) ((screenSize[1] - image.getHeight(null) * scale) / 2);

      AffineTransform originalTransform = g.getTransform();
      g.translate(posX, posY);
      g.scale(scale, scale);
      g.drawImage(image, 0, 0, focusCycleRootAncestor);
      g.setTransform(originalTransform);
    }

    protected void drawBackground(Graphics2D g, java.awt.Color c) {
      g.setColor(c);
      g.fillRect(0, 0, screenSize[0], screenSize[1]);
    }

    private double getPxPerVh() {
      return this.pxPerVh;
    }

    public boolean isVisible() {
      return this.visibility.get();
    }

    public void setVisibility(boolean visibility) {
      this.visibility.set(visibility);
    }

    public List<String> getTags() {
      return this.tags;
    }

    public void addTag(String tag) {
      this.tags.add(tag);
    }

    public void removeTag(String tag) {
      if (tags.contains(tag)) {
        this.tags.remove(tag);
      }
    }

    public PanelRegion getRegion() {
      return this.region;
    }

    public void setPxPerVh(double px) {
      this.pxPerVh = px;
    }

    protected int px(double vh) {
      return (int) (vh * pxPerVh);
    }
  }

  // Widget input interfaces --------------------------------------------------

  /** Interface for interactable UI elements. */
  public interface Interactable {
    void interact(MouseEvent e);

    boolean wasInteracted(MouseEvent e);
  }

  /** Interface for typable UI elements. */
  public interface Typable {
    void type(KeyEvent k);
  }

  /** Interface for scrollable UI elements. */
  public interface Scrollable {
    void scroll(MouseWheelEvent e);
  }

  // User input forwaring -----------------------------------------------------

  /**
   * Interface for handling user input. Extends java.awt.event listeners:
   *
   * <ul>
   *   <li>{@code MouseListener}
   *   <li>{@code MouseMotionListener}
   *   <li>{@code MouseWheelListener}
   *   <li>{@code KeyListener}
   * </ul>
   */
  public interface InteractableHandler
      extends MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    // No need to manually define methods, as they are inherited
  }

  /**
   *
   *
   * <h2>AbstractInteractableHandler</h2>
   *
   * A base class for handling user input events in a GPanel. This class provides empty method
   * definitions for all event listener methods, allowing you to override only the ones you need.
   *
   * <h3>Usage Example:</h3>
   *
   * <pre>
   * panel.setInteractableHandler(new AbstractInteractableHandler() {
   *     {@code @Override}
   *     public void keyPressed(KeyEvent e) {
   *         System.out.println("Key pressed: " + e.getKeyCode());
   *     }
   *
   *     {@code @Override}
   *     public void mouseClicked(MouseEvent e) {
   *         System.out.println("Mouse clicked at: " + e.getX() + ", " + e.getY());
   *     }
   * });
   * </pre>
   *
   * <h3>Features:</h3>
   *
   * <ul>
   *   <li>Provides default empty implementations for all event listener methods.
   *   <li>Allows selective overriding of methods for specific event handling.
   *   <li>Integrates seamlessly with GPanel's event forwarding mechanism.
   * </ul>
   */
  public abstract static class AbstractInteractableHandler implements GPanel.InteractableHandler {
    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
  }

  private <T> void forwardEvent(BiConsumer<InteractableHandler, T> method, T event) {
    if (this.handler != null) {
      method.accept(this.handler, event);
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseDragged, e);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseMoved, e);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseClicked, e);
  }

  @Override
  public void mousePressed(MouseEvent e) {
    forwardEvent(InteractableHandler::mousePressed, e);
    Entry<Interactable, MouseEvent> topmostInteractable = getTopmostInteractable(e);
    if (topmostInteractable != null) {
      topmostInteractable.getKey().interact(topmostInteractable.getValue());
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseReleased, e);
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseEntered, e);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    forwardEvent(InteractableHandler::mouseExited, e);
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    forwardEvent(InteractableHandler::mouseWheelMoved, e);
    this.widgets.stream()
        .filter(w -> w instanceof Scrollable)
        .forEach(w -> ((Scrollable) w).scroll(e));
  }

  @Override
  public void keyTyped(KeyEvent e) {
    forwardEvent(InteractableHandler::keyTyped, e);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    forwardEvent(InteractableHandler::keyPressed, e);
  }

  @Override
  public void keyReleased(KeyEvent e) {
    forwardEvent(InteractableHandler::keyReleased, e);
    this.widgets.stream().filter(w -> w instanceof Typable).forEach(w -> ((Typable) w).type(e));
  }
}
