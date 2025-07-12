/*
 * Author: Matěj Šťastný aka my-daarlin
 * Date created: 11/27/2024
 * Github link: https://github.com/matysta/flaggi
*/

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.BiFunction;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Interactable;
import flaggi.shared.common.GPanel.PanelRegion;
import flaggi.shared.common.GPanel.Renderable;
import flaggi.shared.common.GPanel.Typable;
import flaggi.shared.common.Logger;
import flaggi.shared.common.Logger.LogLevel;
import flaggi.shared.util.FontUtil;
import flaggi.shared.util.ImageUtil;

/**
 * The main menu screen widget.
 */
public class MenuScreen extends Renderable implements Interactable, Typable {

	private String usernameInput, ipInput, errorMessage;
	private boolean isNameFieldFocused, isIpFieldFocused, buttonActive;
	private BiFunction<String, String, String> buttonPressAction;
	private Image logo, textField, button;
	private Font font;

	private static final int LOGO_Y_POSITION = 10;
	private static final int FONT_SIZE = 3;
	private static final double ERROR_Y_POSITION = 72;

	// Constructor --------------------------------------------------------------

	public MenuScreen(String initName, String initIp, BiFunction<String, String, String> buttonPressAction) {
		super(ZIndex.MENU_SCREEN, PanelRegion.CENTER, UiTags.MAIN_MENU);
		this.usernameInput = initName != null ? initName : "";
		this.ipInput = initIp != null ? initIp : "";
		this.buttonPressAction = buttonPressAction;
		loadResources();
		clearErrorMessage();
	}

	// Rendering ----------------------------------------------------------------

	@Override
	public void render(Graphics2D g, Container focusCycleRootAncestor) {
		renderLogo(g, focusCycleRootAncestor);
		renderErrorMessage(g);
		renderTextFields(g, focusCycleRootAncestor);
		renderStartButton(g, focusCycleRootAncestor);
	}

	private void renderLogo(Graphics2D g, Container fcra) {
		int width = px(80);
		Image scaledLogo = ImageUtil.scaleToWidth(this.logo, width, false);
		int x = (px(100) - scaledLogo.getWidth(null)) / 2;
		g.drawImage(scaledLogo, x, px(LOGO_Y_POSITION), fcra);
	}

	private void renderErrorMessage(Graphics2D g) {
		g.setFont(this.font.deriveFont(Font.PLAIN, px(FONT_SIZE)));
		g.setColor(this.errorMessage.contains("Connecting") ? Color.GREEN : Color.RED);
		int[] errorPos = FontUtil.getCenteredPosition(px(100), 0, g.getFontMetrics(), this.errorMessage);
		g.drawString(this.errorMessage, errorPos[0], px(ERROR_Y_POSITION));
	}

	private void renderTextFields(Graphics2D g, Container fcra) {
		g.setFont(this.font.deriveFont(Font.PLAIN, px(FONT_SIZE)));
		Rectangle nameField = getNameFieldBounds();
		Rectangle ipField = getIpFieldBounds();

		Image scaledTextField = ImageUtil.scaleImage(this.textField, nameField.width, nameField.height, false);
		g.drawImage(scaledTextField, nameField.x, nameField.y, fcra);
		g.drawImage(scaledTextField, ipField.x, ipField.y, fcra);

		renderTextField(g, "Name: " + usernameInput, isNameFieldFocused, nameField.x + px(3), nameField.y + px(6));
		renderTextField(g, "IP: " + ipInput, isIpFieldFocused, ipField.x + px(3), ipField.y + px(6));
	}

	private void renderTextField(Graphics2D g, String text, boolean isFocused, int x, int y) {
		g.setColor(isFocused ? Color.BLUE : Color.WHITE);
		g.drawString(text, x, y);
	}

	private void renderStartButton(Graphics2D g, Container fcra) {
		g.setFont(this.font.deriveFont(Font.PLAIN, px(FONT_SIZE)));
		Rectangle startButton = getStartButtonBounds();

		Image scaledButton = ImageUtil.scaleImage(this.button, startButton.width, startButton.height, false);
		g.drawImage(scaledButton, startButton.x, startButton.y, fcra);
		g.setColor(Color.WHITE);
		int[] startButtonTextPos = FontUtil.getCenteredPosition(startButton.width, startButton.height, g.getFontMetrics(), "START");
		if (!buttonActive) {
			g.drawString("START", startButton.x + startButtonTextPos[0], startButton.y + startButtonTextPos[1]);
		}
	}

