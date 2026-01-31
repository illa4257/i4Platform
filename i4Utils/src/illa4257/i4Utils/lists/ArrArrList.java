package illa4257.i4Utils.lists;

import illa4257.i4Utils.logger.i4Logger;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

public class ArrArrList<T> implements Iterable<T> {
    public final ExecutorService executorService;
    public final ConcurrentLinkedQueue<ArrArrList<T>.Page> list = new ConcurrentLinkedQueue<>();
    private final Object l = new Object();
    private volatile Lock lock = null;
    private final int stepSize;

    public class Lock {
        private final Object lo = new Object();
        private int nTasks = 0;
        public volatile Object result = null, resultBackup = null;

        private Lock() throws InterruptedException {
            synchronized (l) {
                while (lock != null)
                    l.wait();
                lock = this;
            }
        }

        private Lock(final Lock ignored) {}

        public void spread(final Runnable runnable) {
            synchronized (lo) { nTasks++; }
            final Object o = result;
            executorService.submit(() -> {
                if (result == o)
                    try {
                        runnable.run();
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.e(ex);
                    }
                synchronized (lo) { if (--nTasks <= 0) lo.notify(); }
            });
        }

        public Object join() throws InterruptedException {
            synchronized (lo) {
                while (nTasks > 0)
                    lo.wait();
            }
            synchronized (l) {
                if (lock == this)
                    lock = null;
                l.notify();
            }
            return result == null && resultBackup != null ? resultBackup : result;
        }
    }

    public class ArrPointer {
        public ArrPage page;
        public int index;

        public ArrPointer(final ArrPage page, final int index) {
            this.page = page;
            this.index = index;
        }
    }

    public abstract class Page implements Iterable<T> {
        public abstract int length();
        public abstract void contains(final Lock lock, final T element);
        public abstract T get(final int index);
        public void findFreeCell(final Lock lock) {}
        public abstract void deduplicate(final Lock lock, final T element);
        public abstract void deduplicate(final Lock lock, final T[] elements);
        public abstract void deduplicate(final Lock lock, final T[] elements, final BiFunction<T, T, Boolean> eq, final BiFunction<T, T, T> merge);
        public abstract void deduplicate(final Lock lock, final Page page);
        public abstract void deduplicate(final Lock lock, final Page page, final BiFunction<T, T, Boolean> eq, final BiFunction<T, T, T> merge);
        public abstract void deduplicateAndGetFreeCell(final Lock lock, final T element);
    }

    public class ArrPage extends Page {
        public final T[] arr;

        public ArrPage(final T[] arr) {
            this.arr = arr;
        }

        @Override public int length() { return arr.length; }

        @Override
        public void contains(final Lock lock, final T element) {
            if (arr.length > stepSize) {
                for (int o = 0; o < arr.length; o += stepSize) {
                    final int start = o;
                    lock.spread(() -> {
                        final int end = Math.min(start + stepSize, arr.length);
                        for (int i = start; i < end; i++) {
                            final T e = arr[i];
                            if (e == null)
                                continue;
                            if (e.equals(element)) {
                                lock.result = true;
                                return;
                            }
                        }
                    });
                }
                return;
            }
            lock.spread(() -> {
                for (final T e2 : arr) {
                    if (e2 == null)
                        continue;
                    if (e2.equals(element)) {
                        lock.result = true;
                        return;
                    }
                }
            });
        }

        @Override
        public T get(final int index) {
            return arr[index];
        }

        @Override
        public void findFreeCell(final Lock lock) {
            for (int o = 0; o < arr.length; o += stepSize) {
                final int start = o;
                lock.spread(() -> {
                    final int end = Math.min(start + stepSize, arr.length);
                    for (int i = start; i < end; i++)
                        if (arr[i] == null) {
                            lock.result = new ArrPointer(this, i);
                            return;
                        }
                });
            }
        }

