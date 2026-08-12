package illa4257.i4Utils.runnables;

public interface FunctionEx<T, R, E extends Throwable> {
    R apply(final T argument) throws E;
}