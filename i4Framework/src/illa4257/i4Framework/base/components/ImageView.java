package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Utils.media.Image;

public class ImageView extends Component {
    public volatile Image image = null;

    public ImageView() {}
    public ImageView(final Image image) { this.image = image; }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final Image img = image;
        if (img == null)
            return;
        final float w = width.calcFloat(), h = height.calcFloat(),
                s = Math.min(w / img.width, h / img.height),
                sw = s * w, sh = s * h;
        ctx.drawImage(img, (w - sw) / 2, (h - sh) / 2, sw, sh);
    }
}