package illa4257.i4Framework.desktop.awt;

import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Utils.lists.ArrIterator;

import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.function.Consumer;

public class AWTFileChooser implements FileChooser {
    private final Object lock = new Object();
    public volatile FileDialog dialog = new FileDialog((Frame) null);
    public volatile Consumer<Boolean> listener = null;

    @Override
    public FileChooser setOpen(final boolean open) {
        dialog.setMode(open ? FileDialog.LOAD : FileDialog.SAVE);
        return this;
    }

    @Override
    public FileChooser setMultiSelectionEnabled(final boolean allow) {
        dialog.setMultipleMode(allow);
        return this;
    }

    @Override
    public FileChooser setTitle(final String title) {
        dialog.setTitle(title);
        return this;
    }

    @Override
    public FileChooser setDefaultExt(final String extension) {
        if (extension != null && !extension.isEmpty())
            dialog.setFile(extension.startsWith("*") ? extension : extension.startsWith(".") ? "*" + extension : "*." + extension);
        return this;
    }

    @Override
    public FileChooser setFilter(final FileChooserFilter filters) {
        dialog.setFilenameFilter((dir, name) -> filters.check(name));
        return this;
    }

    @Override
    public FileChooser setParent(final Window parent) {
        synchronized (lock) {
            final FileDialog d = dialog, n;
            final FrameworkWindow fw = parent != null ? parent.frameworkWindow.get() : null;
            if (fw == null && d.getOwner() == null)
                return this;
            if (fw instanceof Frame)
                n = new FileDialog((Frame) fw, d.getTitle());
            else if (d.getOwner() == null)
                return this;
            else
                n = new FileDialog((Frame) null, d.getTitle());
            n.setMode(d.getMode());
            n.setMultipleMode(d.isMultipleMode());
            n.setFile(d.getFile());
            n.setDirectory(d.getDirectory());
            n.setFilenameFilter(d.getFilenameFilter());
            dialog = n;
        }
        return this;
    }

    @Override
    public FileChooser setInitialDir(final File dir) {
        final FileDialog f = dialog;
        final String d = f.getDirectory();
        if (d != null && !d.isEmpty())
            f.setDirectory(dir != null ? dir.getAbsolutePath() : null);
        return this;
    }

    @Override
    public FileChooser setCurrentDir(final File dir) {
        dialog.setDirectory(dir != null ? dir.getAbsolutePath() : null);
        return this;
    }

    @Override
    public void startThen(final Consumer<Boolean> listener) {
        final FileDialog f = dialog;
        f.setVisible(true);
        f.dispose();
        if (listener != null)
            listener.accept(f.getFiles().length != 0);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<File> iterator() {
        return new ArrIterator<>(dialog.getFiles());
    }
}
