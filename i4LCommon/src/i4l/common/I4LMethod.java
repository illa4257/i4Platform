package i4l.common;

import java.util.ArrayList;

public class I4LMethod extends I4LCode {
    public ArrayList<Object> tags = new ArrayList<>();
    public String name = null;
    public final ArrayList<I4LArg> args = new ArrayList<>();
    public final ArrayList<String> exceptions = new ArrayList<>();

    @Override
    public String toString() {
        return "Method " + tags + " " + name + " " + args + " throws " + exceptions;
    }
}