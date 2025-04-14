package illa4257.i4Framework.desktop.win32;

import com.sun.jna.Native;

/// [Microsoft Docs](https://learn.microsoft.com/en-us/windows/win32/api/commdlg/)
public class ComDlg32 {
    static { Native.register("ComDlg32"); }

    /// [Microsoft Docs](https://learn.microsoft.com/en-us/windows/win32/api/commdlg/nf-commdlg-getopenfilenamew)
    public static native boolean GetOpenFileNameW(final OpenFileNameW params);

    /// [Microsoft Docs](https://learn.microsoft.com/en-us/windows/win32/api/commdlg/nf-commdlg-getsavefilenamew)
    public static native boolean GetSaveFileNameW(final OpenFileNameW params);

    /// [Microsoft Docs](https://learn.microsoft.com/en-us/windows/win32/api/commdlg/nf-commdlg-commdlgextendederror)
    public static native int CommDlgExtendedError();
}