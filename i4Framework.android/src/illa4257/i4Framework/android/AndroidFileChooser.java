package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Intent;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.res.Res;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class AndroidFileChooser implements FileChooser {
    public final AndroidFramework framework;
    private final Intent intent = new Intent();

    private volatile Window parent = null;

    private volatile List<Res> result = Collections.emptyList();

    public AndroidFileChooser(final AndroidFramework framework) {
        this.framework = framework;
        setOpen(true);
        setMultiSelectionEnabled(false);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
    }

    @Override public AndroidFileChooser setParent(final Window parent) { this.parent = parent; return this; }

    @Override
    public AndroidFileChooser setOpen(final boolean open) {
        intent.setAction(open ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
        return this;
    }

    @Override
    public AndroidFileChooser setMultiSelectionEnabled(final boolean allow) {
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allow);
        return this;
    }

    @Override public AndroidFileChooser setTitle(final String title) { return this; }

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
        final AndroidWindow aw = (AndroidWindow) w.frameworkWindow.get();
        final Activity a = aw.activity.get();
        if (a == null)
            return;
        final int code = 2;
        aw.filePicker.put(code, l -> {
            result = l;
            listener.accept(l != null);
        });
        a.startActivityForResult(intent, code);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<Res> iterator() {
        return result.iterator();
    }
}