	// Interaction --------------------------------------------------------------

	@Override
	public void interact(MouseEvent e) {
		String interactedElement = getInteractedElement(e);
		if ("start_button".equals(interactedElement) && !buttonActive) {
			buttonActive = true;
			this.errorMessage = this.buttonPressAction.apply(this.usernameInput, this.ipInput);
		} else if ("name_field".equals(interactedElement)) {
			isIpFieldFocused = false;
			isNameFieldFocused = true;
		} else if ("ip_field".equals(interactedElement)) {
			isIpFieldFocused = true;
			isNameFieldFocused = false;
		}
	}

	private String getInteractedElement(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		if (getStartButtonBounds().contains(x, y))
			return "start_button";
		if (getNameFieldBounds().contains(x, y))
			return "name_field";
		if (getIpFieldBounds().contains(x, y))
			return "ip_field";

		return null;
	}

	@Override
	public boolean wasInteracted(MouseEvent e) {
		return getInteractedElement(e) != null;
	}

	@Override
	public void type(KeyEvent e) {
		if (!this.isVisible())
			return;

		if (isNameFieldFocused)
			handleTyping(e, true);
		else if (isIpFieldFocused)
			handleTyping(e, false);
	}

	// Public -------------------------------------------------------------------

	/**
	 * Resets the menu screen to its initial state. This method clears the error,
	 * and resets the button state.
	 */
	public void reset() {
		this.errorMessage = "";
		this.buttonActive = false;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage != null ? errorMessage : "";
	}

	public void clearErrorMessage() {
		this.errorMessage = "";
	}

	public String getUsernameFieldContent() {
		return usernameInput == null ? "" : usernameInput;
	}

	public String getIpFieldContent() {
		return ipInput == null ? "" : ipInput;
	}

	// Private ---------------------------------------------------------------

	private void handleTyping(KeyEvent e, boolean isNameField) {
		char c = e.getKeyChar();
		StringBuilder input = new StringBuilder(isNameField ? usernameInput : ipInput);

		if ((isNameField && (Character.isLetterOrDigit(c) || Character.isWhitespace(c))) || (!isNameField && (Character.isLetterOrDigit(c) || c == '.' || c == ':'))) {
			input.append(c);
		} else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && input.length() > 0) {
			input.deleteCharAt(input.length() - 1);
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			this.errorMessage = "Cannot use spaces!";
			return;
		}

		if (isNameField) {
			usernameInput = input.toString();
		} else {
			ipInput = input.toString();
		}
	}

	// Resource Loading ------------------------------------------------------

	private void loadResources() {
		loadImages();
		loadFont();
	}

	private void loadFont() {
		try {
			this.font = FontUtil.createFontFromResource("fonts/PixelifySans-VariableFont_wght.ttf").deriveFont(Font.PLAIN, 25);
		} catch (IOException | FontFormatException e) {
			Logger.log(LogLevel.WARN, "MenuScreen: Font cannot be loaded.", e);
			this.font = new Font("Arial", Font.PLAIN, 25);
		}
	}

	private void loadImages() {
		try {
			this.logo = ImageUtil.getImageFromResource("ui/logo.png");
			this.button = ImageUtil.getImageFromResource("ui/button.png");
			this.textField = ImageUtil.getImageFromResource("ui/text_field.png");
		} catch (IOException e) {
			Logger.log(LogLevel.WARN, "Couldn't load " + getClass().getSimpleName() + " textures.", e);
		}
	}

	// Dynamic Bounds Getters ------------------------------------------------

	private Rectangle getStartButtonBounds() {
		return new Rectangle(px(40), px(75), px(20), px(10));
	}

	private Rectangle getNameFieldBounds() {
		return new Rectangle(px(20), px(45), px(60), px(10));
	}

	private Rectangle getIpFieldBounds() {
		return new Rectangle(px(20), px(57), px(60), px(10));
	}

}
