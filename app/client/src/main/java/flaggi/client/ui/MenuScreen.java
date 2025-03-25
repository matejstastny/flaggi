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
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;

import flaggi.client.constants.UiTags;
import flaggi.client.constants.ZIndex;
import flaggi.shared.common.GPanel.Interactable;
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
    private Rectangle nameFieldBounds, ipFieldBounds, startButtonBounds;
    private Image logo, background, textField, button;
    private MenuHandeler handeler;
    private Font font;

    // Constructor --------------------------------------------------------------

    /**
     * @param initName - initial value of the username field, this value can be
     *                 deleted by the user
     * @param initIp   - initial value of the ip field, this value can be deleted by
     *                 the user
     */
    public MenuScreen(String initName, String initIp, MenuHandeler handeler) {
        super(ZIndex.MENU_SCREEN, UiTags.MENU_ELEMENTS);

        this.usernameInput = initName != null ? initName : "";
        this.ipInput = initIp != null ? initIp : "";
        this.handeler = handeler;

        loadFont();
        loadImages();
        initializeBounds();
        clearErrorMessage();
    }

    // Rendering ----------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int[] size, int[] origin, Container focusCycleRootAncestor) {
        calculateElementPositions(size);
        renderBackground(g, size, focusCycleRootAncestor);
        renderLogo(g, size, focusCycleRootAncestor);
        renderErrorMessage(g, size);
        renderTextFields(g, focusCycleRootAncestor);
        renderStartButton(g, focusCycleRootAncestor);
    }

    private void calculateElementPositions(int[] size) {
        int windowWidth = size[0];
        int windowHeight = size[1];

        int fieldWidth = nameFieldBounds.width;
        int fieldHeight = nameFieldBounds.height;
        int buttonWidth = startButtonBounds.width;
        int buttonHeight = startButtonBounds.height;

        int centerX = windowWidth / 2;
        int centerY = windowHeight / 2;

        this.nameFieldBounds.setBounds(centerX - fieldWidth / 2, centerY - 80, fieldWidth, fieldHeight);
        this.ipFieldBounds.setBounds(centerX - fieldWidth / 2, centerY, fieldWidth, fieldHeight);
        this.startButtonBounds.setBounds(centerX - buttonWidth / 2, centerY + 130, buttonWidth, buttonHeight);
    }

    private void renderBackground(Graphics2D g, int[] size, Container fcra) {
        this.background = ImageUtil.scaleToFill(this.background, size[0], size[1], false);
        int x = (size[0] - this.background.getWidth(null)) / 2;
        int y = (size[1] - this.background.getHeight(null)) / 2;
        g.setColor(Color.decode("#C3EEFA"));
        g.fillRect(x, y, this.background.getWidth(null), this.background.getHeight(null));
        g.drawImage(this.background, x, y, fcra);
    }

    private void renderLogo(Graphics2D g, int[] size, Container fcra) {
        int centerX = size[0] / 2;
        int logoWidth = (int) (size[0] * 0.4);
        Image logo = ImageUtil.scaleToWidth(this.logo, logoWidth, false);
        int logoX = centerX - logo.getWidth(null) / 2;
        int logoY = (int) (size[1] * 0.2) - logo.getHeight(null) / 2;
        g.drawImage(logo, logoX, logoY, fcra);
    }

    private void renderErrorMessage(Graphics2D g, int[] size) {
        g.setFont(this.font);
        synchronized (this.errorMessage) {
            g.setColor(this.errorMessage.equals("Connecting...") ? Color.GREEN : Color.RED);
            int[] errorPos = FontUtil.calculateCenteredPosition(size[0], size[1], g.getFontMetrics(), this.errorMessage);
            g.drawString(this.errorMessage, errorPos[0], (int) (size[1] * 0.55));
        }
    }

    private void renderTextFields(Graphics2D g, Container focusCycleRootAncestor) {
        g.drawImage(this.textField, nameFieldBounds.x, nameFieldBounds.y, focusCycleRootAncestor);
        g.drawImage(this.textField, ipFieldBounds.x, ipFieldBounds.y, focusCycleRootAncestor);

        int textFieldPadding = (int) (this.textField.getWidth(null) * 0.05);
        int[] textFieldTextPos = FontUtil.calculateCenteredPosition(this.textField.getWidth(null), this.textField.getHeight(null), g.getFontMetrics(), "Dummy");

        g.setColor(isNameFieldFocused ? Color.BLUE : Color.WHITE);
        g.drawString("Name: " + usernameInput, nameFieldBounds.x + textFieldPadding, nameFieldBounds.y + textFieldTextPos[1]);

        g.setColor(isIpFieldFocused ? Color.BLUE : Color.WHITE);
        g.drawString("IP: " + ipInput, ipFieldBounds.x + textFieldPadding, ipFieldBounds.y + textFieldTextPos[1]);
    }

    private void renderStartButton(Graphics2D g, Container focusCycleRootAncestor) {
        g.drawImage(this.button, startButtonBounds.x, startButtonBounds.y, focusCycleRootAncestor);
        g.setColor(Color.WHITE);
        int[] startButtonTextPos = FontUtil.calculateCenteredPosition(startButtonBounds.width, startButtonBounds.height, g.getFontMetrics(), "START");
        g.drawString("START", startButtonBounds.x + startButtonTextPos[0], startButtonBounds.y + startButtonTextPos[1]);
    }

    // Interaction --------------------------------------------------------------

    @Override
    public boolean interact(MouseEvent e) {
        if (!this.isVisible()) {
            return false;
        }

        boolean interacted = false;

        if (nameFieldBounds.contains(e.getPoint())) {
            setFocus(true, false);
            interacted = true;
        } else if (ipFieldBounds.contains(e.getPoint())) {
            setFocus(false, true);
            interacted = true;
        } else if (startButtonBounds.contains(e.getPoint())) {
            clearErrorMessage();
            setErrorMessage(handeler.joinServer(this.usernameInput, this.ipInput));
            interacted = true;
        } else {
            setFocus(false, false);
        }

        return interacted;
    }

    @Override
    public void type(KeyEvent e) {
        if (!this.isVisible()) {
            return;
        }

        if (isNameFieldFocused) {
            handleTyping(e, true);
        } else if (isIpFieldFocused) {
            handleTyping(e, false);
        }
    }

    // Modifiers --------------------------------------------------------------

    public void setErrorMessage(String errorMessage) {
        synchronized (this.errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public void clearErrorMessage() {
        if (this.errorMessage == null) {
            this.errorMessage = "";
            return;
        }
        synchronized (this.errorMessage) {
            this.errorMessage = "";
        }
    }

    // Accesors --------------------------------------------------------------

    public String getUsernameFieldContent() {
        return usernameInput == null ? "" : usernameInput;
    }

    public String getIpFieldConctent() {
        return ipInput == null ? "" : ipInput;
    }

    // Private ---------------------------------------------------------------

    private void handleTyping(KeyEvent e, boolean isNameField) {
        char c = e.getKeyChar();
        String input = isNameField ? usernameInput : ipInput;

        if (isNameField && (Character.isLetterOrDigit(c) || Character.isWhitespace(c))) {
            input += c;
        } else if (!isNameField && (Character.isDigit(c) || c == '.')) {
            input += c;
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && input.length() > 0) {
            input = input.substring(0, input.length() - 1);
        }

        if (isNameField) {
            usernameInput = input;
        } else {
            ipInput = input;
        }
    }

    private void setFocus(boolean nameFieldFocused, boolean ipFieldFocused) {
        this.isNameFieldFocused = nameFieldFocused;
        this.isIpFieldFocused = ipFieldFocused;
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
            this.background = ImageUtil.getImageFromFile("ui/menu_screen.png");
            this.button = ImageUtil.scaleToWidth(ImageUtil.getImageFromFile("ui/button.png"), 130, false);
            this.textField = ImageUtil.scaleToHeight(ImageUtil.getImageFromFile("ui/text_field.png"), 60, false);
        } catch (IOException e) {
            Logger.log(LogLevel.WARN, "Couldn't load MenuScreen textures.", e);
        }
    }

    private void initializeBounds() {
        this.nameFieldBounds = new Rectangle(0, 0, this.textField.getWidth(null), this.textField.getHeight(null));
        this.ipFieldBounds = new Rectangle(nameFieldBounds);
        this.startButtonBounds = new Rectangle(0, 0, this.button.getWidth(null), this.button.getHeight(null));
    }

    // Nested ----------------------------------------------------------------

    public interface MenuHandeler {
        String joinServer(String name, String ip);
    }

}
