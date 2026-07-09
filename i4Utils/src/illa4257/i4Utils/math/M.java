package illa4257.i4Utils.math;

public class M {
    public static long ceilDiv(long x, long y) {
        final long q = x / y;
        return (x ^ y) >= 0 && (q * y != x) ? q + 1 : q;
    }

    public static long ceilDiv(long x, int y) {
        return ceilDiv(x, (long)y);
    }
}