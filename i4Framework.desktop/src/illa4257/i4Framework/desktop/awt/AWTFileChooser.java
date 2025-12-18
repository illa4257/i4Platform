package illa4257.i4Framework.desktop.awt;

import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Utils.lists.ArrIterator;

import java.awt.*;
import java.io.File;
import java.util.Iterator;
import java.util.function.BiConsumer;

public class AWTFileChooser implements FileChooser {
    private final Object lock = new Object();
    public volatile FileDialog dialog = new FileDialog((Frame) null);
    public volatile BiConsumer<FileChooser, Boolean> listener = null;

    @Override
    public void setOpen(final boolean open) {
        dialog.setMode(open ? FileDialog.LOAD : FileDialog.SAVE);
    }

    @Override
    public void setMultiSelectionEnabled(final boolean allow) {
        dialog.setMultipleMode(allow);
    }

    @Override
    public void setTitle(final String title) {
        dialog.setTitle(title);
    }

    @Override
    public void setDefaultExt(final String extension) {
        if (extension == null || extension.isEmpty())
            return;
        dialog.setFile(extension.startsWith("*") ? extension : extension.startsWith(".") ? "*" + extension : "*." + extension);
    }

    @Override
    public void setFilter(final FileChooserFilter filters) {
        dialog.setFilenameFilter((dir, name) -> filters.check(name));
    }

    @Override
    public void setParent(final Window parent) {
        synchronized (lock) {
            final FileDialog d = dialog, n;
            final FrameworkWindow fw = parent != null ? parent.frameworkWindow.get() : null;
            if (fw == null && d.getOwner() == null)
                return;
            if (fw instanceof Frame)
                n = new FileDialog((Frame) fw, d.getTitle());
            else if (d.getOwner() == null)
                return;
            else
                n = new FileDialog((Frame) null, d.getTitle());
            n.setMode(d.getMode());
            n.setMultipleMode(d.isMultipleMode());
            n.setFile(d.getFile());
            n.setDirectory(d.getDirectory());
            n.setFilenameFilter(d.getFilenameFilter());
            dialog = n;
        }
    }

    @Override
    public void setInitialDir(final File dir) {
        final FileDialog f = dialog;
        final String d = f.getDirectory();
        if (d == null || d.isEmpty())
            return;
        f.setDirectory(dir != null ? dir.getAbsolutePath() : null);
    }

    @Override
    public void setCurrentDir(final File dir) {
        dialog.setDirectory(dir != null ? dir.getAbsolutePath() : null);
    }

    @Override
    public void setOnFinish(final BiConsumer<FileChooser, Boolean> listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        final FileDialog f = dialog;
        f.setVisible(true);
        final BiConsumer<FileChooser, Boolean> l = listener;
        if (l != null)
            l.accept(this, f.getFiles().length != 0);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<File> iterator() {
        return new ArrIterator<>(dialog.getFiles());
    }
}
