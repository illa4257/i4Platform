package illa4257.i4Framework.base.points;

public abstract class ACachePoint extends Point {
    private final Object locker = new Object();
    private volatile boolean cf = false;
    private float cachedFloat;

    @Override
    public void reset() {
        synchronized (locker) {
            cf = false;
        }
        super.reset();
    }

    protected abstract float calc();

    @Override
    public float calcFloat() {
        if (cf)
            return cachedFloat;
        synchronized (locker) {
            if (cf)
                return cachedFloat;
            cachedFloat = calc();
            cf = true;
            return cachedFloat;
        }
    }

    @Override public void onConstruct() { reset(); }
}