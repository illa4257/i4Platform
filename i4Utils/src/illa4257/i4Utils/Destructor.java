package illa4257.i4Utils;

import java.util.concurrent.atomic.AtomicInteger;

public class Destructor implements IDestructor {
    private final AtomicInteger linkNumber = new AtomicInteger(0);

    @Override public int getLinkNumber() { return linkNumber.get(); }
    @Override public int addLinkNumber() { return linkNumber.incrementAndGet(); }
    @Override public int decLinkNumber() { return linkNumber.decrementAndGet(); }
}