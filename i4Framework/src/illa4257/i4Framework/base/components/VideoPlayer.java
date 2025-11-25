package illa4257.i4Framework.base.components;

import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.IFrameGrabber;
import illa4257.i4Utils.media.Image;
import illa4257.i4Utils.logger.i4Logger;

public class VideoPlayer extends Component {
    private boolean isPlaying = false;
    private int index = 0;
    private IFrameGrabber frameGrabber = null;
    private Image lastFrame = null;

    public void setFrameGrabber(final IFrameGrabber grabber) {
        synchronized (locker) {
            this.frameGrabber = grabber;
            lastFrame = null;
            index = 0;
            isPlaying = false;
        }
    }

    public void setPlaying(final boolean isPlaying) {
        synchronized (locker) {
            if (this.isPlaying == isPlaying)
                return;
            this.isPlaying = isPlaying;
            if (isPlaying)
                onTick(this::repaint);
            else
                offTick(this::repaint);
        }
    }

    long st = System.currentTimeMillis(), e;

    @Override
    public void paint(final Context ctx) {
        synchronized (locker) {
            if (isPlaying) {
                final Image newFrame = frameGrabber.get(index++);
                e = System.currentTimeMillis();
                System.out.println("TAKE FRAME " + (e - st));
                st = e;
                if (newFrame == null) {
                    isPlaying = false;
                    offTick(this::repaint);
                } else {
                    if (lastFrame != null)
                        try {
                            lastFrame.close();
                        } catch (final Exception exception) {
                            i4Logger.INSTANCE.log(exception);
                        }
                    lastFrame = newFrame;
                }
            }
            if (lastFrame == null) {
                ctx.setColor(Color.BLACK);
                ctx.drawRect(0, 0, width.calcFloat(), height.calcFloat());
            } else
                ctx.drawImage(lastFrame, 0, 0, width.calcFloat(), height.calcFloat());
        }
    }
}