package i4Framework.base;

import i4Framework.base.components.Component;
import i4Framework.base.components.Window;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Framework {
    private static final ConcurrentLinkedQueue<Framework> frameworks = new ConcurrentLinkedQueue<>();

    public static void registerFramework(final Framework framework) {
        frameworks.add(framework);
    }

    public static boolean isThread(final Component component) {
        for (final Framework f : frameworks)
            if (f.isUIThread(component))
                return true;
        return false;
    }


    public abstract boolean isUIThread(final Component component);

    public abstract FrameworkWindow newWindow(final Window window);
}