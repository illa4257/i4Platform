package illa4257.i4Framework.lwjgl3;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.base.math.Vector2D;
import illa4257.i4Utils.media.Image;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class LWJGL3Context implements Context {
    private final boolean isPushed;
    private final LWJGL3Framework framework;

    public LWJGL3Context(final LWJGL3Framework framework) {
        isPushed = false;
        this.framework = framework;
    }

    public LWJGL3Context(final LWJGL3Framework framework, final float x, final float y) {
        isPushed = true;
        this.framework = framework;
        glPushMatrix();
        GL11.glTranslatef(x, y, 0);
    }

    @Override
    public Context sub(float x, float y, float w, float h) {
        return new LWJGL3Context(framework, x, y);
    }

    @Override
    public void dispose() {
        if (isPushed)
            glPopMatrix();
    }

    @Override
    public Object getClipI() {
        return null;
    }

    @Override
    public float charWidth(final char ch) {
        return framework.g2d.getFontMetrics().charWidth(ch);
    }

    @Override
    public Vector2D bounds(final String string) {
        return Vector2D.fromSize(framework.g2d.getFontMetrics().getStringBounds(string, framework.g2d));
    }

    @Override
    public Vector2D bounds(char[] string) {
        return Vector2D.fromSize(framework.g2d.getFontMetrics().getStringBounds(string, 0, string.length, framework.g2d));
    }

    @Override
    public void setColor(final Color color) {
        GL11.glColor4f(color.red, color.green, color.blue, color.alpha);
    }

    @Override
    public void setStrokeWidth(float newWidth) {

    }

    @Override
    public void setClipI(Object clipArea) {

    }

    @Override
    public void setClip(IPath path) {

    }

    @Override
    public IPath newPath() {
        return null;
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {

    }

    @Override
    public void drawRect(final float x, final float y, final float w, final float h) {
        GL11.glRectf(x, y, x + w, y + h);
    }

    private LWJGL3Texture getChar(final char ch) {
        return framework.characters.computeIfAbsent(ch, k -> {
            final Vector2D v = bounds(Character.toString(ch));
            final BufferedImage tempImage = new BufferedImage(Math.round(v.x), Math.round(v.y), BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g2d = tempImage.createGraphics();
            g2d.setRenderingHints(LWJGL3Framework.current);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(LWJGL3Framework.font);
            final FontMetrics m = g2d.getFontMetrics();
            g2d.drawString(Character.toString(ch), 0, m.getLeading() + m.getAscent());
            g2d.dispose();
            final int[] pixels = tempImage.getRGB(0, 0, tempImage.getWidth(), tempImage.getHeight(), null, 0, tempImage.getWidth());
            final ByteBuffer r = ByteBuffer.allocateDirect(pixels.length * 4);
            for (final int pixel : pixels) {
                r.put((byte) ((pixel >> 16) & 0xFF));
                r.put((byte) ((pixel >> 8) & 0xFF));
                r.put((byte) (pixel & 0xFF));
                r.put((byte) ((pixel >> 24) & 0xFF));
            }
            r.flip();
            final int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tempImage.getWidth(), tempImage.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, r);
            GL30.glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0);
            return new LWJGL3Texture(id, tempImage.getWidth(), tempImage.getHeight());
        });
    }

    private void drawImageInternal(final int image, final float x, final float y, float width, float height) {
        glBindTexture(GL_TEXTURE_2D, image);
        glBegin(GL_QUADS);

        width += x;
        height += y;

        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(x, y);

        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(width, y);

        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(width, height);

        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(x, height);

        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void drawString(final String str, final float x, final float y) {
        drawString(str.toCharArray(), x, y);
    }

    @Override
    public void drawString(char[] str, float x, final float y) {
        for (final char ch : str) {
            if (ch == ' ') {
                x += (float) framework.g2d.getFontMetrics().getStringBounds(" ", framework.g2d).getWidth();
                continue;
            }
            final LWJGL3Texture t = getChar(ch);
            drawImageInternal(t.textureID, x, y, t.width, t.height);
            x += t.width;
        }
    }

    @Override
    public void drawImage(final Image image, final float x, final float y, final float width, final float height) {
        drawImageInternal(LWJGL3Utils.getID(framework, image), x, y, width, height);
    }
}