package i4l.common;

import java.util.ArrayList;

public class I4LMethod extends I4LCode {
    public ArrayList<String> tags = new ArrayList<>();
    public String name = null;
    public final ArrayList<I4LArg> args = new ArrayList<>();
    public final ArrayList<String> exceptions = new ArrayList<>();
}