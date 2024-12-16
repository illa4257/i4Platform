package i4l.common;

import java.util.List;

public class I4LType {
    public String name;
    public List<Object> generics;
    public int arrayLevels = 0;

    public I4LType(final String name) {
        this.name = name;
        this.generics = null;
    }

    public I4LType(final String name, final List<Object> generics) {
        this.name = name;
        this.generics = generics;
    }

    public I4LType(final String name, final List<Object> generics, final int arrayLevels) {
        this.name = name;
        this.generics = generics;
        this.arrayLevels = arrayLevels;
    }

    @Override
    public String toString() { return "TYPE(" + name + ")<" + generics + ">*" + arrayLevels; }
}