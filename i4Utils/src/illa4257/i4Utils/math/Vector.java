package illa4257.i4Utils.math;

import illa4257.i4Utils.str.Str;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class Vector implements Cloneable {
    public final AtomicIntegerArray array;

    public Vector(final float... values) {
        array = new AtomicIntegerArray(values.length);
        for (int i = 0; i < values.length; i++)
            array.set(i, Float.floatToIntBits(values[i]));
    }

    public Vector(final Vector vector) {
        final int l;
        array = new AtomicIntegerArray(l = vector.array.length());
        for (int i = 0; i < l; i++)
            array.set(i, vector.array.get(i));
    }

    public Vector set(final int index, final float value) { array.set(index, Float.floatToIntBits(value)); return this; }

    public Vector set(final float... values) {
        final int l = Math.min(values.length, array.length());
        for (int i = 0; i < l; i++)
            array.set(i, Float.floatToIntBits(values[i]));
        return this;
    }

    public Vector set(final Vector vector) {
        final int l = Math.min(vector.array.length(), array.length());
        for (int i = 0; i < l; i++)
            array.set(i, vector.array.get(i));
        return this;
    }

    public Vector add(final int index, final float delta) {
        while (true) {
            final int old = array.get(index);
            if (array.compareAndSet(index, old, Float.floatToIntBits(Float.intBitsToFloat(old) + delta)))
                return this;
        }
    }

    public Vector add(final float... values) {
        final int l = Math.min(values.length, array.length());
        for (int i = 0; i < l; i++)
            while (true) {
                final int old = array.get(i);
                if (array.compareAndSet(i, old, Float.floatToIntBits(Float.intBitsToFloat(old) + values[i])))
                    break;
            }
        return this;
    }

    public Vector add(final Vector vector) {
        final int l = Math.min(vector.array.length(), array.length());
        for (int i = 0; i < l; i++)
            while (true) {
                final int old = array.get(i);
                if (array.compareAndSet(i, old, Float.floatToIntBits(Float.intBitsToFloat(old) + Float.intBitsToFloat(vector.array.get(i)))))
                    break;
            }
        return this;
    }

    public Vector subtract(final int index, final float delta) {
        while (true) {
            final int old = array.get(index);
            if (array.compareAndSet(index, old, Float.floatToIntBits(Float.intBitsToFloat(old) - delta)))
                return this;
        }
    }

    public Vector subtract(final float... values) {
        final int l = Math.min(values.length, array.length());
        for (int i = 0; i < l; i++)
            while (true) {
                final int old = array.get(i);
                if (array.compareAndSet(i, old, Float.floatToIntBits(Float.intBitsToFloat(old) - values[i])))
                    break;
            }
        return this;
    }

    public Vector subtract(final Vector vector) {
        final int l = Math.min(vector.array.length(), array.length());
        for (int i = 0; i < l; i++)
            while (true) {
                final int old = array.get(i);
                if (array.compareAndSet(i, old, Float.floatToIntBits(Float.intBitsToFloat(old) - Float.intBitsToFloat(vector.array.get(i)))))
                    break;
            }
        return this;
    }

    public float distance(final boolean padThisWithZeroes, final boolean padTargetWithZeroes, final float... values) {
        return (float) Math.sqrt(distanceSquared(padThisWithZeroes, padTargetWithZeroes, values));
    }

    public float distance(final boolean padThisWithZeroes, final boolean padTargetWithZeroes, final Vector vector) {
        return (float) Math.sqrt(distanceSquared(padThisWithZeroes, padTargetWithZeroes, vector));
    }

    public float distanceSquared(final boolean padThisWithZeroes, final boolean padTargetWithZeroes, final float... values) {
        float v = 0;
        int i = 0;
        final int l1 = array.length(), l2 = Math.min(values.length, l1);
        for (; i < l2; i++) {
            final float d = Float.intBitsToFloat(array.get(i)) - values[i];
            v += d * d;
        }
        if (padThisWithZeroes)
            for (; i < l1; i++) {
                final float d = Float.intBitsToFloat(array.get(i));
                v += d * d;
            }
        if (padTargetWithZeroes)
            for (; i < values.length; i++) {
                final float d = values[i];
                v += d * d;
            }
        return v;
    }

    public float distanceSquared(final boolean padThisWithZeroes, final boolean padTargetWithZeroes, final Vector vector) {
        float v = 0;
        int i = 0;
        final int l1 = array.length(), l3 = vector.array.length(), l2 = Math.min(l3, l1);
        for (; i < l2; i++) {
            final float d = Float.intBitsToFloat(array.get(i)) - Float.intBitsToFloat(vector.array.get(i));
            v += d * d;
        }
        if (padThisWithZeroes)
            for (; i < l1; i++) {
                final float d = Float.intBitsToFloat(array.get(i));
                v += d * d;
            }
        if (padTargetWithZeroes)
            for (; i < l3; i++) {
                final float d = Float.intBitsToFloat(vector.array.get(i));
                v += d * d;
            }
        return v;
    }

    public Vector copy() { return new Vector(this); }
    @SuppressWarnings("MethodDoesntCallSuperMethod") @Override public Vector clone() { return copy(); }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Vector))
            return super.equals(obj);
        final Vector vector = (Vector) obj;
        final int l;
        if (vector.array.length() != (l = array.length()))
            return false;
        for (int i = 0; i < l; i++)
            if (vector.array.get(i) != array.get(i))
                return false;
        return true;
    }

    @Override public String toString() {
        final StringBuilder b = Str.builder().append("Vector(");
        try {
            final int l = array.length();
            if (l > 0) {
                b.append(array.get(0));
                for (int i = 1; i < l; i++)
                    b.append(", ").append(array.get(i));
            }
            return b.append(')').toString();
        } finally {
            Str.recycle(b);
        }
    }
}