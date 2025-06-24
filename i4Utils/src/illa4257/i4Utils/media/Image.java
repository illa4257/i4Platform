package illa4257.i4Utils.media;

import illa4257.i4Utils.logger.i4Logger;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Image implements Closeable {
    public final int width, height;
    private final Object locker = new Object();
    public volatile int[] pixels = null;
    public volatile ByteBuffer byteBuffer = null;

    public Image(final int width, final int height, final Object key, final Object img) {
        if (key == null || img == null)
            throw new IllegalArgumentException("Image is null!");
        this.width = width;
        this.height = height;
        imageMap.put(key, img);
    }

    public Image(final int width, final int[] pixels) {
        this.width = width;
        this.height = pixels.length / width;
        this.pixels = pixels;
    }

    public Image(final int width, final int height, final ByteBuffer byteBuffer) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Width and height should be more than 0");
        if (byteBuffer == null)
            throw new IllegalArgumentException("ByteBuffer is null!");
        this.width = width;
        this.height = height;
        this.byteBuffer = byteBuffer;
    }

    public boolean hasIntArray() { return pixels != null; }
    public boolean hasByteBuffer() { return byteBuffer != null; }

    public int[] directIntArray() {
        if (pixels == null)
            synchronized (locker) {
                if (pixels == null) {
                    for (final Object o : imageMap.values())
                        if (o instanceof ImagePixelable)
                            return pixels = ((ImagePixelable) o).getPixels();
                    if (byteBuffer == null)
                        for (final Object o : imageMap.values())
                            if (o instanceof ImageBufferable) {
                                byteBuffer = ((ImageBufferable) o).getByteBuffer();
                                break;
                            }
                    if (byteBuffer != null)
                        if (byteBuffer.isDirect()) {
                            pixels = new int[width * height];
                            byteBuffer.asIntBuffer().get(pixels);
                        } else
                            pixels = byteBuffer.asIntBuffer().array();
                    else
                        throw new RuntimeException("No data.");
                }
            }
        return pixels;
    }

    public int[] asIntArray() { return Arrays.copyOf(directIntArray(), pixels.length); }

    public ByteBuffer asByteBuffer() {
        if (byteBuffer == null)
            synchronized (locker) {
                if (byteBuffer == null) {
                    for (final Object o : imageMap.values())
                        if (o instanceof ImageBufferable)
                            return byteBuffer = ((ImageBufferable) o).getByteBuffer();
                    if (pixels == null) {
                        for (final Object o : imageMap.values())
                            if (o instanceof ImagePixelable)
                                pixels = ((ImagePixelable) o).getPixels();
                        if (pixels == null)
                            throw new RuntimeException("No data.");
                    }
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

    public final ConcurrentHashMap<Object, Object> imageMap = new ConcurrentHashMap<>();

    @Override
    public void close() {
        synchronized (locker) {
            if (byteBuffer != null) {
                byteBuffer.clear();
                byteBuffer = null;
            }
        }
        final Iterator<Map.Entry<Object, Object>> cache = imageMap.entrySet().iterator();
        while (cache.hasNext()) {
            final Object v = cache.next().getValue();
            try {
                if (v instanceof Closeable)
                    ((Closeable) v).close();
                else if (v instanceof AutoCloseable)
                    ((AutoCloseable) v).close();
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
            cache.remove();
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "{width=" + width + ", height=" + height + ", images=" + imageMap.values() + '}';
    }
}