package illa4257.i4Utils.conc;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SpaceMgr {
    public final AtomicLong remaining;
    private final ConcurrentLinkedQueue<P> q = new ConcurrentLinkedQueue<>();

    private static class P {
        public final long len;
        public final Runnable callback;

        public P(final long len, final Runnable callback) { this.len = len; this.callback = callback; }

        @Override
        public int hashCode() {
            return callback.hashCode();
        }

        @SuppressWarnings("EqualsDoesntCheckParameterClass")
        @Override
        public boolean equals(final Object obj) {
            return callback.equals(obj);
        }
    }

    public SpaceMgr(final long remaining) {
        this.remaining = new AtomicLong(remaining);
    }

    public SpaceMgr(final AtomicLong remaining) {
        this.remaining = remaining;
    }

    public void alloc(final long l, final Runnable callback) {
        while (true) {
            final long r = remaining.get(), n = r - l;
            if (n < 0) {
                q.offer(new P(l, callback));
                return;
            }
            if (remaining.compareAndSet(r, n)) {
                callback.run();
                return;
            }
        }
    }

    public void free(final long l) {
        long r = remaining.addAndGet(l), d;
        for (final P p : q) {
            while (true) {
                if (p.len > r)
                    break;
                d = r - p.len;
                if (remaining.compareAndSet(r, d)) {
                    if (q.remove(p)) {
                        r = d;
                        p.callback.run();
                    } else
                        remaining.getAndAdd(p.len);
                    break;
                }
                r = remaining.get();
            }
        }
    }
}