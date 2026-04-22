package flaggi.client.ui;

import flaggi.client.constants.Constants;
import flaggi.client.constants.UiTags;
import flaggi.proto.ServerMessages.ServerGameObject;
import flaggi.proto.ServerMessages.ServerStateUpdate;
import flaggi.shared.ui.GPanel.PanelRegion;
import flaggi.shared.ui.GPanel.Renderable;
import flaggi.shared.ui.VhGraphics;
import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

public class DebugOverlay extends Renderable {

    private List<String> data = new ArrayList<>();

    public DebugOverlay() {
        super(100000, PanelRegion.TOP_LEFT, UiTags.GAME);
        setVisibility(true);
    }

    public void update(ServerStateUpdate s) {
        data.clear();
        try {
            ServerGameObject g = s.getMe();
            data.add("Username: " + g.getUsername());
            data.add("Game tick: " + s.getTick());
            data.add("X position: " + g.getX());
            data.add("Y position: " + g.getY());
            data.add("Hitpoints: " + g.getHp());
            data.add("Objects: " + s.getOtherCount());
        } catch (Exception e) {
            data.add("N/A");
        }
    }

    @Override
    public void render(VhGraphics g, Container focusCycleRootAncestor) {
        g.setColor(Color.BLACK);
        g.setFont(Constants.FONT, 5);
        int height = g.raw().getFontMetrics().getHeight() + px(0.05);
        List<String> cache = new ArrayList<>(data);
        for (int i = 0; i < cache.size(); i++) {
            g.drawString(cache.get(i), 2, (height / 2) * (i + 1));
        }
    }
}
