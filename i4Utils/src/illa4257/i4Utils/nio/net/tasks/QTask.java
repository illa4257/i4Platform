package illa4257.i4Utils.nio.net.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class QTask extends Task {
    private final AtomicBoolean c = new AtomicBoolean(false);
    public int interestOps = 0;
    public Task next = null;

    public QTask setQ(final Task nextTask, final int interestOps) {
        this.next = nextTask;
        this.interestOps = interestOps;
        return this;
    }

    protected void complete() {
        if (c.getAndSet(true))
            return;
        transport.attach(next);
        transport.interestOps(interestOps);
        //noinspection resource
        transport.getSelectionKey().selector().wakeup();
    }

    @Override
    public void recycle() {
        next.recycle();
    }
}