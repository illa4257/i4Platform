package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;
import illa4257.i4Framework.base.events.dnd.DroppedEvent;
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

    static SwingComponent getComponent(final java.awt.Container t, final Component c) {
        for (final java.awt.Component co : t.getComponents())
            if (co instanceof SwingComponent && ((SwingComponent) co).getComponent() == c)
                return (SwingComponent) co;
        return null;
    }

    @SuppressWarnings("rawtypes")
    static EventListener[] registerContainer(final java.awt.Container t, Container c) {
        return new EventListener[] {
            c.addEventListener(AddComponentEvent.class, e -> {
                if (e.container != c || getComponent(t, e.child) != null)
                    return;
                final SwingComponent co = new SwingComponent(e.child);
                t.add(co, 0);
                co.repaint();
            }),
            c.addEventListener(RemoveComponentEvent.class, e -> {
                if (e.container != c)
                    return;
                final SwingComponent co = getComponent(t, e.child);
                if (co == null)
                    return;
                t.remove(co);
                co.dispose();
            })
        };
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
                    final ArrayList<File> l = new ArrayList<>();
                    final Transferable transferable = dropTargetDropEvent.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                        //noinspection unchecked
                        l.addAll((List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor));
                    component.fire(new DroppedEvent(true, 0, dropTargetDropEvent.getLocation().x, dropTargetDropEvent.getLocation().y, dropTargetDropEvent.getLocation().x, dropTargetDropEvent.getLocation().y, l));
                    dropTargetDropEvent.dropComplete(true);
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.e(ex);
                    dropTargetDropEvent.dropComplete(false);
                }
            }
        };
    }
}