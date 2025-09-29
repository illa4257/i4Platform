package illa4257.i4Framework.base.events.keyboard;

import java.util.HashMap;

public class KeyMapper {
    public final HashMap<Integer, Integer> map = new HashMap<>();

    public int get(final int code) {
        final Integer r = map.get(code);
        return r != null ? r : code;
    }

    public KeyMapper m(final int from, final int to) {
        map.put(from, to);
        return this;
    }

    public KeyMapper helpers(final int ctrl, final int shift, final int alt) {
        m(ctrl, KeyEvent.CTRL);
        m(shift, KeyEvent.SHIFT);
        m(alt, KeyEvent.ALT);
        return this;
    }

    public KeyMapper del(final int backspace, final int delete) {
        m(backspace, KeyEvent.BACKSPACE);
        m(delete, KeyEvent.DELETE);
        return this;
    }

    public KeyMapper arrows(final int up, final int left, final int down, final int right) {
        m(up, KeyEvent.UP);
        m(left, KeyEvent.LEFT);
        m(down, KeyEvent.DOWN);
        m(right, KeyEvent.RIGHT);
        return this;
    }

    public KeyMapper functional(final int copy, final int paste, final int contextMenu) {
        m(copy, KeyEvent.COPY);
        m(paste, KeyEvent.PASTE);
        m(contextMenu, KeyEvent.CONTEXT_MENU);
        return this;
    }
}