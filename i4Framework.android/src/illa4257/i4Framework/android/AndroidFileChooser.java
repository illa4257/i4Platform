package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Intent;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.components.Window;

import java.io.File;
import java.util.Iterator;
import java.util.function.Consumer;

public class AndroidFileChooser implements FileChooser {
    public final AndroidFramework framework;
    private volatile Window parent = null;

    public AndroidFileChooser(final AndroidFramework framework) { this.framework = framework; }

    @Override public AndroidFileChooser setParent(final Window parent) { this.parent = parent; return this; }

    @Override
    public AndroidFileChooser setOpen(boolean open) {
        return this;
    }

    @Override
    public AndroidFileChooser setMultiSelectionEnabled(boolean allow) {
        return this;
    }

    @Override
    public AndroidFileChooser setTitle(String title) {
        return this;
    }

    @Override
    public AndroidFileChooser setDefaultExt(String extension) {
        return this;
    }

    @Override
    public AndroidFileChooser setFilter(FileChooserFilter filters) {
        return this;
    }

    @Override
    public AndroidFileChooser setInitialDir(File dir) {
        return this;
    }

    @Override
    public AndroidFileChooser setCurrentDir(File dir) {
        return this;
    }

    @Override
    public void startThen(final Consumer<Boolean> listener) {
        final Window w = parent;
        if (w == null)
            return;
        final FrameworkWindow fw = w.frameworkWindow.get();
        if (!(fw instanceof AndroidWindow))
            return;
        final Activity a = ((AndroidWindow) fw).activity.get();
        if (a == null)
            return;
        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        a.startActivityForResult(intent, 2);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<File> iterator() {
        return null;
    }
}
