package illa4257.i4Framework.base.points;

import illa4257.i4Utils.Destructor;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Point extends Destructor {
    private final ConcurrentLinkedQueue<Runnable> subscribed = new ConcurrentLinkedQueue<>();

    public void reset() {
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
        if (subscribed.offer(listener))
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

    public abstract float calcFloat();
    public int calcInt() { return Math.round(calcFloat()); }
}