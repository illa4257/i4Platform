package i4l.common;

import java.util.ArrayList;
import java.util.List;

public class I4LClass extends I4LCode {
    public List<Object> tags = new ArrayList<>();

    @Override
    public String toString() {
        return "CLASS " + tags;
    }
}