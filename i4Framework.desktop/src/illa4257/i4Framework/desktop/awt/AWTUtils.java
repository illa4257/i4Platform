package illa4257.i4Framework.desktop.awt;

import java.awt.*;

import static java.awt.Cursor.*;
import static java.awt.Cursor.DEFAULT_CURSOR;
import static java.awt.Cursor.E_RESIZE_CURSOR;
import static java.awt.Cursor.MOVE_CURSOR;
import static java.awt.Cursor.NE_RESIZE_CURSOR;
import static java.awt.Cursor.NW_RESIZE_CURSOR;
import static java.awt.Cursor.N_RESIZE_CURSOR;
import static java.awt.Cursor.SE_RESIZE_CURSOR;
import static java.awt.Cursor.SW_RESIZE_CURSOR;
import static java.awt.Cursor.S_RESIZE_CURSOR;
import static java.awt.Cursor.W_RESIZE_CURSOR;

public class AWTUtils {
    public static int getCursorId(final illa4257.i4Framework.base.styling.Cursor cursor) {
        switch (cursor) {
            case TEXT: return Cursor.TEXT_CURSOR;
            case POINTER: return HAND_CURSOR;
            case GRAB: return MOVE_CURSOR; // not supported
            case GRABBING: return MOVE_CURSOR; // not supported
            case N_RESIZE: return N_RESIZE_CURSOR;
            case SE_RESIZE: return SE_RESIZE_CURSOR;
            case E_RESIZE: return E_RESIZE_CURSOR;
            case EW_RESIZE: return E_RESIZE_CURSOR; // Not defined
            case NE_RESIZE: return NE_RESIZE_CURSOR;
            case NS_RESIZE: return N_RESIZE_CURSOR; // Not defined
            case NW_RESIZE: return NW_RESIZE_CURSOR;
            case NWSE_RESIZE: return MOVE_CURSOR;
            case S_RESIZE: return S_RESIZE_CURSOR;
            case SW_RESIZE: return SW_RESIZE_CURSOR;
            case W_RESIZE: return W_RESIZE_CURSOR;
            default: return DEFAULT_CURSOR;
        }
    }

    public static Cursor getCursor(final illa4257.i4Framework.base.styling.Cursor cursor) {
        //noinspection MagicConstant
        return getPredefinedCursor(getCursorId(cursor));
    }
}