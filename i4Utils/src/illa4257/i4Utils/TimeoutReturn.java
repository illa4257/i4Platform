package illa4257.i4Utils;

public class TimeoutReturn<T> {
    private final Object locker = new Object();
    private volatile boolean isNotified = false, returned = false, threw = false;
    private volatile T value = null;
    private volatile Throwable throwable = null;

    public void reset() { isNotified = returned = threw = false; }

    public void finishNotify() {
        synchronized (locker) {
            isNotified = true;
            locker.notifyAll();
        }
    }

    public void err(final Throwable t) {
        synchronized (locker) {
            if (!isNotified) {
                isNotified = true;
                locker.notifyAll();
            }
            threw = true;
            throwable = t;
            locker.notifyAll();
        }
    }

    public void finish(final T value) {
        synchronized (locker) {
            if (!isNotified) {
                isNotified = true;
                locker.notifyAll();
            }
            this.value = value;
            returned = true;
            locker.notifyAll();
        }
    }

    public boolean join(final long ms) throws InterruptedException {
        synchronized (locker) {
            if (!isNotified)
                locker.wait(ms);
            if (isNotified && !returned && !threw)
                locker.wait();
            return isNotified;
        }
    }

    public T join() throws InterruptedException {
        synchronized (locker) {
            if (!isNotified)
                locker.wait();
            if (!returned && !threw)
                locker.wait();
            if (threw)
                throw new RuntimeException(throwable);
            return value;
        }
    }

    public T getValue() {
        if (threw)
            throw new RuntimeException(throwable);
        return value;
    }
}