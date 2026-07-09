package illa4257.i4Framework.base.styling;

import java.util.ArrayList;

public class StyleCall {
    public final String name;
    public final ArrayList<ArrayList<Object>> objs = new ArrayList<>();

    public StyleCall(final String s) {
        this.name = s;
    }

    @Override
    public String toString() {
        return name + objs;
    }
}