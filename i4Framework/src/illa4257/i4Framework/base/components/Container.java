package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Container extends Component implements Iterable<Component> {
    final ConcurrentLinkedQueue<Component> components = new ConcurrentLinkedQueue<>();

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
            fire(new AddComponentEvent(component));
        }
        return r;
    }

    public boolean remove(final Component component) {
        final boolean r = component != null && this.components.remove(component);
        if (r) {
            if (getLinkNumber() > 0)
                component.unlink();
            fire(new RemoveComponentEvent(component));
        }
        return r;
    }

    @Override
    public void invokeAll() {
        super.invokeAll();
        for (final Component component : components)
            component.invokeAll();
    }

    public final void paintComponents(final Context ctx) {
        for (final Component component : components)
            component.paint(ctx);
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