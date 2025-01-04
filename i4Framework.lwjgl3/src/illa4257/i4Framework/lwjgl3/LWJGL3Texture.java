package illa4257.i4Framework.lwjgl3;

import org.lwjgl.opengl.GL11;

import java.io.Closeable;

public class LWJGL3Texture implements Closeable {
    public final int textureID, width, height;

    public LWJGL3Texture(final int textureID, final int width, final int height) {
        this.textureID = textureID;
        this.width = width;
        this.height = height;
    }

    @Override
    public void close() {
        GL11.glDeleteTextures(textureID);
    }
}