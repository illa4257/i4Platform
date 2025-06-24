package illa4257.i4Framework.android;

import android.graphics.Bitmap;
import illa4257.i4Utils.media.Image;
import illa4257.i4Utils.media.ImageBufferable;

import java.nio.ByteBuffer;

public class AndroidImage implements ImageBufferable {
    public Bitmap bitmap;

    public AndroidImage(final Bitmap bitmap) { this.bitmap = bitmap; }

    @Override
    public ByteBuffer getByteBuffer() {
        final ByteBuffer b = ByteBuffer.allocate(bitmap.getRowBytes() * bitmap.getHeight());
        bitmap.copyPixelsToBuffer(b);
        return b;
    }

    public static AndroidImage compute(final Image img) {
        final Bitmap b = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888);
        b.copyPixelsFromBuffer(img.asByteBuffer());
        return new AndroidImage(b);
    }
}
