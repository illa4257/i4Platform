package illa4257.i4Utils.media;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Color {
    public static Color repeat3(final int value) { return new Color(value, value, value); }
    public static Color repeat3(final float value) { return new Color(value, value, value); }
    public static Color repeat4(final int value) { return new Color(value, value, value, value); }
    public static Color repeat4(final float value) { return new Color(value, value, value, value); }

    @SuppressWarnings("unused")
    public static final Color
                WHITE = repeat3(1f),
                LIGHT_GRAY = repeat3(.7529412f),
                GRAY = repeat3(.5f),
                DARK_GRAY = repeat3(.2509804f),
                BLACK = repeat3(0f),

                TRANSPARENT = repeat4(0f),

                RED = new Color(1f, 0, 0),
                GREEN = new Color(0, 1f, 0),
                BLUE = new Color(0, 0, 1f),

                YELLOW = new Color(1f, 1f, 0),
                CYAN = new Color(0, 1f, 1f),
                MAGENTA = new Color(1f, 0, 1f),

                PURPLE = new Color(.5f, 0, .5f),

                ORANGE = new Color(1f, .78431374f, 0),
                PINK = new Color(1f, .6862745f, .6862745f),

                NAVY_BLUE = new Color(0, 0, .5f);

    public final float red, green, blue, alpha;

    public Color(final float red, final float green, final float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = 1;
    }

    public Color(final float red, final float green, final float blue, final float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public Color(final int red, final int green, final int blue) {
        this.red = (red & 0xFF) / 255f;
        this.green = (green & 0xFF) / 255f;
        this.blue = (blue & 0xFF) / 255f;
        this.alpha = 1;
    }

    public Color(final int red, final int green, final int blue, final int alpha) {
        this.red = (red & 0xFF) / 255f;
        this.green = (green & 0xFF) / 255f;
        this.blue = (blue & 0xFF) / 255f;
        this.alpha = (alpha & 0xFF) / 255f;
    }

    public Color(final int rgba) { this(rgba >> 24, rgba >> 16, rgba >> 8, rgba); }

    public Color(final java.awt.Color color) {
        this.red = color.getRed() / 255f;
        this.green = color.getGreen() / 255f;
        this.blue = color.getBlue() / 255f;
        this.alpha = color.getAlpha() / 255f;
    }

    public static Color parse(final String s) {
        try {
            final int l = s.length();
            if (s.startsWith("#")) {
                if (l == 4) {
                    final int r = Integer.parseInt(s.substring(1, 2), 16),
                            g = Integer.parseInt(s.substring(2, 3), 16),
                            b = Integer.parseInt(s.substring(3, 4), 16);
                    return new Color(r * 16 + r, g * 16 + g, b * 16 + b);
                }
                if (l == 5) {
                    final int r = Integer.parseInt(s.substring(1, 2), 16),
                            g = Integer.parseInt(s.substring(2, 3), 16),
                            b = Integer.parseInt(s.substring(3, 4), 16),
                            a = Integer.parseInt(s.substring(4, 5), 16);
                    return new Color(r * 16 + r, g * 16 + g, b * 16 + b, a * 16 + a);
                }
                if (l == 7)
                    return fromRGB(Integer.parseInt(s.substring(1), 16));
                if (l == 9)
                    return new Color(Integer.parseInt(s.substring(1), 16));
            } else if (s.startsWith("0x")) {
                if (l == 8)
                    return fromRGB(Integer.parseInt(s.substring(2), 16));
                if (l == 10)
                    return new Color(Integer.parseInt(s.substring(2), 16));
            }
        } catch (final Exception ignored) {}
        try {
            final Field f = Color.class.getDeclaredField(s.toUpperCase());
            if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()))
                return (Color) f.get(null);
        } catch (final Exception ignored) {}
        throw new IllegalArgumentException("Invalid color format: " + s);
    }

    public static Color fromRGB(final int rgb) { return new Color(rgb >> 16, rgb >> 8, rgb); }
    public static Color fromARGB(final int argb) { return new Color(argb >> 16, argb >> 8, argb, argb >> 24); }

    public Color withAlpha(final float newAlpha) { return new Color(red, green, blue, newAlpha); }

    public int redInt() { return Math.round(red * 255); }
    public int greenInt() { return Math.round(green * 255); }
    public int blueInt() { return Math.round(blue * 255); }
    public int alphaInt() { return Math.round(alpha * 255); }

    public int toARGB() {
        return Math.round(alpha * 255) << 24 | Math.round(red * 255) << 16 | Math.round(green * 255) << 8 | Math.round(blue * 255);
    }

    public String toHexRGB() {
        return String.format("#%02X%02X%02X", (int) (red * 255), (int) (green * 255), (int) (blue * 255));
    }

    public String toHexRGBA() {
        return String.format("#%02X%02X%02X%02X", (int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
    }

    public java.awt.Color toAwtColor() {
        return new java.awt.Color(
                Math.round(red * 255),
                Math.round(green * 255),
                Math.round(blue * 255),
                Math.round(alpha * 255)
        );
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Color) {
            final Color c = (Color) o;
            return red == c.red && green == c.green && blue == c.blue && alpha == c.alpha;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Color{red=" + redInt() + ", green=" + greenInt() + ", blue=" + blueInt() + ", alpha=" + alphaInt() + "}";
    }
}