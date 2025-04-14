package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Window;
import illa4257.i4Utils.runnables.Consumer2;

import java.io.File;
import java.util.function.Consumer;

public interface IFileChooser extends Iterable<File> {
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
    void setOnFinish(final Consumer2<IFileChooser, Boolean> listener);
    default void setOnFinish(final Consumer<Boolean> listener) { setOnFinish((ignored, v) -> listener.accept(v)); }
    void start();
}