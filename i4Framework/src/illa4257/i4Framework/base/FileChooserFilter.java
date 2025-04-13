package illa4257.i4Framework.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileChooserFilter {
    public final ConcurrentLinkedQueue<FileChooserFilter> filters;
    public final String description;
    public final Iterable<String> patterns;

    public FileChooserFilter(final String description) {
        filters = new ConcurrentLinkedQueue<>();
        this.description = description;
        patterns = new ConcurrentLinkedQueue<>();
        filters.add(this);
    }

    public FileChooserFilter(final String description, final Iterable<String> patterns) {
        filters = new ConcurrentLinkedQueue<>();
        this.description = description;
        this.patterns = patterns != null ? patterns : new ConcurrentLinkedQueue<>();
        filters.add(this);
    }

    public FileChooserFilter(final ConcurrentLinkedQueue<FileChooserFilter> filters, final String description) {
        this.filters = filters != null ? filters : new ConcurrentLinkedQueue<>();
        this.description = description;
        patterns = new ConcurrentLinkedQueue<>();
        this.filters.add(this);
    }

    public FileChooserFilter(final ConcurrentLinkedQueue<FileChooserFilter> filters, final String description, final Iterable<String> patterns) {
        this.filters = filters != null ? filters : new ConcurrentLinkedQueue<>();
        this.description = description;
        this.patterns = patterns != null ? patterns : new ConcurrentLinkedQueue<>();
        this.filters.add(this);
    }

    public FileChooserFilter addPattern(final String... pattern) {
        if (patterns instanceof Collection)
            ((Collection<String>) patterns).addAll(Arrays.asList(pattern));
        return this;
    }

    public FileChooserFilter next(final String description) {
        return new FileChooserFilter(filters, description);
    }

    public FileChooserFilter next(final String description, final Iterable<String> patterns) {
        return new FileChooserFilter(filters, description, patterns);
    }
}