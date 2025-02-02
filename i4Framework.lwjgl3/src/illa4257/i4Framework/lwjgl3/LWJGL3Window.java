package illa4257.i4Framework.lwjgl3;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import org.lwjgl.opengl.GLCapabilities;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class LWJGL3Window implements FrameworkWindow {
    private final LWJGL3Framework framework;
    private final Window window;
    final long windowID;
    GLCapabilities capabilities = null;

    int width, height;

    private final ConcurrentLinkedQueue<EventListener> listeners = new ConcurrentLinkedQueue<>();

    public LWJGL3Window(final LWJGL3Framework framework, Window window) {
        this.framework = framework;
        if (window == null)
            window = new Window();
        this.window = window;

        final boolean isVisible = window.isVisible();
        if (isVisible)
            window.link();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, isVisible ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        String title = window.getTitle();
        if (title == null)
            title = "";
        this.width = Math.max(window.width.calcInt(), 1);
        this.height = Math.max(window.height.calcInt(), 1);
        windowID = glfwCreateWindow(this.width, this.height, title, NULL, framework.sharedContext);

        glfwSetFramebufferSizeCallback(windowID, this::onResize);

        listeners.add(window.addDirectEventListener(VisibleEvent.class, e -> {
            if (e.value) {
                framework.addWindow(this);
                glfwShowWindow(windowID);
                this.window.link();
            } else {
                framework.removeWindow(this);
                glfwHideWindow(windowID);
                this.window.unlink();
            }
        }));
        listeners.add(window.addEventListener(RecalculateEvent.class, e -> {
            if (width == this.window.width.calcInt() && height == this.window.height.calcInt())
                return;
            glfwSetWindowSize(windowID, Math.max(this.window.width.calcInt(), 1), Math.max(this.window.height.calcInt(), 1));
        }));
    }

    private void onResize(final long id, final int width, final int height) {
        this.width = width;
        this.height = height;
        glViewport(0, 0, width, height);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, 1, -1);
        glMatrixMode(GL_MODELVIEW);
        window.setSize(width, height);
    }

    @Override
    public LWJGL3Framework getFramework() {
        return framework;
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public void dispose() {
        glfwFreeCallbacks(windowID);
        glfwDestroyWindow(windowID);
    }
}
