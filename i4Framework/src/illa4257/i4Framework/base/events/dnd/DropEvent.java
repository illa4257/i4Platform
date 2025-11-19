package illa4257.i4Framework.base.events.dnd;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.IMoveableInputEvent;

import java.io.File;
import java.util.List;

public class DropEvent extends Event implements IMoveableInputEvent {
    public final List<File> files;

    public DropEvent(final boolean isSystem, final int pointerId, final float globalX, final float globalY,
                     final float x, final float y, final List<File> files) {
        super(isSystem, pointerId, globalX, globalY, x, y);
        this.files = files;
    }
}