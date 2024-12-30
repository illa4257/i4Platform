package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

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
            i4Logger.INSTANCE.log(Level.ERROR, "Negative number of links.", Thread.currentThread().getStackTrace());
        }
    }
}