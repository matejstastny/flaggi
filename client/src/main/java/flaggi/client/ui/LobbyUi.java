package flaggi.client.ui;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.UpdateLoop.Updatable;
import flaggi.shared.ui.GPanel.Interactable;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.GPanel.Scrollable;
import flaggi.shared.ui.VhGraphics;
import flaggi.shared.util.FontUtil;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Lobby screen where other players can be invited into the game. */
public class LobbyUi extends Renderable implements Scrollable, Interactable, Updatable {

  private static final int ITEM_WIDTH = 98;
  private static final int ITEM_HEIGHT = 8;
  private static final int ITEM_PADDING = 2;
  private static final int BUTTON_WIDTH = 20;
  private static final int BUTTON_HEIGHT = 6;
  private static final int BUTTON_PADDING = 1;
  private static final int ITEMS_TOP_OFFSET = 15;
  private static final int SCROLL_STEP = 15;
  private static final int HEADER_HEIGHT = 7;
  private static final int HEADER_OFFSET = 3;
  private static final int HEADER_CORNER_RADIUS = 3;

  private final Consumer<String> inviteAction;
  private final Runnable refreshAction;
  private final List<ClientItem> clientItems = new ArrayList<>();
  private int scrollOffset = 0;
  private final int cornerRadius = 2;
  private long lastUpdateTime = 0;

  // Constructor --------------------------------------------------------------

  public LobbyUi(Consumer<String> inviteAction, Runnable refreshAction) {
    super(ZIndex.MENU_SCREEN, PanelRegion.CENTER, UiTags.LOBBY);
    this.refreshAction = refreshAction;
    this.inviteAction = inviteAction;
  }

  // Rendering ----------------------------------------------------------------

  @Override
  public void render(VhGraphics g, Container focusCycleRootAncestor) {
    AffineTransform previousTransform = g.raw().getTransform();
    g.raw().translate(0, scrollOffset);

    renderHeader(g.raw());
    int offsetY = px(ITEMS_TOP_OFFSET);
    for (ClientItem clientItem : clientItems) {
      renderClientItem(g.raw(), clientItem, offsetY);
      offsetY += px(ITEM_HEIGHT) + px(ITEM_PADDING);
    }

    g.raw().setTransform(previousTransform);
  }

  private void renderHeader(Graphics2D g) {
    g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(3)));
    g.setColor(new Color(50, 50, 50));

    int width = px(100);
    int height = px(HEADER_HEIGHT);
    int radius = px(HEADER_CORNER_RADIUS);

    g.fillRoundRect(0, px(HEADER_OFFSET), width, height, radius, radius);

    String text = "ONLINE PLAYERS";
    int[] pos = FontUtil.getCenteredPosition(width, height, g.getFontMetrics(), text);
    g.setColor(Color.WHITE);
    g.drawString(text, pos[0], px(HEADER_OFFSET) + pos[1]);
  }

  private void renderClientItem(Graphics2D g, ClientItem clientItem, int offsetY) {
    g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(2)));
    int paddingX = px(ITEM_PADDING);
    int width = px(100) - 2 * paddingX;
    int height = px(ITEM_HEIGHT);

    g.setColor(new Color(45, 45, 60));
    g.fillRoundRect(paddingX, offsetY, width, height, px(cornerRadius), px(cornerRadius));

    g.setColor(Color.WHITE);
    int[] namePos =
        FontUtil.getCenteredPosition(width, height, g.getFontMetrics(), clientItem.name);
    g.drawString(clientItem.name, px(5), offsetY + namePos[1]);

    renderJoinButton(g, width, offsetY);
  }

  private void renderJoinButton(Graphics2D g, int itemWidth, int offsetY) {
    int buttonWidth = px(BUTTON_WIDTH);
    int buttonHeight = px(BUTTON_HEIGHT);
    int padding = px(BUTTON_PADDING);
    int radius = px(cornerRadius);

    g.setColor(new Color(70, 140, 70));
    RoundRectangle2D button =
        new RoundRectangle2D.Float(
            itemWidth - buttonWidth - padding,
            offsetY + padding,
            buttonWidth,
            buttonHeight,
            radius,
            radius);
    g.fill(button);

    int[] textPos =
        FontUtil.getCenteredPosition(buttonWidth, buttonHeight, g.getFontMetrics(), "Join");
    g.setColor(Color.WHITE);
    g.drawString(
        "Join", itemWidth - buttonWidth + textPos[0] - padding, offsetY + textPos[1] + padding);
  }

  // Interactions -------------------------------------------------------------

  @Override
  public boolean wasInteracted(MouseEvent e) {
    return getInteractedClientItem(e) != null;
  }

  @Override
  public void interact(MouseEvent e) {
    ClientItem item = getInteractedClientItem(e);
    if (item != null) {
      inviteAction.accept(item.uuid);
    }
  }

  @Override
  public synchronized void scroll(MouseWheelEvent e) {
    int tempScrollOffset =
        Math.max(-getMaxScroll(), Math.min(0, scrollOffset - e.getWheelRotation() * SCROLL_STEP));
    scrollOffset = tempScrollOffset;
  }

  // Public -------------------------------------------------------------------

  public void setClients(Map<String, String> clients) {
    if (clients != null) {
      clientItems.clear();
      clients.forEach((id, name) -> clientItems.add(new ClientItem(name, id)));
      scrollOffset = Math.max(scrollOffset, -getMaxScroll());
    }
  }

  // Private ------------------------------------------------------------------

  private int getMaxScroll() {
    return Math.max(0, clientItems.size() * (px(ITEM_HEIGHT) + px(ITEM_PADDING)) - px(80));
  }

  private ClientItem getInteractedClientItem(MouseEvent e) {
    int mouseX = e.getX();
    int mouseY = e.getY() - scrollOffset;

    int offsetY = px(ITEMS_TOP_OFFSET);
    for (ClientItem item : clientItems) {
      RoundRectangle2D button =
          new RoundRectangle2D.Float(
              px(ITEM_WIDTH) - px(BUTTON_WIDTH) - px(BUTTON_PADDING),
              offsetY + px(BUTTON_PADDING),
              px(BUTTON_WIDTH),
              px(BUTTON_HEIGHT),
              px(cornerRadius),
              px(cornerRadius));
      if (button.contains(mouseX, mouseY)) {
        return item;
      }
      offsetY += px(ITEM_HEIGHT) + px(ITEM_PADDING);
    }
    return null;
  }

  private static class ClientItem {
    private final String name;
    private final String uuid;

    private ClientItem(String name, String uuid) {
      this.name = name;
      this.uuid = uuid;
    }
  }

  @Override
  public void update() {
    if (this.isVisible()) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastUpdateTime > 5000) {
        if (this.refreshAction != null) {
          this.refreshAction.run();
        }
        lastUpdateTime = currentTime;
      }
    }
  }
}
