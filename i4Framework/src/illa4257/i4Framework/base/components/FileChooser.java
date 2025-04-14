package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.IFileChooser;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.PPointAdd;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Utils.runnables.Consumer2;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FileChooser implements IFileChooser {
    private static final int ITEM_HEIGHT = 24;
    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public final Framework framework;
    public final FrameworkWindow frameworkWindow;
    public final Window window = new Window();

    private final Point offset = new NumberPointMultiplier(window.densityMultiplier, 8);
    private final Button confirm = new Button();
    private final TextField path = new TextField();
    private final ScrollPane pane = new ScrollPane();
    private final Container container = new Panel();

    private volatile boolean open = true, multiSelection = false;
    private volatile String defaultExtension = null;
    private volatile Consumer2<IFileChooser, Boolean> listener = null;
    private volatile List<File> files = Collections.emptyList();
    private volatile File current = null;

    public FileChooser(final Framework framework) {
        this.framework = framework;
        frameworkWindow = framework.newWindow(window);
        window.setSize(600, 440);
        window.center();

        window.addEventListener(VisibleEvent.class, e -> {
            if (e.value)
                return;
            final Consumer2<IFileChooser, Boolean> l = listener;
            if (l != null)
                l.accept(this, !files.isEmpty());
        });

        final Button back = new Button("^");
        back.setStartX(offset);
        back.setStartY(offset);
        back.setWidth(32, Unit.DP);
        back.setHeight(32, Unit.DP);
        back.addEventListener(ActionEvent.class, e -> {
            if (current == null)
                return;
            setCurrentDir(current.getAbsoluteFile().getParentFile());
            forceRefresh();
        });
        window.add(back);

        path.setStartX(new PPointAdd(back.endX, offset));
        path.setStartY(offset);
        path.setEndX(new PPointSubtract(window.width, offset));
        path.setEndY(back.endY);
        window.add(path);

        container.classes.add("list");
        container.setEndX(pane.viewableWidth);

        pane.setY(48, Unit.DP);
        pane.setEndX(window.width);
        pane.setEndY(new PPointSubtract(window.height, new NumberPointMultiplier(window.densityMultiplier, 64)));
        pane.setContent(container);
        window.add(pane);

        confirm.setText("Open");
        confirm.addEventListener(ActionEvent.class, e -> {
            if (current == null)
                return;
            files = Collections.singletonList(current);
            frameworkWindow.dispose();
        });
        confirm.setStartX(new PPointSubtract(confirm.endX, new NumberPointMultiplier(window.densityMultiplier, 64)));
        confirm.setStartY(new PPointAdd(pane.endY, offset));
        confirm.setEndX(new PPointSubtract(window.width, offset));
        confirm.setEndY(new PPointSubtract(window.height, offset));
        window.add(confirm);
    }

    @Override public void setOpen(final boolean open) { this.open = open; }
    @Override public void setMultiSelectionEnabled(final boolean allow) { this.multiSelection = true; }
    @Override public void setTitle(final String title) { window.setTitle(title); }
    @Override public void setDefaultExt(final String extension) { this.defaultExtension = extension; }
    @Override public void setFilter(final FileChooserFilter filters) {}

    @Override
    public void setInitialDir(final File dir) {
        current = dir;
        path.setText(dir != null ? dir.getAbsolutePath() : "This PC");
        path.repaint();
    }

    @Override
    public void setCurrentDir(final File dir) {
        current = dir;
        path.setText(dir != null ? dir.getAbsolutePath() : "This PC");
        path.repaint();
    }

    private void addItems(final File[] l) {
        container.clear();
        int y = 0;
        for (final File f : l) {
            final Button btn = new Button();
            btn.addEventListener(ActionEvent.class, event -> {
                if (f.isFile()) {
                    files = Collections.singletonList(f);
                    frameworkWindow.dispose();
                    return;
                }
                path.setText(f.getAbsolutePath());
                path.repaint();
                setCurrentDir(f);
                forceRefresh();
            });
            btn.styles.put("text-align", new StyleSetting("left"));
            btn.setText(f.getName());
            btn.setY(y);
            btn.setEndX(container.width);
            btn.setHeight(ITEM_HEIGHT);
            container.add(btn);
            y += ITEM_HEIGHT;
        }
        resize();
    }

    private void forceRefresh() {
        if (current == null) {
            path.repaint();
            final File[] roots = File.listRoots();
            container.clear();
            int y = 0;
            for (final File f : roots) {
                final Button btn = new Button();
                btn.addEventListener(ActionEvent.class, event -> {
                    path.setText(f.getAbsolutePath());
                    path.repaint();
                    setCurrentDir(f);
                    forceRefresh();
                });
                btn.styles.put("text-align", new StyleSetting("left"));
                btn.setText(fsv.getSystemDisplayName(f) + " (" + f.getFreeSpace() + '/' + f.getTotalSpace() + ')');
                btn.setY(y, Unit.DP);
                btn.setEndX(container.width);
                btn.setHeight(ITEM_HEIGHT * 2, Unit.DP);
                container.add(btn);
                y += ITEM_HEIGHT * 2;
            }
            resize();
            return;
        }
        addItems(fsv.getFiles(current, true));
    }

    private void resize() {
        int y = container.components.isEmpty() ? 0 : container.components.size() * container.components.peek().height.calcInt();
        container.setHeight(y);
        pane.setScroll(0, 0);
        pane.repaint();
    }

    @Override public void setOnFinish(final Consumer2<IFileChooser, Boolean> listener) { this.listener = listener; }

    public void start() {
        if (window.isVisible())
            return;
        window.setVisible(true);
        forceRefresh();
    }

    @Override public Iterator<File> iterator() { return files.iterator(); }
}