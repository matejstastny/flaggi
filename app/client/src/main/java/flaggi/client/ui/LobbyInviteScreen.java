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
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Interactable;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.GPanel.Scrollable;
import flaggi.shared.util.FontUtil;

/**
 * Lobby screen where other players can be invited into the game.
 */
public class LobbyInviteScreen extends Renderable implements Scrollable, Interactable {

	private static final int itemWidth = 98;
	private static final int itemHeight = 8;
	private static final int itemPadding = 2;
	private static final int buttonWidth = 20;
	private static final int buttonHeight = 6;
	private static final int buttonPadding = 1;
	private static final int itemsTopOffset = 15;
	int cornerRadius = 2;

	private BiConsumer<String, Integer> inviteAction;
	private List<ClientItem> clientItems;
	private int scrollOffset = 0;

	// Constructor --------------------------------------------------------------

	public LobbyInviteScreen(BiConsumer<String, Integer> inviteAction) {
		super(ZIndex.MENU_SCREEN, PanelRegion.CENTER, UiTags.LOBBY);
		this.inviteAction = inviteAction;
		this.clientItems = new ArrayList<>();
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, int[] viewportOffset, Container focusCycleRootAncestor) {
		AffineTransform previousTransform = g.getTransform();
		g.translate(0, this.scrollOffset);

		renderHeader(g);
		int offsetY = px(itemsTopOffset);
		for (ClientItem clientItem : clientItems) {
			renderClientItem(g, clientItem, offsetY);
			offsetY += px(itemHeight) + px(itemPadding);
		}

		g.setTransform(previousTransform);

	}

	private void renderHeader(Graphics2D g) {
		g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(3)));
		g.setColor(new Color(50, 50, 50));

		int headerWidth = px(100);
		int headerHeight = px(7);
		int headerCornerRadius = px(3);
		int[] headerBounds = { 0, px(3), headerWidth, headerHeight, headerCornerRadius };

		g.fillRoundRect(headerBounds[0], headerBounds[1], headerBounds[2], headerBounds[3], headerCornerRadius, headerCornerRadius);

		String headerText = "ONLINE PLAYERS";
		int[] textPosition = FontUtil.calculateCenteredPosition(headerWidth, headerHeight, g.getFontMetrics(), headerText);
		g.setColor(Color.WHITE);
		g.drawString(headerText, textPosition[0] + headerBounds[0], textPosition[1] + headerBounds[1]);
	}

	private void renderClientItem(Graphics2D g, ClientItem clientItem, int offsetY) {
		g.setFont(Constants.FONT.deriveFont(Font.PLAIN, px(2)));
		int itemWidth = px(98);
		int itemHeight = px(8);
		int paddingX = px(itemPadding);

		// Draw item background
		g.setColor(new Color(45, 45, 60));
		g.fillRoundRect(paddingX, offsetY, px(100) - 2 * paddingX, itemHeight, px(cornerRadius), px(cornerRadius));

		// Render client name
		g.setColor(Color.WHITE);
		int[] namePosition = FontUtil.calculateCenteredPosition(itemWidth, itemHeight, g.getFontMetrics(), clientItem.name);
		g.drawString(clientItem.name, px(5), offsetY + namePosition[1]);

		// Render "Join" button
		int buttonWidth = px(20);
		int buttonHeight = px(6);
		int buttonPadding = px(1);
		g.setColor(new Color(70, 140, 70));
		RoundRectangle2D joinButton = getJoinButtonShape(itemWidth, offsetY, buttonWidth, buttonHeight, buttonPadding, px(cornerRadius));
		g.fill(joinButton);

		// Center "Join" text inside the button
		int[] buttonTextPosition = FontUtil.calculateCenteredPosition(buttonWidth, buttonHeight, g.getFontMetrics(), "Join");
		g.setColor(Color.WHITE);
		g.drawString("Join", itemWidth - buttonWidth + buttonTextPosition[0] - buttonPadding, offsetY + buttonTextPosition[1] + buttonPadding);
	}

	// Interaction --------------------------------------------------------------

	public boolean wasInteracted(MouseEvent e) {
		return getInteractedClientItem(e) != null;
	}

	@Override
	public void interact(MouseEvent e) {
		ClientItem item = getInteractedClientItem(e);
		if (item != null) {
			inviteAction.accept(item.name, item.playerID);
		}
	}

	@Override
	public synchronized void scroll(MouseWheelEvent e) {
		int scrollAmount = e.getWheelRotation() * 15;
		int tempScrollOffset = -(this.scrollOffset - scrollAmount);
		int maxScroll = getMaxScroll();

		if (tempScrollOffset < 0) {
			scrollOffset = 0;
		} else if (tempScrollOffset > maxScroll) {
			scrollOffset = -maxScroll;
		} else {
			scrollOffset -= scrollAmount;
		}
	}

	// Modifiers ----------------------------------------------------------------

	public void setClients(Map<Integer, String> clients) {
		if (clients == null) {
			return;
		}

		clientItems.clear();

		for (Entry<Integer, String> entry : clients.entrySet()) {
			int clientId = entry.getKey();
			String name = entry.getValue();
			clientItems.add(new ClientItem(name, clientId));
		}

		if (scrollOffset < -getMaxScroll()) {
			scrollOffset = -getMaxScroll();
		}
	}

	// Private ------------------------------------------------------------------

	private int getMaxScroll() {
		int totalHeight = clientItems.size() * (px(itemHeight) + px(itemPadding)) - px(80);
		return Math.max(0, totalHeight);
	}

	private ClientItem getInteractedClientItem(MouseEvent e) {

		int mouseX = e.getX();
		int mouseY = e.getY() - scrollOffset;

		int offsetY = px(itemsTopOffset);
		for (ClientItem item : clientItems) {
			RoundRectangle2D r = getJoinButtonShape(px(itemWidth), offsetY, px(buttonWidth), px(buttonHeight), px(buttonPadding), px(cornerRadius));
			if (r.contains(mouseX, mouseY)) {
				return item;
			}
			offsetY += px(itemHeight) + px(itemPadding);
		}
		return null;
	}

	// Nested -------------------------------------------------------------------

	public interface LobbyHandler {
		void invitePlayer(String playerName, int playerID);
	}

	private static class ClientItem {
		public String name;
		public int playerID;

		public ClientItem(String name, int playerID) {
			this.name = name;
			this.playerID = playerID;
		}
	}

	private RoundRectangle2D getJoinButtonShape(int itemWidth, int offsetY, int buttonWidth, int buttonHeight, int buttonPadding, int cornerRadius) {
		return new RoundRectangle2D.Float(itemWidth - buttonWidth - buttonPadding, offsetY + buttonPadding, buttonWidth, buttonHeight, cornerRadius, cornerRadius);
	}

}
