package illa4257.i4Utils.runnables;

public interface FunctionEx<R, E extends Throwable, T> {
    R accept(final T argument) throws E;
}