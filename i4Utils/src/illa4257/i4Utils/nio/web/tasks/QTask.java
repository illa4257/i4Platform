package illa4257.i4Utils.nio.web.tasks;

public abstract class QTask extends Task {
    public int interestOps = 0;
    public Task next = null;

    public QTask setQ(final Task nextTask, final int interestOps) {
        this.next = nextTask;
        this.interestOps = interestOps;
        return this;
    }

    protected void complete() {
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