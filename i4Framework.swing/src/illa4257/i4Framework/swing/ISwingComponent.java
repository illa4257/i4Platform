package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.dnd.DroppedEvent;
import illa4257.i4Framework.base.res.FileRes;
import illa4257.i4Framework.base.res.Res;
import illa4257.i4Utils.logger.i4Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface ISwingComponent {
    Component getComponent();
    void dispose();

    static SwingComponent find(final java.awt.Container container, final Component c) {
        for (final java.awt.Component co : container.getComponents())
            if (co instanceof SwingComponent) {
                if (((SwingComponent) co).getComponent() == c)
                    return (SwingComponent) co;
                final SwingComponent r = find((SwingComponent) co, c);
                if (r != null)
                    return r;
            }
        return null;
    }

    static SwingComponent getComponent(final java.awt.Container t, final Component c) {
        for (final java.awt.Component co : t.getComponents())
            if (co instanceof SwingComponent && ((SwingComponent) co).getComponent() == c)
                return (SwingComponent) co;
        return null;
    }

    static DropTarget wrapDropTarget(final Component component) {
        return new DropTarget() {
            @Override
            public synchronized void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
                super.dragEnter(dropTargetDragEvent);
            }

            @Override
            public synchronized void dragOver(DropTargetDragEvent dropTargetDragEvent) {
                super.dragOver(dropTargetDragEvent);
            }

            @Override
            public synchronized void dropActionChanged(DropTargetDragEvent dropTargetDragEvent) {
                super.dropActionChanged(dropTargetDragEvent);
            }

            @Override
            public synchronized void dragExit(DropTargetEvent dropTargetEvent) {
                super.dragExit(dropTargetEvent);
            }

            @Override
            public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
                try {
                    dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY);
                    final ArrayList<Res> l = new ArrayList<>();
                    final Transferable transferable = dropTargetDropEvent.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                        //noinspection unchecked
                        for (final File f : (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor))
                            l.add(new FileRes(f));
                    component.fire(new DroppedEvent(component, true, 0, dropTargetDropEvent.getLocation().x, dropTargetDropEvent.getLocation().y, dropTargetDropEvent.getLocation().x, dropTargetDropEvent.getLocation().y, l));
                    dropTargetDropEvent.dropComplete(true);
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.e(ex);
                    dropTargetDropEvent.dropComplete(false);
                }
            }
        };
    }
}