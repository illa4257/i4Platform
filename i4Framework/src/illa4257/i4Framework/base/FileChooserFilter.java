package illa4257.i4Framework.base;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileChooserFilter {
    public final ConcurrentLinkedQueue<FileChooserFilter> filters;
    public final AtomicInteger selected;
    public final String description;
    public final ConcurrentLinkedQueue<String> patterns = new ConcurrentLinkedQueue<>();

    public FileChooserFilter(final String description, final String... patterns) { this(null, description, patterns); }
    public FileChooserFilter(final String description, final Iterator<String> patterns) { this(null, description, patterns); }
    public FileChooserFilter(final String description, final Iterable<String> patterns) { this(null, description, patterns); }

    public FileChooserFilter(final FileChooserFilter base, final String description, final String... patterns) {
        this.filters = base != null ? base.filters : new ConcurrentLinkedQueue<>();
        this.selected = base != null ? base.selected : new AtomicInteger(0);
        this.description = description;
        if (patterns != null && patterns.length > 0)
            this.patterns.addAll(Arrays.asList(patterns));
        this.filters.add(this);
    }

    public FileChooserFilter(final FileChooserFilter base, final String description, final Iterator<String> patterns) {
        this.filters = base != null ? base.filters : new ConcurrentLinkedQueue<>();
        this.selected = base != null ? base.selected : new AtomicInteger(0);
        this.description = description;
        if (patterns != null)
            while (patterns.hasNext())
                this.patterns.add(patterns.next());
        this.filters.add(this);
    }

    public FileChooserFilter(final FileChooserFilter base, final String description, final Iterable<String> patterns) {
        this(base, description, patterns != null ? patterns.iterator() : null);
    }

    public FileChooserFilter select(final int index) {
        selected.set(index);
        return this;
    }

    public FileChooserFilter select() {
        int i = 0;
        for (final FileChooserFilter f : filters) {
            if (this == f) {
                selected.set(i);
                return this;
            }
            i++;
        }
        return this;
    }

    public FileChooserFilter addPatterns(final String... patterns) {
        if (patterns != null && patterns.length > 0)
            this.patterns.addAll(Arrays.asList(patterns));
        return this;
    }

    public FileChooserFilter addPatterns(final Iterator<String> patterns) {
        if (patterns != null)
            while (patterns.hasNext())
                this.patterns.add(patterns.next());
        return this;
    }

    public FileChooserFilter addPatterns(final Iterable<String> patterns) {
        return patterns != null ? addPatterns(patterns.iterator()) : this;
    }

    public FileChooserFilter next(final String description) { return new FileChooserFilter(this, description); }
    public FileChooserFilter next(final String description, final String... patterns) { return new FileChooserFilter(this, description, patterns); }
    public FileChooserFilter next(final String description, final Iterator<String> patterns) { return new FileChooserFilter(this, description, patterns); }
    public FileChooserFilter next(final String description, final Iterable<String> patterns) { return new FileChooserFilter(this, description, patterns); }

    public boolean check(final String filename) {
        for (final String pattern : patterns)
            if (pattern.startsWith("*")) {
                if (filename.endsWith(pattern.substring(1)))
                    return true;
            } else if (pattern.startsWith(".")) {
                if (filename.endsWith(pattern))
                    return true;
            } else if (filename.endsWith("." + pattern))
                return true;
        return false;
    }
}