package illa4257.i4Framework.desktop;

import illa4257.i4Utils.media.Image;
import illa4257.i4Utils.media.ImagePixelable;

import java.awt.image.BufferedImage;

public class BufImgRef implements ImagePixelable {
    public final BufferedImage image;

    public BufImgRef(final BufferedImage img) { image = img; }

    @Override
    public int[] getPixels() {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    public static BufImgRef compute(final Image img) {
        final BufferedImage bufferedImage = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB);
        if (img.pixels != null)
            bufferedImage.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
        else if (img.byteBuffer != null)
            if (img.byteBuffer.isDirect()) {
                img.pixels = new int[img.width * img.height];
                img.byteBuffer.asIntBuffer().get(img.pixels);
                bufferedImage.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
            } else
                bufferedImage.setRGB(0, 0, img.width, img.height, img.pixels = img.byteBuffer.asIntBuffer().array(), 0, img.width);
        else
            bufferedImage.setRGB(0, 0, img.width, img.height, img.directIntArray(), 0, img.width);
        return new BufImgRef(bufferedImage);
    }
}