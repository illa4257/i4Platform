package illa4257.i4Framework.base;

public abstract class ATexture {
    public final int width, height;

    public ATexture(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    public abstract void dispose();
}