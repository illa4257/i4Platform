package i4Framework.base;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Image implements AutoCloseable {
    public final int width, height;
    private final Object locker = new Object();
    private ByteBuffer byteBuffer = null;
    private BufferedImage bufferedImage = null;

    public Image(final int width, final int height, final ByteBuffer byteBuffer) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Width and height should be more than 0");
        if (byteBuffer == null)
            throw new IllegalArgumentException("ByteBuffer is null!");
        this.width = width;
        this.height = height;
        this.byteBuffer = byteBuffer;
    }

    public Image(final BufferedImage bufferedImage) {
        if (bufferedImage == null)
            throw new IllegalArgumentException("Buffered Image is null!");
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        this.bufferedImage = bufferedImage;
    }

    public ByteBuffer asByteBuffer() {
        synchronized (locker) {
            if (byteBuffer == null) {
                final int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
                final ByteBuffer r = ByteBuffer.allocateDirect(pixels.length * 4);
                for (final int pixel : pixels) {
                    r.put((byte) ((pixel >> 16) & 0xFF));
                    r.put((byte) ((pixel >> 8) & 0xFF));
                    r.put((byte) (pixel & 0xFF));
                    r.put((byte) ((pixel >> 24) & 0xFF));
                }
                r.flip();
                byteBuffer = r;
            }
        }
        return byteBuffer.duplicate();
    }

    public BufferedImage asBufferedImage() {
        synchronized (locker) {
            if (bufferedImage == null) {
                bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                if (byteBuffer.isDirect()) {
                    final int[] pixels = new int[width * height];
                    byteBuffer.asIntBuffer().get(pixels);
                    bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
                } else
                    bufferedImage.setRGB(0, 0, width, height, byteBuffer.asIntBuffer().array(), 0, width);
            }
        }
        return bufferedImage;
    }

    public final ConcurrentHashMap<Object, Object> imageMap = new ConcurrentHashMap<>();

    @Override
    public void close() throws Exception {
        synchronized (locker) {
            if (byteBuffer != null) {
                byteBuffer.clear();
                byteBuffer = null;
            }
        }
        final Iterator<Map.Entry<Object, Object>> cache = imageMap.entrySet().iterator();
        while (cache.hasNext()) {
            final Object v = cache.next().getValue();
            if (v instanceof Closeable)
                ((Closeable) v).close();
            else if (v instanceof AutoCloseable)
                ((AutoCloseable) v).close();
            cache.remove();
        }
    }
}