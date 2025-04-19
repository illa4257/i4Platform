package illa4257.i4Utils.runnables;

import java.io.IOException;

public interface FuncIOEx<R, A1> extends FuncEx<R, A1> {
    R accept(final A1 arg1) throws IOException;
}