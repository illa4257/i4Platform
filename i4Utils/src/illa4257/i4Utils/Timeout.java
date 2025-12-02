package illa4257.i4Utils;

public class Timeout {
    private final Object locker = new Object();
    private volatile boolean isNotified = false;

    public void reset() { isNotified = false; }

    public void finish() {
        isNotified = true;
        locker.notifyAll();
    }

    public boolean join(final long ms) throws InterruptedException {
        synchronized (locker) {
            if (!isNotified)
                locker.wait(ms);
            return isNotified;
        }
    }

    public void join() throws InterruptedException {
        synchronized (locker) {
            if (!isNotified)
                locker.wait();
        }
    }
}