package i4Framework.base;

public class Color {
    public static Color repeat3(final float value) { return new Color(value, value, value); }
    public static Color repeat3(final int value) { return new Color(value, value, value); }

    public static final Color
                WHITE = repeat3(1f),
                BLACK = repeat3(0f);

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
        this.red = red / 255f;
        this.green = green / 255f;
        this.blue = blue / 255f;
        this.alpha = 1;
    }

    public Color(final int red, final int green, final int blue, final int alpha) {
        this.red = red / 255f;
        this.green = green / 255f;
        this.blue = blue / 255f;
        this.alpha = alpha / 255f;
    }

    public Color(final java.awt.Color color) {
        this.red = color.getRed() / 255f;
        this.green = color.getGreen() / 255f;
        this.blue = color.getBlue() / 255f;
        this.alpha = color.getAlpha() / 255f;
    }

    public Color withAlpha(final float newAlpha) { return new Color(red, green, blue, newAlpha); }

    //public void bind() { glColor4f(red, green, blue, alpha); }

    public java.awt.Color toAwtColor() {
        return new java.awt.Color(
                Math.round(red * 255),
                Math.round(green * 255),
                Math.round(blue * 255),
                Math.round(alpha * 255)
        );
    }
}