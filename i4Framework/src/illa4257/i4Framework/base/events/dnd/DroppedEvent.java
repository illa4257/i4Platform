package illa4257.i4Framework.base.events.dnd;

import java.io.File;
import java.util.List;

public class DroppedEvent extends DropEvent {
    public DroppedEvent(final boolean isSystem, final int pointerId, final float globalX, final float globalY,
                        final float x, final float y, final List<File> files) {
        super(isSystem, pointerId, globalX, globalY, x, y, files);
    }
}