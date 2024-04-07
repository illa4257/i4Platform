package i4l.common;

import java.util.ArrayList;

public class I4LArg {
    public final ArrayList<String> params = new ArrayList<>();

    @Override
    public String toString() {
        return params.toString();
    }
}