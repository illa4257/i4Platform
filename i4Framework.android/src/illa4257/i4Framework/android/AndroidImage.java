package illa4257.i4Framework.android;

import android.graphics.Bitmap;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.graphics.ImagePixelable;

public class AndroidImage implements ImagePixelable {
    public Bitmap bitmap;

    public AndroidImage(final Bitmap bitmap) { this.bitmap = bitmap; }

    /*@Override
    public ByteBuffer getByteBuffer() {
        final ByteBuffer b = ByteBuffer.allocate(bitmap.getRowBytes() * bitmap.getHeight());
        bitmap.copyPixelsToBuffer(b);
        return b;
    }*/

    public static AndroidImage compute(final Image img) {
        final Bitmap b = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888);
        b.copyPixelsFromBuffer(img.asByteBuffer());
        return new AndroidImage(b);
    }

    @Override
    public int[] getPixels() {
        final int w = bitmap.getWidth(), h = bitmap.getHeight();
        final int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        return pixels;
    }
}