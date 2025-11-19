package illa4257.i4Utils.math;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class Vector3 implements Cloneable {
    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public volatile float x, y, z;

    public Vector3(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(final Vector3 vector) {
        x = vector.x;
        y = vector.y;
        z = vector.z;
    }

    public Vector3 set(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 set(final Vector3 vector) {
        x = vector.x;
        y = vector.y;
        z = vector.z;
        return this;
    }

    public Vector3 add(final float x, final float y, final float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3 add(final Vector3 vector) {
        x += vector.x;
        y += vector.y;
        z += vector.z;
        return this;
    }

    public Vector3 subtract(final float x, final float y, final float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3 subtract(final Vector3 vector) {
        x -= vector.x;
        y -= vector.y;
        z -= vector.z;
        return this;
    }

    public float distance(final float x, final float y, final float z) {
        final float dx = this.x - x, dy = this.y - y, dz = this.z - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distance(final Vector3 vector) {
        final float dx = x - vector.x, dy = y - vector.y, dz = z - vector.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float distanceSquared(final float x, final float y, final float z) {
        final float dx = this.x - x, dy = this.y - y, dz = this.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float distanceSquared(final Vector3 vector) {
        final float dx = x - vector.x, dy = y - vector.y, dz = z - vector.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vector3 copy() { return new Vector3(this); }

    @Override
    public Vector3 clone() {
        try {
            return (Vector3) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Vector3))
            return super.equals(obj);
        final Vector3 vector = (Vector3) obj;
        return x == vector.x && y == vector.y && z == vector.z;
    }

    @Override public String toString() { return "Vector3(" + x + ", " + y + ", " + z + ")"; }
}