        @Override
        public void deduplicate(final Lock lock, final T[] elements) {
            if (elements.length > stepSize) {
                for (int o = 0; o < arr.length; o += stepSize) {
                    final int start = o;
                    for (int o2 = 0; o2 < elements.length; o2 += stepSize) {
                        final int start2 = o2;
                        lock.spread(() -> {
                            final int end = Math.min(start + stepSize, arr.length),
                                    end2 = Math.min(start2 + stepSize, elements.length);
                            for (int i = start; i < end; i++) {
                                final T e1 = arr[i];
                                if (e1 == null)
                                    continue;
                                for (int i2 = start2; i2 < end2; i2++) {
                                    final T e2 = elements[i2];
                                    if (e2 == null)
                                        continue;
                                    if (e1.equals(e2)) {
                                        arr[i] = null;
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
                return;
            }
            for (int o = 0; o < arr.length; o += stepSize) {
                final int start = o;
                lock.spread(() -> {
                    final int end = Math.min(start + stepSize, arr.length);
                    for (int i = start; i < end; i++) {
                        final T e1 = arr[i];
                        if (e1 == null)
                            continue;
                        for (final T e2 : elements) {
                            if (e2 == null)
                                continue;
                            if (e1.equals(e2)) {
                                arr[i] = null;
                                break;
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void deduplicate(final Lock lock, final T[] elements, final BiFunction<T, T, Boolean> eq, final BiFunction<T, T, T> merge) {
            if (elements.length > stepSize) {
                for (int o = 0; o < arr.length; o += stepSize) {
                    final int start = o;
                    for (int o2 = 0; o2 < elements.length; o2 += stepSize) {
                        final int start2 = o2;
                        lock.spread(() -> {
                            final int end = Math.min(start + stepSize, arr.length),
                                    end2 = Math.min(start2 + stepSize, elements.length);
                            for (int i = start; i < end; i++) {
                                final T e1 = arr[i];
                                if (e1 == null)
                                    continue;
                                for (int i2 = start2; i2 < end2; i2++) {
                                    final T e2 = elements[i2];
                                    if (e2 == null)
                                        continue;
                                    if (eq.apply(e1, e2)) {
                                        elements[i2] = merge.apply(e1, e2);
                                        arr[i] = null;
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
                return;
            }
            for (int o = 0; o < arr.length; o += stepSize) {
                final int start = o;
                lock.spread(() -> {
                    final int end = Math.min(start + stepSize, arr.length);
                    for (int i = start; i < end; i++) {
                        final T e1 = arr[i];
                        if (e1 == null)
                            continue;
                        int j = -1;
                        for (final T e2 : elements) {
                            j++;
                            if (e2 == null)
                                continue;
                            if (eq.apply(e1, e2)) {
                                elements[j] = merge.apply(e1, e2);
                                arr[i] = null;
                                break;
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void deduplicate(final Lock lock, final T element) {
            for (int o = 0; o < arr.length; o += stepSize) {
                final int start = o;
                lock.spread(() -> {
                    final int end = Math.min(start + stepSize, arr.length);
                    for (int i = start; i < end; i++) {
                        final T e1 = arr[i];
                        if (e1 == null)
                            continue;
                        if (e1.equals(element)) {
                            arr[i] = null;
                            lock.result = true;
                            return;
                        }
                    }
                });
            }
        }

        @Override
        public void deduplicate(final Lock lock, final Page page, final BiFunction<T, T, Boolean> eq, final BiFunction<T, T, T> merge) {
            if (page == this) {
                lock.spread(() -> {
                    f:
                    for (int i = 0; i < arr.length; i++) {
                        final T e1 = arr[i];
                        if (e1 == null)
                            continue;
                        for (int j = i + 1; j < arr.length; j++) {
                            final T e2 = arr[j];
                            if (e2 == null)
                                continue;
                            if (eq.apply(e1, e2)) {
                                arr[j] = merge.apply(e1, e2);
                                arr[i] = null;
                                continue f;
                            }
                        }
                    }
                });
                return;
            }
            if (page instanceof ArrArrList.ArrPage) {
                deduplicate(lock, ((ArrPage) page).arr, eq, merge);
                return;
            }
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void deduplicate(final Lock lock, final Page page) {
            if (page == this) {
                lock.spread(() -> {
                    f:
                    for (int i = 0; i < arr.length; i++) {
                        final T e1 = arr[i];
                        if (e1 == null)
                            continue;
                        for (int j = i + 1; j < arr.length; j++) {
                            final T e2 = arr[j];
                            if (e2 == null)
                                continue;
                            if (e1.equals(e2)) {
                                arr[i] = null;
                                continue f;
                            }
                        }
                    }
                });
                return;
            }
            if (page instanceof ArrArrList.ArrPage) {
                deduplicate(lock, ((ArrPage) page).arr);
                return;
            }
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void deduplicateAndGetFreeCell(final Lock lock, final T element) {
            for (int o = 0; o < arr.length; o += stepSize) {
                final int start = o;
                lock.spread(() -> {
                    final int end = Math.min(start + stepSize, arr.length);
                    for (int i = start; i < end; i++) {
                        final T e1 = arr[i];
                        if (e1 == null) {
                            if (lock.resultBackup == null)
                                lock.resultBackup = new ArrPointer(this, i);
                            continue;
                        }
                        if (e1.equals(element)) {
                            lock.result = new ArrPointer(this, i);
                            return;
                        }
                    }
                });
            }
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                public boolean f = true;
                public int i = -1;

                private void find() {
                    for (++i; i < arr.length; i++)
                        if (arr[i] != null)
                            break;
                }

                @Override
                public boolean hasNext() {
                    if (f) {
                        find();
                        f = false;
                    }
                    return i < arr.length;
                }

                @Override
                public T next() {
                    if (f)
                        find();
                    f = true;
                    return arr[i];
                }
            };
        }
    }

    private final Class<T> cls;

    public ArrArrList(final ExecutorService executorService, final int stepSize, final Class<T> cls) {
        this.executorService = executorService;
        this.stepSize = stepSize;
        this.cls = cls;
    }

    public boolean contains(final T element) throws InterruptedException {
        final Lock l = new Lock();
        l.result = false;
        for (final Page p : list)
            p.contains(l, element);
        return (boolean) l.join();
    }

    public ArrPage add(final T[] arr) {
        final ArrPage p = new ArrPage(arr);
        list.offer(p);
        return p;
    }

    public void add(final T element) throws InterruptedException {
        final Lock lock = new Lock();
        for (final Page p : list)
            p.findFreeCell(lock);
        final Object r = lock.join();
        if (r instanceof ArrArrList.ArrPointer) {
            @SuppressWarnings("unchecked")
            final ArrArrList<T>.ArrPointer arrPointer = (ArrArrList<T>.ArrPointer) r;
            arrPointer.page.arr[arrPointer.index] = element;
            return;
        }
        @SuppressWarnings("unchecked")
        final ArrPage page = new ArrPage((T[]) Array.newInstance(cls, 1024));
        page.arr[0] = element;
        list.offer(page);
    }

    public void addDeduplicate(final T[] arr) throws InterruptedException {
        final Lock l = new Lock();
        for (final Page p : list)
            p.deduplicate(l, arr);
        list.offer(new ArrPage(arr));
        l.join();
    }

    public void addDeduplicate(final T element) throws InterruptedException {
        final Lock lock = new Lock();
        for (final Page p : list)
            p.deduplicateAndGetFreeCell(lock, element);
        final Object r = lock.join();
        if (r instanceof ArrArrList.ArrPointer) {
            @SuppressWarnings("unchecked")
            final ArrArrList<T>.ArrPointer arrPointer = (ArrArrList<T>.ArrPointer) r;
            arrPointer.page.arr[arrPointer.index] = element;
            return;
        }
        @SuppressWarnings("unchecked")
        final ArrPage page = new ArrPage((T[]) Array.newInstance(cls, 1024));
        page.arr[0] = element;
        list.offer(page);
    }

    public void deduplicate(final BiFunction<T, T, Boolean> eq, final BiFunction<T, T, T> merge) throws InterruptedException {
        final Lock lock = new Lock();
        Lock l = new Lock(lock);
        for (final Page p : list)
            p.deduplicate(l, p, eq, merge);
        l.join();
        int i = 0;
        for (final Page p : list) {
            l = new Lock(lock);
            final Iterator<Page> pi2 = list.iterator();
            Iter.skip(pi2, ++i);
            while (pi2.hasNext()) {
                final Page p2 = pi2.next();
                if (p != p2)
                    p2.deduplicate(l, p, eq, merge);
            }
            l.join();
        }
        lock.join();
    }

    public void deduplicate() throws InterruptedException {
        final Lock lock = new Lock();
        Lock l = new Lock(lock);
        for (final Page p : list)
            p.deduplicate(l, p);
        l.join();
        int i = 0;
        for (final Page p : list) {
            l = new Lock(lock);
            final Iterator<Page> pi2 = list.iterator();
            Iter.skip(pi2, ++i);
            while (pi2.hasNext()) {
                final Page p2 = pi2.next();
                if (p != p2)
                    p2.deduplicate(l, p);
            }
            l.join();
        }
        lock.join();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return new IteratorIterable<>(list.iterator());
    }
}