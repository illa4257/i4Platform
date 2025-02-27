package illa4257.i4Utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface QueueObserver<T> {
    void onInit(final Queue<T> queue);

    void onAdd(final T element);

    void onRemove(final T element);

    class ObservedQueue<T> {
        public final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
        public final ConcurrentLinkedQueue<QueueObserver<T>> observers = new ConcurrentLinkedQueue<>(),
                newObservers = new ConcurrentLinkedQueue<>();
        public final ConcurrentLinkedQueue<Object> actions = new ConcurrentLinkedQueue<>();
        public final ConcurrentLinkedQueue<Runnable> triggers = new ConcurrentLinkedQueue<>();

        private static class Add<T> {
            public final T element;

            private Add(final T element) { this.element = element; }
        }

        private static class Remove<T> {
            public final T element;

            private Remove(final T element) { this.element = element; }
        }

        @SuppressWarnings("unchecked")
        public void tick() {
            for (Object t = actions.poll(); t != null; t = actions.poll())
                if (t instanceof ObservedQueue.Add)
                    for (final QueueObserver<T> o : observers)
                        o.onAdd(((ObservedQueue.Add<T>) t).element);
                else if (t instanceof ObservedQueue.Remove)
                    for (final QueueObserver<T> o : observers)
                        o.onRemove(((ObservedQueue.Remove<T>) t).element);
            for (QueueObserver<T> o = newObservers.poll(); o != null; o = newObservers.poll())
                o.onInit(queue);
        }

        public void addObserver(final QueueObserver<T> observer) {
            if (observer == null)
                return;
            newObservers.add(observer);
        }

        public void removeObserver(final QueueObserver<T> observer) {
            if (observer == null)
                return;
            newObservers.remove(observer);
            observers.remove(observer);
        }

        public void add(final T element) {
            if (element == null)
                return;
            if (queue.add(element) && actions.add(new ObservedQueue.Add<>(element)))
                for (final Runnable r : triggers)
                    r.run();
        }

        public void remove(final T element) {
            if (queue.remove(element) && actions.add(new ObservedQueue.Remove<>(element)))
                for (final Runnable r : triggers)
                    r.run();
        }
    }
}
