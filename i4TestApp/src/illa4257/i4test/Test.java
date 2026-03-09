package illa4257.i4test;

public class Test {
    public static void main(final String[] args) {
        System.out.println(Thread.currentThread().getThreadGroup());

        for (final Thread t : Thread.getAllStackTraces().keySet())
            System.out.println(t.getName() + " | " + t.getThreadGroup());
    }
}