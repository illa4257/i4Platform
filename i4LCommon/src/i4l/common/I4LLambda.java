package i4l.common;

import java.util.List;

public class I4LLambda {
    public List<Argument> arguments;
    public Object operation = null;

    public I4LLambda(final List<Argument> arguments) { this.arguments = arguments; }

    public static class Argument {
        public String name, type = null;
        public boolean isFinal = true;

        public Argument(final String name) { this.name = name; }
    }
}