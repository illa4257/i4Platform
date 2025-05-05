package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Intent;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.IFileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Utils.runnables.Consumer2;

import java.io.File;
import java.util.Iterator;

public class AndroidFileChooser implements IFileChooser {
    public final AndroidFramework framework;
    private volatile Window parent = null;

    public AndroidFileChooser(final AndroidFramework framework) {
        this.framework = framework;
    }

    @Override public void setParent(final Window parent) { this.parent = parent; }

    @Override
    public void setOpen(boolean open) {

    }

    @Override
    public void setMultiSelectionEnabled(boolean allow) {

    }

    @Override
    public void setTitle(String title) {

    }

    @Override
    public void setDefaultExt(String extension) {

    }

    @Override
    public void setFilter(FileChooserFilter filters) {

    }

    @Override
    public void setInitialDir(File dir) {

    }

    @Override
    public void setCurrentDir(File dir) {

    }

    @Override
    public void setOnFinish(Consumer2<IFileChooser, Boolean> listener) {

    }

    @Override
    public void start() {
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

    @Override
    public Iterator<File> iterator() {
        return null;
    }
}
