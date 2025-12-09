package illa4257.i4Framework.desktop.win32;

import com.sun.jna.Memory;
import com.sun.jna.WString;
import illa4257.i4Framework.base.FileChooserFilter;
import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.desktop.DesktopFramework;
import illa4257.i4Utils.str.Str;
import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.runnables.Consumer2;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static illa4257.i4Framework.desktop.win32.OpenFileNameW.*;

public class WinFileChooser implements FileChooser {
    private static final int MAX_PATH = 512, MAX_FILES = 1000, LEN = MAX_FILES * MAX_PATH, BUFF_SZ = LEN * 4 + 1;

    private final OpenFileNameW session = new OpenFileNameW();

    private volatile boolean isVisible = false, open = true;
    public volatile Consumer2<FileChooser, Boolean> listener = null;
    private volatile List<File> files = Collections.emptyList();

    public WinFileChooser() {
        session.nMaxFile = LEN;
        session.lpstrFile = new Memory(BUFF_SZ);
        session.lpstrFile.clear(BUFF_SZ);
        session.Flags =
                OFN_ENABLESIZING    |
                OFN_EXPLORER        |
                OFN_FILEMUSTEXIST   |
                OFN_HIDEREADONLY    |
                OFN_OVERWRITEPROMPT ;
    }

    @Override public void setOpen(final boolean open) { this.open = open; }

    @Override public void setMultiSelectionEnabled(final boolean allow) {
        if ((session.Flags & OFN_ALLOWMULTISELECT) == 0 == allow)
            session.Flags ^= OFN_ALLOWMULTISELECT;
    }
    @Override public void setTitle(final String title) {
        session.lpstrTitle = title != null ? new WString(title) : null;
    }

    @Override public void setDefaultExt(final String extension) {
        session.lpstrDefExt = extension != null ? new WString(extension) : null;
    }

    @Override
    public void setFilter(final FileChooserFilter filters) {
        if (filters == null) {
            session.lpstrFilter = null;
            return;
        }
        final StringBuilder b = new StringBuilder();
        for (final FileChooserFilter filter : filters.filters) {
            b.append(filter.description).append('\0');
            Str.join(b, ";", filter.patterns, p -> p);
            b.append('\0');
        }
        session.lpstrFilter = new WString(b.toString());
        session.nFilterIndex = filters.selected.get() + 1;
    }

    @Override public void setParent(final Window parent) { session.hwndOwner = DesktopFramework.getWindowPointer(parent); }

    @Override public void setInitialDir(final File dir) {
        session.lpstrInitialDir = dir != null ? new WString(dir.getAbsolutePath()) : null;
    }

    @Override public void setCurrentDir(final File dir) {
        session.lpstrFile.clear(BUFF_SZ);
        if (dir != null)
            session.lpstrFile.setWideString(0, new File(dir, "*").getAbsolutePath());
    }

    @Override public void setOnFinish(final Consumer2<FileChooser, Boolean> listener) { this.listener = listener; }
    @SuppressWarnings("NullableProblems")
    @Override public Iterator<File> iterator() { return files.iterator(); }

    @Override
    public void start() {
        if (isVisible)
            return;
        isVisible = true;

        if (open ? ComDlg32.GetOpenFileNameW(session) : ComDlg32.GetSaveFileNameW(session)) {
            final byte[] bytes = session.lpstrFile.getByteArray(0, BUFF_SZ);
            final ArrayList<String> filePaths = new ArrayList<>();
            final int l = bytes.length - 1;
            byte lb = 0, c;
            for (int last = 0, i = 0; i < l; i++) {
                c = bytes[i];
                if (c == 0 && lb == 0) {
                    if (i == last)
                        break;
                    filePaths.add(new String(bytes, last, i - last, StandardCharsets.UTF_16LE));
                    last = i + 2;
                    lb = ' ';
                    continue;
                }
                lb = c;
            }

            if (filePaths.isEmpty()) {
                files = Collections.emptyList();
                isVisible = false;
                final Consumer2<FileChooser, Boolean> listener1 = listener;
                if (listener1 != null)
                    try {
                        listener1.accept(this, false);
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                return;
            }
            final File r = new File(filePaths.get(0));
            if (filePaths.size() == 1)
                files = Collections.singletonList(r);
            else {
                final ArrayList<File> fl = new ArrayList<>();
                final Iterator<String> str = filePaths.iterator();
                str.next();
                while (str.hasNext())
                    fl.add(new File(r, str.next()));
                files = fl;
            }
            isVisible = false;
            final Consumer2<FileChooser, Boolean> listener1 = listener;
            if (listener1 != null)
                try {
                    listener1.accept(this, true);
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(ex);
                }
            return;
        }
        final int err = ComDlg32.CommDlgExtendedError();
        if (err != 0)
            i4Logger.INSTANCE.log(Level.ERROR, "WinFileChooser Error: " + err, Thread.currentThread().getStackTrace());
        files = Collections.emptyList();
        isVisible = false;
        final Consumer2<FileChooser, Boolean> listener1 = listener;
        if (listener1 != null)
            try {
                listener1.accept(this, false);
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }
}