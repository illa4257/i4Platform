package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Utils.MiniUtil;

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
    }

    @Override
    public Component find(float x, float y, final float[] localPos) {
        final Component r = super.find(x, y, localPos);
        if (r != null) {
            x -= r.startX.calcFloat();
            y -= r.startY.calcFloat();
            Component r2;
            for (final Component c : components)
                if ((r2 = c.find(x, y, localPos)) != null)
                    return r2;
            localPos[0] = x;
            localPos[1] = y;
        }
        return r;
    }

    public boolean contains(final Component component) { return components.contains(component); }

    public void clear() {
        Component c;
        if (getLinkNumber() > 0)
            while ((c = components.poll()) != null) {
                c.unlink();
                fire(new RemoveComponentEvent(this, c));
            }
        else
            while ((c = components.poll()) != null)
                fire(new RemoveComponentEvent(this, c));
    }

    public boolean add(final Component component) {
        final boolean r = component != null && this.components.add(component);
        if (r) {
            component.densityMultiplier.set(densityMultiplier);
            final Container c = component.parent.getAndSet(this);
            if (getLinkNumber() > 0)
                component.link();
            if (c != null && c != this)
                c.remove(c);
            component.fire(new ChangeParentEvent(component));
            fire(new AddComponentEvent(this, component));
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
            component.fire(new ChangeParentEvent(component));
            fire(new RemoveComponentEvent(this, component));
            updated();
        }
        return r;
    }

    public void toFront(final Component component) {
        if (components.remove(component) && components.offer(component))
            component.fire(new ChangeZ(component, MiniUtil.indexOf(component, components)));
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

    protected void unfocus(final boolean u) {
        if (focused != null) {
            if (u)
                setPseudoClass("focus-within", false);
            if (focused instanceof Container)
                ((Container) focused).unfocus(true);
            if (focused.isFocused()) {
                focused.setPseudoClass("focus-within", false);
                focused.setPseudoClass("focus", false);
                focused.fire(new FocusEvent(focused, false));
            }
            if (u)
                focused = null;
        }
    }

    public Component getFocusedComponent() {
        synchronized (locker) {
            return focused instanceof Container ? ((Container) focused).getFocusedComponent() : focused;
        }
    }

    protected boolean childFocus(final Component targetChild, final Component target) {
        synchronized (locker) {
            if (!isVisible())
                return false;
            final Container p = getParent();
            if (p == null)
                return false;
            if (p.childFocus(this, target)) {
                targetChild.setPseudoClass("focus-within", true);
                targetChild.setPseudoClass("focus", true);
                if (focused == null)
                    setPseudoClass("focus-within", true);
                else
                    unfocus(false);
                focused = targetChild;
                if (targetChild == target)
                    focused.fire(new FocusEvent(focused, true));
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
                if (focused.pseudoClasses.contains("focus"))
                    focused.fire(new FocusEvent(focused, false));
                return true;
            }
            focused.fire(new FocusEvent(focused, false));
            boolean line = false;
            for (final Component c : components) {
                if (line) {
                    if (!c.isVisible())
                        continue;
                    if (c.isFocusable()) {
                        (focused = c).fire(new FocusEvent(focused, true));
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

    public int getComponentCount() { return components.size(); }

    @SuppressWarnings("NullableProblems")
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