package illa4257.i4test;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Framework.base.utils.CSSParser;
import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.media.Image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class i4Test {
    public static final i4Logger L = new i4Logger("i4Test");

    public static Framework framework;

    public static void init(final Framework framework) {
        L.inheritGlobalIO();
        i4Test.framework = framework;
        framework.addThemeListener(i4Test::onThemeChange);
        onThemeChange(framework.getTheme(), framework.getBaseTheme());

        final FrameworkWindow fw = framework.newWindow(null);
        final Window w = fw.getWindow();

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

        w.setSize(720, 480);
        w.center();
        w.setVisible(true);
    }

    public static void onThemeChange(final String theme, final BaseTheme baseTheme) {
        framework.stylesheet.clear();
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(framework.openResource("assets:///illa4257/i4Framework/" + baseTheme.name().toLowerCase() + ".css")))) {
            CSSParser.parse(framework.stylesheet, r);
        } catch (final IOException ex) {
            L.log(ex);
        }
        framework.fireAllWindows(new StyleUpdateEvent());
    }
}