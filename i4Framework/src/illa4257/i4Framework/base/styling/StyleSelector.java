package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StyleSelector {
    public final SyncVar<String> id = new SyncVar<>(), tag = new SyncVar<>();
    public final ConcurrentLinkedQueue<String> classes = new ConcurrentLinkedQueue<>(),
            pseudoClasses = new ConcurrentLinkedQueue<>();
    public volatile StyleSelector parent = null;

    public boolean isIdEmpty() {
        final String id = this.id.get();
        return id == null || id.isEmpty();
    }

    public boolean check(final Component component) {
        final String id = this.id.get();
        if (id != null && !id.isEmpty())
            if (!id.equals(component.id.get()))
                return false;
        for (final String cls : classes)
            if (!component.classes.contains(cls))
                return false;
        final String tag = this.tag.get();
        if (tag != null && !tag.isEmpty() && !tag.equalsIgnoreCase(component.tag.get()))
            return false;
        final StyleSelector parent = this.parent;
        if (parent != null) {
            final Container p = component.getParent();
            return p != null && parent.check(p);
        }
        return true;
    }

    public StyleSelector setID(final String newID) {
        id.set(newID);
        return this;
    }

    public StyleSelector addClass(final String cls) {
        classes.add(cls);
        return this;
    }

    public StyleSelector addPseudoClass(final String pseudoClass) {
        pseudoClasses.add(pseudoClass);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder r = new StringBuilder();
        r.append("StyleSelector{");

        final StyleSelector p = parent;
        if (p != null)
            r.append(p).append(" > ");

        String v = tag.get();
        if (v != null && !v.isEmpty())
            r.append(v);

        v = id.get();
        if (v != null && !v.isEmpty())
            r.append('#').append(v);

        v = String.join(".", classes);
        if (!v.isEmpty())
            r.append('.').append(v);

        v = String.join(":", pseudoClasses);
        if (!v.isEmpty())
            r.append(':').append(v);

        r.append('}');

        return r.toString();
    }
}