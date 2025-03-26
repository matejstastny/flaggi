/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 1/9/2025
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Interactable;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.GPanel.Scrollable;

/**
 * Lobby screen where other players can be invited into the game.
 */
public class LobbyInviteScreen extends Renderable implements Scrollable, Interactable {

    private static final int SCROLL_SPEED = 15;

    private List<ClientItem> clientItems;
    private LobbyHandler handler;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static final int ITEM_HEIGHT = 60;
    private static final int PADDING = 15;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 35;

    // Constructor --------------------------------------------------------------

    public LobbyInviteScreen(LobbyHandler handler) {
        super(ZIndex.MENU_SCREEN, UiTags.LOBBY);
        this.clientItems = new ArrayList<>();
        this.handler = handler;
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
        // Background
        g.setColor(new Color(30, 30, 30)); // Dark gray background
        g.fillRect(0, 0, size[0], size[1]);

        // Header
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, size[0], 70);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Online players", 20, 45);

        updateMaxScroll(size);
        int yOffset = scrollOffset + 80;

        // Render client items
        for (ClientItem item : clientItems) {
            renderClientItem(g, item, yOffset, size);
            yOffset += ITEM_HEIGHT + PADDING;
            if (yOffset > size[1])
                break;
        }
    }

    private void renderClientItem(Graphics2D g, ClientItem item, int yOffset, int[] size) {
        // Background for client item
        g.setColor(new Color(45, 45, 60)); // Dark blue-gray
        g.fillRoundRect(10, yOffset, size[0] - 20, ITEM_HEIGHT, 15, 15);

        // Client name
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(item.name, 20, yOffset + ITEM_HEIGHT / 2 + 5);

        // Button
        g.setColor(new Color(70, 140, 70)); // Green
        g.fillRoundRect(item.buttonBounds.x, yOffset + 10, BUTTON_WIDTH, BUTTON_HEIGHT, 10, 10);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Join", item.buttonBounds.x + 22, yOffset + 32);

        // Update button bounds
        item.buttonBounds.y = yOffset + 10;
    }

    // Interaction --------------------------------------------------------------

    @Override
    public boolean interact(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();

        for (ClientItem item : clientItems) {
            if (item.buttonBounds.contains(mouseX, mouseY)) {
                handler.invitePlayer(item.name, item.playerID);
                return true;
            }
        }

        return false;
    }

    @Override
    public void scroll(MouseWheelEvent e) {
        int scrollAmount = e.getWheelRotation() * SCROLL_SPEED;
        scrollOffset -= scrollAmount;

        if (scrollOffset > 0) {
            scrollOffset = 0;
        } else if (scrollOffset < -maxScroll) {
            scrollOffset = -maxScroll;
        }
    }

    // Modifiers ----------------------------------------------------------------

    public void setClients(Map<Integer, String> clients) {
        clientItems.clear();
        int yOffset = PADDING;

        for (Map.Entry<Integer, String> entry : clients.entrySet()) {
            int clientId = entry.getKey();
            String name = entry.getValue();
            Rectangle buttonBounds = new Rectangle(700, yOffset, BUTTON_WIDTH, BUTTON_HEIGHT);
            clientItems.add(new ClientItem(name, clientId, buttonBounds));
            yOffset += ITEM_HEIGHT + PADDING;
        }

        int[] defaultSize = { 800, 600 };
        updateMaxScroll(defaultSize);

        if (scrollOffset < -maxScroll) {
            scrollOffset = -maxScroll;
        }
    }

    // Private ------------------------------------------------------------------

    private void updateMaxScroll(int[] size) {
        int totalHeight = clientItems.size() * (ITEM_HEIGHT + PADDING) + 80;
        maxScroll = Math.max(0, totalHeight - size[1]);
    }

    // Nested -------------------------------------------------------------------

    public interface LobbyHandler {
        void invitePlayer(String playerName, int playerID);
    }

    private static class ClientItem {
        public String name;
        public int playerID;
        public Rectangle buttonBounds;

        public ClientItem(String name, int playerID, Rectangle buttonBounds) {
            this.name = name;
            this.playerID = playerID;
            this.buttonBounds = buttonBounds;
        }
    }

}
