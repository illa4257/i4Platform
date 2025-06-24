package illa4257.i4Framework.base.graphics;

import illa4257.i4Utils.media.Image;
import illa4257.i4Utils.logger.i4Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadedFrameGrabber implements IFrameGrabber {
    private final Object locker = new Object();
    private final int nThreads;
    private int lastFrameIndex = 0;

    private final IFrameGrabber grabber;

    private final ExecutorService service;

    private static class Entry {
        public final Object locker = new Object();
        public int frameIndex = -1;
        public Image frame = null;
    }

    private final Entry[] frames;

    public ThreadedFrameGrabber(final IFrameGrabber frameGrabber, int nThreads) {
        if (nThreads < 1)
            throw new IllegalArgumentException("nThreads cannot be less then 1");
        this.nThreads = nThreads;
        frames = new Entry[nThreads];
        grabber = frameGrabber;
        service = Executors.newFixedThreadPool(nThreads);

        for (int i = 0; i < nThreads; i++)
            frames[i] = new Entry();
        service.execute(() -> run(grabber, 0));
        for (int i = 1; i < nThreads; i++) {
            final int o = i;
            service.execute(() -> run(grabber.clone(), o));
        }
    }

    private void run(final IFrameGrabber grabber, final int offset) {
        final Entry e = frames[offset];
        try {
            while (true) {
                int newIndex, last;
                synchronized (locker) {
                    newIndex = Math.floorDiv(lastFrameIndex, nThreads) * nThreads + offset;
                    if (lastFrameIndex > newIndex)
                        newIndex++;
                    last = lastFrameIndex;
                }
                synchronized (e.locker) {
                    if (e.frameIndex != newIndex) {
                        e.frameIndex = newIndex;
                        e.frame = grabber.get(newIndex);
                        e.locker.notifyAll();
                    }
                }
                synchronized (locker) {
                    if (last == lastFrameIndex)
                        locker.wait();
                }
            }
        } catch (final InterruptedException ex) {
            i4Logger.INSTANCE.log(ex);
        }
    }

    @Override public int fps() { return grabber.fps(); }

    @Override
    public Image get(final int index) {
        synchronized (locker) {
            lastFrameIndex = index;
            locker.notifyAll();
        }
        final Entry e = frames[index % nThreads];
        synchronized (e.locker) {
            if (e.frameIndex != index)
                try {
                    e.locker.wait();
                } catch (final InterruptedException ex) {
                    i4Logger.INSTANCE.log(ex);
                }
            return e.frame;
        }
    }

    @Override
    public IFrameGrabber clone() {
        return new ThreadedFrameGrabber(grabber.clone(), nThreads);
    }
}
