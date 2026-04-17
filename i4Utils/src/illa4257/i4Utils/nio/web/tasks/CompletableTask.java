package illa4257.i4Utils.nio.web.tasks;

public class CompletableTask extends QTask {
    @Override
    public void tick() {}

    @Override public void complete() { super.complete(); }
}