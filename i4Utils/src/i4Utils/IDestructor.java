package i4Utils;

public interface IDestructor {
    int getLinkNumber();
    int addLinkNumber();
    int decLinkNumber();

    default void onConstruct() {}
    default void onDestruct() {}

    default void link() {
        if (addLinkNumber() == 1)
            onConstruct();
    }

    default void unlink() {
        final int n = decLinkNumber();
        if (n == 0)
            onDestruct();
        else if (n < 0) {
            addLinkNumber();
            System.err.println("[Point] Negative number of links.");
            Log.printStacktrace(Thread.currentThread().getStackTrace());
        }
    }
}