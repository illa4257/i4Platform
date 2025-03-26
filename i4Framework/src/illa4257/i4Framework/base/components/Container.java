package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Container extends Component implements Iterable<Component> {
    final ConcurrentLinkedQueue<Component> components = new ConcurrentLinkedQueue<>();
    protected Component focused = null;

    public Container() {
        addEventListener(StyleUpdateEvent.class, e -> {
            for (final Component c : components)
                c.fire(e);
        });
        addEventListener(FocusEvent.class, e -> {
            if (e.value)
                return;
            synchronized (locker) {
                if (focused != null) {
                    focused.fire(new FocusEvent(false));
                    focused = null;
                }
            }
        });
    }

    public boolean contains(final Component component) { return components.contains(component); }

    public void clear() {
        Component c;
        if (getLinkNumber() > 0)
            while ((c = components.poll()) != null) {
                c.unlink();
                fire(new RemoveComponentEvent(c));
            }
        else
            while ((c = components.poll()) != null)
                fire(new RemoveComponentEvent(c));
    }

    public boolean add(final Component component) {
        final boolean r = component != null && this.components.add(component);
        if (r) {
            if (getLinkNumber() > 0)
                component.link();
            final Container c = component.parent.getAndSet(this);
            if (c != null && c != this)
                c.remove(c);
            component.fire(new ChangeParentEvent());
            fire(new AddComponentEvent(component));
            updated();
            if (component.isRepeated())
                repeated(true);
        }
        return r;
    }

    public boolean remove(final Component component) {
        final boolean r = component != null && this.components.remove(component);
        if (r) {
            if (getLinkNumber() > 0)
                component.unlink();
            component.parent.setIfEquals(null, this);
            component.fire(new ChangeParentEvent());
            fire(new RemoveComponentEvent(component));
            updated();
        }
        return r;
    }

    @Override public boolean isFocusable() { return false; }

    public Component getComponent(int i) {
        if (i < 0)
            return null;
        if (i == 0)
            return components.peek();
        for (final Component c : components) {
            if (i == 0)
                return c;
            i--;
        }
        return null;
    }

    protected boolean childFocus(final Component targetChild, final Component target) {
        synchronized (locker) {
            if (!isVisible())
                return false;
            final Container p = getParent();
            if (p == null)
                return false;
            if (p.childFocus(this, target)) {
                if (focused != null)
                    focused.fire(new FocusEvent(false));
                focused = targetChild;
                if (targetChild == target)
                    focused.fire(new FocusEvent(true));
                return true;
            }
            return false;
        }
    }

    public boolean focusNextElement() {
        synchronized (locker) {
            if (focused == null) {
                focused = components.peek();
                if (focused == null)
                    return false;
                if (!focused.isVisible())
                    return focusNextElement();
                if (!focused.isFocusable()) {
                    if (focused instanceof Container) {
                        final boolean r = ((Container) focused).focusNextElement();
                        if (r)
                            return true;
                    }
                    return focusNextElement();
                }
            }
            if (focused instanceof Container && ((Container) focused).focusNextElement()) {
                if (focused.isFocused)
                    focused.fire(new FocusEvent(false));
                return true;
            }
            focused.fire(new FocusEvent(false));
            boolean line = false;
            for (final Component c : components) {
                if (line) {
                    if (!c.isVisible())
                        continue;
                    if (c.isFocusable()) {
                        (focused = c).fire(new FocusEvent(true));
                        return true;
                    }
                    if (c instanceof Container && ((Container) c).focusNextElement()) {
                        focused = c;
                        return true;
                    }
                    continue;
                }
                if (c == focused)
                    line = true;
            }
            focused = null;
            return false;
        }
    }

    @Override
    protected void repeated(final boolean v) {
        if (v) {
            if (isRepeated.getAndSet(true))
                return;
        } else
            if (!repeatedInvoke.isEmpty() || components.stream().anyMatch(c -> c.isRepeated.get()))
                return;
        final Container c = getParent();
        if (c == null)
            return;
        c.repeated(v);
    }

    @Override
    public void invokeAll() {
        for (final Component component : components)
            component.invokeAll();
        super.invokeAll();
    }

    public final void paintComponents(final Context ctx) {
        for (final Component component : components) {
            final Context c = ctx.sub(component.startX.calcFloat(), component.startY.calcFloat(), component.width.calcFloat(), component.height.calcFloat());
            component.paint(c);
            if (component instanceof Container)
                ((Container) component).paintComponents(c);
            c.dispose();
        }
    }

    @Override public Iterator<Component> iterator() { return components.iterator(); }
    @Override public void forEach(final Consumer<? super Component> consumer) { components.forEach(consumer); }
    @Override public Spliterator<Component> spliterator() { return components.spliterator(); }

    @Override
    public void onConstruct() {
        super.onConstruct();
        for (final Component c : components)
            c.link();
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        for (final Component c : components)
            c.unlink();
    }
}