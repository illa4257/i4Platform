package i4Framework.base.points;

import i4Utils.IDestructor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Point implements IDestructor {
    private final AtomicInteger linkNumber = new AtomicInteger(0);
    private float cacheFloat;
    private int cacheInt;

    private boolean cf = false, ci = false;

    private final ConcurrentLinkedQueue<Runnable> subscribed = new ConcurrentLinkedQueue<>();

    public void silentReset() {
        ci = cf = false;
    }

    public void reset() {
        ci = cf = false;
        for (final Runnable s : subscribed)
            s.run();
    }

    public void fireAll() {
        for (final Runnable s : subscribed)
            s.run();
    }

    public boolean subscribe(final Runnable listener) {
        if (listener == null)
            return false;
        if (subscribed.add(listener))
            link();
        else
            return false;
        return true;
    }

    public boolean unsubscribe(final Runnable listener) {
        if (listener == null)
            return false;
        if (subscribed.remove(listener))
            unlink();
        else
            return false;
        return true;
    }

    protected abstract float calc();

    public float calcFloat() {
        if (cf)
            return cacheFloat;
        cacheFloat = calc();
        cf = true;
        return cacheFloat;
    }

    public int calcInt() {
        if (ci)
            return cacheInt;
        cacheInt = Math.round(calcFloat());
        ci = true;
        return cacheInt;
    }

    @Override public int getLinkNumber() { return linkNumber.get(); }
    @Override public int addLinkNumber() { return linkNumber.incrementAndGet(); }
    @Override public int decLinkNumber() { return linkNumber.decrementAndGet(); }
}