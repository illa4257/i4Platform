package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.IFileChooser;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.PPointAdd;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.StyleSetting;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.function.Consumer;

public class FileChooser extends Window implements IFileChooser {
    private static final int ITEM_HEIGHT = 24;

    private volatile Consumer<Boolean> onResult = null;

    private final Point offset = new NumberPointMultiplier(densityMultiplier, 8);

    public final Framework framework;
    public final FrameworkWindow frameworkWindow;

    private File current = null;

    public final Button confirm = new Button();
    public final ScrollPane pane = new ScrollPane();
    public final Container container = new Panel();

    private final Button back = new Button();

    private final TextField path = new TextField();

    public FileChooser(final Framework framework) {
        this.framework = framework;
        frameworkWindow = framework.newWindow(this);
        setSize(600, 440);
        //setSize(600, 640);
        center();

        tag.set("Window");

        back.setText("^");
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
        add(back);

        path.setStartX(new PPointAdd(back.endX, offset));
        path.setStartY(offset);
        path.setEndX(new PPointSubtract(width, offset));
        path.setEndY(back.endY);
        add(path);

        container.classes.add("list");
        container.setEndX(pane.viewableWidth);

        pane.setY(48, Unit.DP);
        pane.setEndX(width);
        pane.setEndY(new PPointSubtract(height, new NumberPointMultiplier(densityMultiplier, 64)));
        pane.setContent(container);
        add(pane);

        confirm.setText("Open");
        confirm.addEventListener(ActionEvent.class, e -> {
            if (current == null)
                return;
            frameworkWindow.dispose();
        });
        confirm.setStartX(new PPointSubtract(confirm.endX, new NumberPointMultiplier(densityMultiplier, 64)));
        confirm.setStartY(new PPointAdd(pane.endY, offset));
        confirm.setEndX(new PPointSubtract(width, offset));
        confirm.setEndY(new PPointSubtract(height, offset));
        add(confirm);
    }

    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public void setCurrentDir(final File dir) {
        synchronized (locker) {
            current = dir;
        }
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

    @Override
    public void setOnFinish(final Consumer<Boolean> listener) {
        onResult = listener;
    }

    public void start() {
        setVisible(true);
        forceRefresh();
    }
}