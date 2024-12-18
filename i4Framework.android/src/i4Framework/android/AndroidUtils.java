package i4Framework.android;

import android.graphics.Bitmap;
import i4Framework.base.Image;

public class AndroidUtils {
    public static Bitmap toBitmap(final Image image) {
        return (Bitmap) image.imageMap.computeIfAbsent(Bitmap.class, ignored -> {
            final Bitmap b = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888);
            b.copyPixelsFromBuffer(image.asByteBuffer());
            return b;
        });
    }
}