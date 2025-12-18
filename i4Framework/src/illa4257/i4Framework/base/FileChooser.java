package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Window;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FileChooser extends Iterable<File> {
    default void requestFocus() {}

    ///  Default: true
    void setOpen(final boolean open);

    ///  Default: false
    void setMultiSelectionEnabled(final boolean allow);

    void setTitle(final String title);
    void setDefaultExt(final String extension);
    void setFilter(final FileChooserFilter filters);

    default void setParent(final Window parent) {}
    void setInitialDir(final File dir);
    void setCurrentDir(final File dir);
    void setOnFinish(final BiConsumer<FileChooser, Boolean> listener);
    default void setOnFinish(final Consumer<Boolean> listener) { setOnFinish((ignored, v) -> listener.accept(v)); }
    void start();
}