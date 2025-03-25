/*
* Author: Matěj Šťastný aka Kirei
* Date created: 11/27/2024
* Github link: https://github.com/kireiiiiiiii/flaggi
*/

package flaggi.client.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

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
    private boolean isNameFieldFocused, isIpFieldFocused;
    private Image logo, textField, button;
    private MenuHandler handler;
    private Font font;

    private static final int LOGO_Y_POSITION = 10;
    private static final int ERROR_Y_POSITION = 55;
    private static final int TEXT_FIELD_Y_POSITION = 45;
    private static final int IP_FIELD_Y_POSITION = 57;
    private static final int START_BUTTON_Y_POSITION = 75;

    public MenuScreen(String initName, String initIp, MenuHandler handler) {
        super(ZIndex.MENU_SCREEN, PanelRegion.CENTER, UiTags.MENU_ELEMENTS);

        this.usernameInput = initName != null ? initName : "";
        this.ipInput = initIp != null ? initIp : "";
        this.handler = handler;

        loadResources();

        clearErrorMessage();
    }

    // -------------------- Rendering --------------------

    @Override
    public void render(Graphics2D g, int[] origin, Container fcra) {
        renderLogo(g, fcra);
        renderErrorMessage(g);
        renderTextFields(g, fcra);
        renderStartButton(g, fcra);
    }

    private void renderLogo(Graphics2D g, Container fcra) {
        int width = px(80);
        Image scaledLogo = ImageUtil.scaleToWidth(this.logo, width, false);
        int x = (px(100) - scaledLogo.getWidth(null)) / 2;
        g.drawImage(scaledLogo, x, px(LOGO_Y_POSITION), fcra);
    }

    private void renderErrorMessage(Graphics2D g) {
        g.setFont(this.font.deriveFont(Font.PLAIN, px(4)));
        g.setColor(Color.RED);
        int[] errorPos = FontUtil.calculateCenteredPosition(px(100), 0, g.getFontMetrics(), this.errorMessage);
        g.drawString(this.errorMessage, errorPos[0], px(ERROR_Y_POSITION));
    }

    private void renderTextFields(Graphics2D g, Container fcra) {
        g.setFont(this.font.deriveFont(Font.PLAIN, px(3)));
        Image scaledTextField = ImageUtil.scaleImage(this.textField, px(60), px(10), false);
        g.drawImage(scaledTextField, px(20), px(TEXT_FIELD_Y_POSITION), fcra);
        g.drawImage(scaledTextField, px(20), px(IP_FIELD_Y_POSITION), fcra);

        renderTextField(g, "Name: " + usernameInput, isNameFieldFocused, px(23), px(TEXT_FIELD_Y_POSITION + 6));
        renderTextField(g, "IP: " + ipInput, isIpFieldFocused, px(23), px(IP_FIELD_Y_POSITION + 6));
    }

    private void renderTextField(Graphics2D g, String text, boolean isFocused, int x, int y) {
        g.setColor(isFocused ? Color.BLUE : Color.WHITE);
        g.drawString(text, x, y);
    }

    private void renderStartButton(Graphics2D g, Container fcra) {
        g.setFont(this.font.deriveFont(Font.PLAIN, px(3)));
        Image scaledButton = ImageUtil.scaleImage(this.button, px(20), px(10), false);
        g.drawImage(scaledButton, px(40), px(START_BUTTON_Y_POSITION), fcra);
        g.setColor(Color.WHITE);
        int[] startButtonTextPos = FontUtil.calculateCenteredPosition(px(20), px(10), g.getFontMetrics(), "START");
        g.drawString("START", px(40) + startButtonTextPos[0], px(START_BUTTON_Y_POSITION) + startButtonTextPos[1]);
    }

    // -------------------- Interaction --------------------

    @Override
    public boolean interact(MouseEvent e) {
        handler.joinServer(usernameInput, ipInput);
        return false;
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

    // Modifiers --------------------------------------------------------------

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public void clearErrorMessage() {
        this.errorMessage = "";
    }

    // Accesors --------------------------------------------------------------

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

        if ((isNameField && (Character.isLetterOrDigit(c) || Character.isWhitespace(c))) || (!isNameField && (Character.isDigit(c) || c == '.'))) {
            input.append(c);
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && input.length() > 0) {
            input.deleteCharAt(input.length() - 1);
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
            this.logo = ImageUtil.getImageFromFile("ui/logo.png");
            this.button = ImageUtil.getImageFromFile("ui/button.png");
            this.textField = ImageUtil.getImageFromFile("ui/text_field.png");
        } catch (IOException e) {
            Logger.log(LogLevel.WARN, "Couldn't load " + getClass().getSimpleName() + " textures.", e);
        }
    }

    // Interface -------------------------------------------------------------

    public interface MenuHandler {
        String joinServer(String name, String ip);
    }

}
