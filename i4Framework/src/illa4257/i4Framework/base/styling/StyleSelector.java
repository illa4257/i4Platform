package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.ConcurrentLinkedQueue;

public class StyleSelector {
    public final SyncVar<String> id = new SyncVar<>(), tag = new SyncVar<>();
    public final ConcurrentLinkedQueue<String> classes = new ConcurrentLinkedQueue<>(),
            pseudoClasses = new ConcurrentLinkedQueue<>();
    public volatile StyleSelector parent;

    public StyleSelector() {
        parent = null;
    }

    public StyleSelector(final StyleSelector parent) {
        this.parent = parent;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public StyleSelector clone() {
        final StyleSelector n = new StyleSelector(), p = parent;
        n.id.set(id.get());
        n.tag.set(tag.get());
        n.classes.addAll(classes);
        n.pseudoClasses.addAll(pseudoClasses);
        n.parent = p != null ? p.clone() : null;
        return n;
    }

    public boolean isIdEmpty() {
        final String id = this.id.get();
        return id == null || id.isEmpty();
    }

    public boolean isTagEmpty() {
        final String tag = this.tag.get();
        return tag == null || tag.isEmpty();
    }

    public boolean isEmpty() {
        return isIdEmpty() && isTagEmpty() && classes.isEmpty() && pseudoClasses.isEmpty();
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
        if (tag != null && !tag.isEmpty() && !tag.equals("*") && !tag.equalsIgnoreCase(component.tag.get()))
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

    public StyleSelector getFirstParent() {
        StyleSelector c = this;
        while (c.parent != null)
            c = c.parent;
        return c;
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