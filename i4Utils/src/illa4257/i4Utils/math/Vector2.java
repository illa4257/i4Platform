package illa4257.i4Utils.math;

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class Vector2 implements Cloneable {
    public static final Vector2 ZERO = new Vector2(0, 0);

    public volatile float x, y;

    public Vector2(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(final Vector2 vector) {
        x = vector.x;
        y = vector.y;
    }

    public Vector2 set(final float x, final float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2 set(final Vector2 vector) {
        x = vector.x;
        y = vector.y;
        return this;
    }

    public Vector2 add(final float x, final float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Vector2 add(final Vector2 vector) {
        x += vector.x;
        y += vector.y;
        return this;
    }

    public Vector2 subtract(final float x, final float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2 subtract(final Vector2 vector) {
        x -= vector.x;
        y -= vector.y;
        return this;
    }

    public float distance(final float x, final float y) {
        final float dx = this.x - x, dy = this.y - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float distance(final Vector2 vector) {
        final float dx = x - vector.x, dy = y - vector.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float distanceSquared(final float x, final float y) {
        final float dx = this.x - x, dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public float distanceSquared(final Vector2 vector) {
        final float dx = x - vector.x, dy = y - vector.y;
        return dx * dx + dy * dy;
    }

    public Vector2 copy() { return new Vector2(this); }

    @Override
    public Vector2 clone() {
        try {
            return (Vector2) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Vector2))
            return super.equals(obj);
        final Vector2 vector = (Vector2) obj;
        return x == vector.x && y == vector.y;
    }

    @Override public String toString() { return "Vector2(" + x + ", " + y + ")"; }
}