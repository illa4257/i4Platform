package illa4257.i4Utils.runnables;

public interface SupplierEx<R, E extends Throwable> {
    R run() throws E;
}