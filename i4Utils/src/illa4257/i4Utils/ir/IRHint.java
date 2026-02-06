package illa4257.i4Utils.ir;

import java.util.ArrayList;

public class IRHint {
    public final IRHints hint;
    public ArrayList<Object> params = new ArrayList<>();

    public IRHint(IRHints hint) {
        this.hint = hint;
    }

    @Override
    public String toString() {
        return "HINT(" + hint + params + ")";
    }
}