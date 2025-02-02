package illa4257.i4Framework.lwjgl3;

import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.components.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.awt.RenderingHints.*;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.system.MemoryUtil.NULL;

public class LWJGL3Framework extends Framework {
    public static final Object globalLocker = new Object();
    public static LWJGL3Framework INSTANCE = null;

    public final long sharedContext;
    final Object locker = new Object();
    final ArrayList<LWJGL3Window> windows = new ArrayList<>();

    static final Font font;
    static Map<RenderingHints.Key, Object> current, RECOMMENDED;

    final BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D g2d = tempImage.createGraphics();

    final ConcurrentHashMap<Character, LWJGL3Texture> characters = new ConcurrentHashMap<>();

    Thread thread = null;

    static {
        final HashMap<RenderingHints.Key, Object> m = new HashMap<>();

        m.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        m.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        m.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        m.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        RECOMMENDED = Collections.unmodifiableMap(m);

        current = RECOMMENDED;

        font = new Font(Font.DIALOG, Font.PLAIN, 16);
    }

    public LWJGL3Framework() throws IllegalStateException {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        sharedContext = glfwCreateWindow(1, 1, "", NULL, NULL);
        synchronized (globalLocker) {
            if (INSTANCE == null)
                INSTANCE = this;
        }
        glfwMakeContextCurrent(sharedContext);
        GL.createCapabilities();

        g2d.setFont(font);
        g2d.setRenderingHints(current);
    }

    void addWindow(final LWJGL3Window window) {
        synchronized (locker) {
            final boolean isEmpty = windows.isEmpty(), isAdded = windows.add(window);
            if (!isEmpty || !isAdded)
                return;
            thread = new Thread(() -> {
                final Thread current = Thread.currentThread();
                while (true) {
                    for (final LWJGL3Window w : windows) {
                        if (glfwWindowShouldClose(w.windowID))
                            glfwSetWindowShouldClose(w.windowID, false);
                        glfwMakeContextCurrent(w.windowID);
                        if (w.capabilities == null) {
                            w.capabilities = GL.createCapabilities();
                            glClearColor(0, 0, 0, 1);
                            glfwSwapInterval(1);

                            glEnable(GL_TEXTURE_2D);
                            glEnable(GL_BLEND);
                            //glEnable(GL_ALPHA_TEST);
                            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                            glViewport(0, 0, w.width, w.height);
                            glMatrixMode(GL_PROJECTION);
                            glLoadIdentity();
                            glOrtho(0, w.width, w.height, 0, 1, -1);
                            glMatrixMode(GL_MODELVIEW);
                        }
                        GL.setCapabilities(w.capabilities);
                        w.getWindow().invokeAll();

                        glClearColor(0, 0, 0, 0);
                        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                        glBindTexture(GL_TEXTURE_2D, 0);
                        glColor4f(1, 1, 1, 0);
                        glRectf(0, 0, 8, 8);

                        final LWJGL3Context ctx = new LWJGL3Context(this);
                        w.getWindow().paint(ctx);
                        w.getWindow().paintComponents(ctx);
                        ctx.dispose();

                        glfwSwapBuffers(w.windowID);
                    }

                    synchronized (locker) {
                        if (current != thread)
                            break;
                    }
                    glfwPollEvents();
                }
            });
            thread.start();
        }
    }

    void removeWindow(final LWJGL3Window window) {
        synchronized (locker) {
            if (windows.remove(window) && windows.isEmpty())
                thread = null;
        }
    }

    @Override
    public boolean isUIThread(Component component) {
        return Thread.currentThread() == thread;
    }

    @Override
    public FrameworkWindow newWindow(Window window) {
        return new LWJGL3Window(this, window);
    }

    @Override
    public void dispose() {
        for (final Map.Entry<Character, LWJGL3Texture> e : characters.entrySet())
            characters.remove(e.getKey()).close();
        glfwDestroyWindow(sharedContext);
        glfwTerminate();
    }
}