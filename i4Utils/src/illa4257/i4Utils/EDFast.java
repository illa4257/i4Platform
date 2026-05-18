package illa4257.i4Utils;

import java.io.Closeable;
import java.util.Arrays;

public class EDFast implements Closeable {
    private final byte[] key;
    private int index = 0;

    public EDFast(final byte[] k) {
        this.key = k;
    }

    public void reset() {
        index = 0;
    }

    private byte next() {
        final byte n = key[index++];
        if (index == key.length)
            index = 0;
        return n;
    }

    public void encrypt(final byte[] data) {
        for (int i = 0; i < data.length; i++) {
            switch (next() & 3) {
                case 0: {
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 3) | (r >>> 5));
                    data[i] ^= next();
                    data[i] ^= next();
                    data[i] += next();
                    break;
                }
                case 1: {
                    data[i] ^= next();
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 2) | (r >>> 6));
                    data[i] ^= next();
                    data[i] -= next();
                    break;
                }
                case 2: {
                    data[i] -= next();
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 1) | (r >>> 7));
                    data[i] ^= next();
                    data[i] += next();
                    break;
                }
                case 3: {
                    data[i] -= next();
                    data[i] ^= next();
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 5) | (r >>> 3));
                    data[i] -= next();
                    break;
                }
            }
            data[i] ^= next();
            final int r = data[i] & 0xFF;
            data[i] = (byte) ((r << 5) | (r >>> 3));
            data[i] -= next();
        }
    }

    public void decrypt(final byte[] data) {
        for (int i = 0; i < data.length; i++) {
            final byte n = next(), b3 = next(), b2 = next(), b1 = next(), be = next();
            data[i] += next();
            int r1 = data[i] & 0xFF;
            data[i] = (byte) ((r1 << 3) | (r1 >>> 5));
            data[i] ^= be;
            switch (n & 3) {
                case 0: {
                    data[i] -= b1;
                    data[i] ^= b2;
                    data[i] ^= b3;
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 5) | (r >>> 3));
                    break;
                }
                case 1: {
                    data[i] += b1;
                    data[i] ^= b2;
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 6) | (r >>> 2));
                    data[i] ^= b3;
                    break;
                }
                case 2: {
                    data[i] -= b1;
                    data[i] ^= b2;
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 7) | (r >>> 1));
                    data[i] += b3;
                    break;
                }
                case 3: {
                    data[i] += b1;
                    final int r = data[i] & 0xFF;
                    data[i] = (byte) ((r << 3) | (r >>> 5));
                    data[i] ^= b2;
                    data[i] += b3;
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        Arrays.fill(key, (byte) 0);
    }
}