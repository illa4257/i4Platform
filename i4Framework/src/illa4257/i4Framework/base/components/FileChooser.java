package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.HorizontalAlign;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.points.PointAttach;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * @deprecated
 */
public class FileChooser extends Window {
    private static final int ITEM_HEIGHT = 24;

    public final Framework framework;
    public final FrameworkWindow frameworkWindow;

    private File current = null;

    public final Button confirm = new Button();
    public final ScrollPane pane = new ScrollPane();
    public final Container container = new Panel();

    private final Button back = new Button();

    public FileChooser(final Framework framework) {
        this.framework = framework;
        frameworkWindow = framework.newWindow(this);
        setSize(600, 440);
        //setSize(600, 640);
        center();

        back.setText("^");
        back.setX(8);
        back.setY(8);
        back.setSize(32, 32);
        back.addEventListener(ActionEvent.class, e -> {
            if (current == null)
                return;
            setCurrentDir(current.getParentFile());
            forceRefresh();
        });
        add(back);

        //pane.setX(8);
        pane.setY(48);
        pane.setEndX(width);
        pane.setEndY(new PointAttach(-48, height));
        pane.setContent(container);
        add(pane);

        confirm.setText("Open");
        confirm.addEventListener(ActionEvent.class, e -> {
            if (current == null)
                return;
            frameworkWindow.dispose();
        });
        confirm.setStartX(new PointAttach(-64, confirm.endX));
        confirm.setStartY(new PointAttach(8, pane.endY));
        confirm.setEndX(new PointAttach(-8, width));
        confirm.setEndY(new PointAttach(-8, height));
        add(confirm);
    }

    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public void setCurrentDir(final File dir) {
        synchronized (locker) {
            current = dir;
        }
    }

    private void addItems(final File[] l) {
        container.clear();
        int y = 0;
        for (final File f : l) {
            final Button btn = new Button();
            btn.setHorizontalAlign(HorizontalAlign.LEFT);
            btn.addEventListener(ActionEvent.class, event -> {
                if (f.isFile()) {
                    frameworkWindow.dispose();
                    return;
                }
                setCurrentDir(f);
                forceRefresh();
            });
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
            final File[] roots = fsv.getRoots();
            container.clear();
            int y = 0;
            for (final File f : roots) {
                final Button btn = new Button();
                btn.setHorizontalAlign(HorizontalAlign.LEFT);
                btn.addEventListener(ActionEvent.class, event -> {
                    setCurrentDir(f);
                    forceRefresh();
                });
                btn.setText(f.getName() + " (" + f.getFreeSpace() + '/' + f.getTotalSpace() + ')');
                btn.setY(y);
                btn.setEndX(container.width);
                btn.setHeight(ITEM_HEIGHT * 2);
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
        container.setEndX(new PointAttach(-pane.vBar.width.calcFloat(), pane.width));
        container.setHeight(y);
        pane.setScroll(0, 0);

        invokeLater(() -> {
            //repaint();
            pane.repaint();
            back.repaint();
        });
    }

    public void start() {
        setVisible(true);
        forceRefresh();
    }
}