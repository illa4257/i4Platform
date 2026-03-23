package illa4257.i4Utils.lists;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Sorted<T> implements Iterable<T> {
    private final Comparator<T> comparator;

    private volatile Node<T> root = null;

    public Sorted(final Comparator<T> comparator) {
        if (comparator == null)
            throw new IllegalArgumentException();
        this.comparator = comparator;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> cur = root;

            @Override
            public boolean hasNext() {
                return cur != null;
            }

            @Override
            public T next() {
                if (cur == null)
                    throw new NoSuchElementException();
                final T r = cur.elem;
                cur = cur.next;
                return r;
            }
        };
    }

    private static class Node<T> {
        public volatile T elem;
        public volatile Node<T> next = null;

        public Node(final T elem) {
            this.elem = elem;
        }
    }

    public T peek() {
        final Node<T> n = root;
        return n != null ? n.elem : null;
    }

    public synchronized T poll() {
        final Node<T> n = root;
        if (n == null)
            return null;
        root = n.next;
        return n.elem;
    }

    public synchronized void add(final T elem) {
        if (root == null) {
            root = new Node<>(elem);
            return;
        }
        Node<T> p = root, c;
        if (comparator.compare(elem, p.elem) < 1) {
            c = new Node<>(elem);
            c.next = p;
            root = c;
            return;
        }
        c = root.next;
        while (true) {
            if (c == null) {
                p.next = new Node<>(elem);
                return;
            }
            if (comparator.compare(elem, c.elem) >= 1) {
                p = c;
                c = c.next;
                continue;
            }
            c = new Node<>(elem);
            c.next = p.next;
            p.next = c;
            return;
        }
    }

    public synchronized boolean remove(final T elem) {
        if (root == null)
            return false;
        if (elem.equals(root.elem)) {
            root = root.next;
            return true;
        }
        Node<T> p = root, c;
        while (true) {
            c = p.next;
            if (c == null)
                return false;
            if (elem.equals(c.elem)) {
                p.next = c.next;
                return true;
            }
            p = c;
        }
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append('[');
        Node<T> n = root;
        if (n != null) {
            b.append(n.elem);
            for (n = n.next; n != null; n = n.next)
                b.append(", ").append(n.elem);
        }
        b.append(']');
        return b.toString();
    }
}