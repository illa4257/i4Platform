package illa4257.i4Utils.lists;

import illa4257.i4Utils.logger.i4Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

public class ArrArrList<T> {
    public final ExecutorService executorService;
    public final ConcurrentLinkedQueue<Page> list = new ConcurrentLinkedQueue<>();
    private final Object l = new Object();
    private volatile Lock lock = null;
    private final int stepSize;

    public class Lock {
        private final Object lo = new Object();
        private int nTasks = 0;
        public volatile Object result = null;

        private Lock() throws InterruptedException {
            synchronized (l) {
                while (lock != null)
                    l.wait();
                lock = this;
            }
        }

        public void spread(final Runnable runnable) {
            synchronized (lo) { nTasks++; }
            final Object o = result;
            executorService.submit(() -> {
                if (result == o)
                    try {
                        runnable.run();
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
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
                lock = null;
                l.notify();
            }
            return result;
        }
    }

    public abstract class Page {
        public abstract void contains(final Lock lock, final T element);
        public abstract void deduplicate(final Lock lock, final T[] elements);
    }

    public class ArrPage extends Page {
        public final T[] arr;

        public ArrPage(final T[] arr) {
            this.arr = arr;
        }

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
    }

    public ArrArrList(final ExecutorService executorService, final int stepSize) {
        this.executorService = executorService;
        this.stepSize = stepSize;
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

    public void addDeduplicate(final T[] arr) throws InterruptedException {
        final Lock l = new Lock();
        for (final Page p : list)
            p.deduplicate(l, arr);
        list.offer(new ArrPage(arr));
        l.join();
    }
}