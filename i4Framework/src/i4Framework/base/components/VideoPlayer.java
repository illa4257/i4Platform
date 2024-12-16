package i4Framework.base.components;

import i4Framework.base.Color;
import i4Framework.base.Context;
import i4Framework.base.IFrameGrabber;
import i4Framework.base.Image;
import i4Framework.base.events.components.RepaintEvent;
import i4Utils.logger.i4Logger;

public class VideoPlayer extends Component {
    private boolean isPlaying = false;
    private int index = 0;
    private IFrameGrabber frameGrabber = null;
    private Image lastFrame = null;

    private final Runnable repaint = () -> fire(new RepaintEvent());

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
                onTick(repaint);
            else
                offTick(repaint);
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
                    offTick(repaint);
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