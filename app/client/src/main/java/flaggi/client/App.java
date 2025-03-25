/*
 * Author: Matěj Šťastný aka Kirei
 * Date created: 11/4/2024 (v2 - 2/25/2025)
 * Github link: https://github.com/kireiiiiiiii/flaggi
 */

package flaggi.client;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;

import flaggi.client.constants.Constants;
import flaggi.client.ui.MenuBackground;
import flaggi.client.ui.MenuScreen;
import flaggi.client.ui.MenuScreen.MenuHandler;
import flaggi.shared.common.GPanel;
import flaggi.shared.common.Logger;
import flaggi.shared.common.GPanel.InteractableHandler;
import flaggi.shared.util.ScreenUtil;

public class App implements InteractableHandler, MenuHandler {

    private final GPanel gpanel;

    // Main ---------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }

    public App() {
        this.gpanel = getDefaultGpanel();
        addDefaultWidgets();
        this.gpanel.toggleWidgetsVisibility(true); // TODO DEBUG
        Logger.logMemoryUsage(5);
    }

    // Private ------------------------------------------------------------------

    private GPanel getDefaultGpanel() {
        int[] screenSize = ScreenUtil.getScreenDimensions();
        GPanel gp = new GPanel(screenSize[0], screenSize[1], true, Constants.WINDOW_NAME, this);
        if (Constants.FRAMERATE >= 0) {
            gp.setFpsCap(Constants.FRAMERATE);
        }
        return gp;
    }

    private void addDefaultWidgets() {
        this.gpanel.add( //
                new MenuScreen("nameinit", "ipinit", this), //
                new MenuBackground() //
        );
    }

    // UI Handeling -------------------------------------------------------------

    @Override
    public String joinServer(String name, String ip) {
        System.out.println("JoinButton:" + name + ":" + ip);
        return "passed";
    }

    // Interaction --------------------------------------------------------------

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}
