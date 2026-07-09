package illa4257.i4test;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.*;
import illa4257.i4Framework.base.components.Button;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Label;
import illa4257.i4Framework.base.components.Panel;
import illa4257.i4Framework.base.components.TextField;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.curves.CubicBezierCurve;
import illa4257.i4Framework.base.curves.Curve;
import illa4257.i4Framework.base.curves.SineCurveOut;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.points.ops.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.base.utils.CSSParser;
import illa4257.i4Utils.logger.i4Logger;

import java.io.BufferedReader;
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

        final Component c = new Component();
        c.setStartX(w.safeStartX);
        c.setStartY(w.safeStartY);
        c.setWidth(new NumberPointMultiplier(360, w.dp));
        c.setHeight(new NumberPointMultiplier(360, w.dp));
        c.style.set("background", "blue url('assets:///test.png')");
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
        l.setHeight(24 * 3, DP);
        w.add(l);

        final Panel pc = new Panel();
        final Point pcw = new NumberPointMultiplier(288, w.dp), d8 = new NumberPointMultiplier(8, w.dp), pch = new NumberPointMultiplier(112, w.dp);
        pc.setStartX(new NumberPointMultiplier(new PPointSubtract(w.width, pcw), .5f));
        pc.setEndY(new PPointSubtract(w.safeEndY, d8));
        pc.setWidth(pcw);
        pc.setHeight(pch);
        pc.style.set("border-radius", "16dp");

        final ComboBox<String> cb = new ComboBox<>();
        cb.setX(16, DP);
        cb.setY(16, DP);
        cb.setWidth(256, DP);
        cb.setHeight(32, DP);
        cb.options = Arrays.asList("TEST", "GG", "Hello, world!");
        pc.add(cb);

        final Button b = new Button("TEST");
        b.addEventListener(ActionEvent.class, e -> framework.newPopupMenu(b)
                .add("test", () -> framework.newDialog(w)
                        .setTitle("Test")
                        .setMessage("Hello, world!")
                        .setContent(new TextField())
                        .setPositiveButton("OK", () -> {
                            System.out.println("OK");
                        })
                        .setNegativeButton("Cancel", () -> {
                            System.out.println("Cancel");
                        })
                        .show())
                .add("test 2", () -> {
                    framework.newFileChooser(w).start(() -> {});
                })
                .show());
        b.style.set("left", "16dp");
        b.style.set("right", "16dp");
        b.style.set("top", "64dp");
        pc.add(b);

        w.add(pc);

        final Component t = new Component() {
            final Curve c = new CubicBezierCurve(0.37f, 0, 0.63f, 1f);

            @Override
            public void paint(Context context) {
                super.paint(context);
                final float w = width.calcFloat(), h = height.calcFloat();
                float ox = 0, oy = h;
                final int m = Math.round(w);
                for (int i = 1; i < m; i++) {
                    final float nx = i, ny = h - c.calc(nx / w) * h;
                    context.setStrokeWidth(2);
                    context.setPaint(Color.GREEN);
                    context.drawLine(ox, oy, nx, ny);
                    ox = nx;
                    oy = ny;
                }
            }
        };
        t.style.set("width", "128dp");
        t.style.set("height", "128dp");
        t.style.set("right", "64dp");
        t.style.set("bottom", "64dp");
        //t.style.set("border-radius", "16dp");
        t.style.set("background", "blue");
        w.add(t);

        //w.onTick(() -> {});

        w.setWidth(720, DP);
        w.setHeight(480, DP);
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