package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeZ implements SingleEvent {
    public boolean isPrevented = false, isParentPrevented = true;
    public final int z;

    public ChangeZ(final int z) { this.z = z; }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override public boolean isPrevented() { return isPrevented; }
    @Override public boolean isParentPrevented() { return isParentPrevented || isPrevented; }

    public ChangeZ prevent(final boolean prevent) { isPrevented = prevent; return this; }
    public ChangeZ parentPrevent(final boolean prevent) { isParentPrevented = prevent; return this; }
}