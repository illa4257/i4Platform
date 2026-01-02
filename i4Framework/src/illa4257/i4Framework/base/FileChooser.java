package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Window;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FileChooser extends Iterable<File> {
    default FileChooser requestFocus() { return this; }

    ///  Default: true
    FileChooser setOpen(final boolean open);

    ///  Default: false
    FileChooser setMultiSelectionEnabled(final boolean allow);

    FileChooser setTitle(final String title);
    FileChooser setDefaultExt(final String extension);
    FileChooser setFilter(final FileChooserFilter filters);

    default FileChooser setParent(final Window parent) { return this; }
    FileChooser setInitialDir(final File dir);
    FileChooser setCurrentDir(final File dir);

    void startThen(final Consumer<Boolean> listener);
    default void startThen(final BiConsumer<FileChooser, Boolean> listener) { startThen(success -> listener.accept(this, success)); }

    default void start(final Consumer<FileChooser> onSuccess) {
        startThen(success -> {
            if (success) onSuccess.accept(this);
        });
    }

    default void start(final Runnable onSuccess) {
        startThen(success -> {
            if (success) onSuccess.run();
        });
    }
}