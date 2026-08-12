package illa4257.i4Framework.base.points;

import illa4257.i4Utils.Destructor;

import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import static illa4257.i4Framework.base.Framework.L;

public abstract class Point extends Destructor {
    private static final ThreadLocal<Stack<Runnable>> reseting = ThreadLocal.withInitial(Stack::new);
    private final ConcurrentLinkedQueue<Runnable> subscribed = new ConcurrentLinkedQueue<>();

    public final Runnable reset = this::reset;

    public void reset() {
        final Stack<Runnable> stack = reseting.get();
        for (final Runnable s : subscribed) {
            if (stack.contains(s)) {
                L.d("There's a loop " + s + " in " + stack, Thread.currentThread().getStackTrace());
                continue;
            }
            stack.push(s);
            s.run();
            stack.pop();
        }
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