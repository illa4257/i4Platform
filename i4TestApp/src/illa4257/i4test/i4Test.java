package illa4257.i4test;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.*;
import illa4257.i4Framework.base.components.Button;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Label;
import illa4257.i4Framework.base.components.Panel;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Framework.base.utils.CSSParser;
import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.media.Image;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import static illa4257.i4Framework.base.math.Unit.DP;

public class i4Test {
    public static final i4Logger L = new i4Logger("i4Test");

    public static Framework framework;

    public static void init(final Framework framework) {
        L.inheritGlobalIO();
        i4Test.framework = framework;
        framework.addThemeListener(i4Test::onThemeChange);
        onThemeChange(framework.getTheme(), framework.getBaseTheme());
    }

    public static void start() {
        final FrameworkWindow fw = framework.newWindow(null);
        final Window w = fw.getWindow();
        w.setTitle("i4Test");

        final int[] p = new int[128 * 128];
        Arrays.fill(p, 0xFF00FFFF);

        Image img = null;

        try {
            //img = new Image(128, p);
            img = framework.getImage("assets:///test.png");

            Cache.images.put("test-img", img);
        } catch (final Exception ex) {
            L.log(ex);
        }

        System.out.println(img);

        final Component c = new Component();
        c.setStartX(w.safeStartX);
        c.setStartY(w.safeStartY);
        c.setWidth(new NumberPointMultiplier(360, w.densityMultiplier));
        c.setHeight(new NumberPointMultiplier(360, w.densityMultiplier));
        c.styles.put("background-color", new StyleSetting("0x0000FF"));
        c.styles.put("background-image", new StyleSetting("test-img"));
        w.add(c);

        final Label l = new Label("???");
        try {
            l.text = framework.getAppDataDir() + "\n" +
                    framework.getLocalAppDataDir() + "\n" +
                    framework.getAppDir();
        } catch (final Exception ex) {
            l.text = ex.toString();
            L.log(ex);
        }
        l.setStartX(w.safeStartX);
        l.setEndX(w.safeEndX);
        l.setStartY(w.safeStartY);
        l.setHeight(32 * 3, DP);
        w.add(l);

        final Panel pc = new Panel();
        final Point pcw = new NumberPointMultiplier(288, w.densityMultiplier), pch = new NumberPointMultiplier(112, w.densityMultiplier), pcho = new NumberPointMultiplier(120, w.densityMultiplier);
        pc.setStartX(new NumberPointMultiplier(new PPointSubtract(w.width, pcw), .5f));
        pc.setStartY(new PPointSubtract(w.safeEndY, pcho));
        pc.setWidth(pcw);
        pc.setHeight(pch);
        pc.styles.put("border-radius", new StyleSetting("16dp"));

        final ComboBox<String> cb = new ComboBox<>();
        cb.setX(16, DP);
        cb.setY(16, DP);
        cb.setWidth(256, DP);
        cb.setHeight(32, DP);
        cb.options = Arrays.asList("TEST", "GG", "Hello, world!");
        pc.add(cb);

        final Button b = new Button("TEST");
        b.addEventListener(ActionEvent.class, e -> {
            System.out.println("Hello, world!");
        });
        b.setX(16, DP);
        b.setY(64, DP);
        b.setWidth(256, DP);
        b.setHeight(32, DP);
        pc.add(b);

        w.add(pc);

        w.setSize(720, 480);
        w.center();
        w.setVisible(true);
    }

    public static void onThemeChange(final String theme, final BaseTheme baseTheme) {
        framework.stylesheet.clear();
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(framework.openResource("assets:///illa4257/i4Framework/" + baseTheme.name().toLowerCase() + ".css")))) {
            CSSParser.parse(framework.stylesheet, r);
        } catch (final Exception ex) {
            L.e(ex);
        }
        framework.fireAllWindows(StyleUpdateEvent::new);
    }
}