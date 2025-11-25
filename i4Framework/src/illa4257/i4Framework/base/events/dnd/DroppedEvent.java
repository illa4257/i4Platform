package illa4257.i4Framework.base.events.dnd;

import illa4257.i4Framework.base.components.Component;

import java.io.File;
import java.util.List;

public class DroppedEvent extends DropEvent {
    public DroppedEvent(final Component component, final boolean isSystem, final int pointerId, final float globalX, final float globalY,
                        final float x, final float y, final List<File> files) {
        super(component, isSystem, pointerId, globalX, globalY, x, y, files);
    }
}