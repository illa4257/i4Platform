package illa4257.i4Framework.base;

import java.util.function.Consumer;

public interface IFileChooser {
    void setTitle(final String title);
    void setOnFinish(final Consumer<Boolean> listener);
    void start();
}