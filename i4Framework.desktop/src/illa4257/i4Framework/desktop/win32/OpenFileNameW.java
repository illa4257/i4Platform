package illa4257.i4Framework.desktop.win32;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;

/// [Microsoft Docs](https://learn.microsoft.com/en-us/windows/win32/api/commdlg/ns-commdlg-openfilenamew)
@SuppressWarnings("unused")
@Structure.FieldOrder({
        "lStructSize", "hwndOwner", "hInstance", "lpstrFilter", "lpstrCustomFilter", "nMaxCustFilter",
        "nFilterIndex", "lpstrFile", "nMaxFile", "lpstrFileTitle", "nMaxFileTitle", "lpstrInitialDir", "lpstrTitle",
        "Flags", "nFileOffset", "nFileExtension", "lpstrDefExt", "lCustData", "lpfnHook", "lpTemplateName",
        "pvReserved", "dwReserved", "FlagsEx"
})
public class OpenFileNameW extends Structure {
    public final static int
            OFN_ALLOWMULTISELECT    = 0x00000200,
            OFN_ENABLESIZING        = 0x00800000,
            OFN_EXPLORER            = 0x00080000,
            OFN_FILEMUSTEXIST       = 0x00001000,
            OFN_HIDEREADONLY        = 0x00000004,
            OFN_OVERWRITEPROMPT     = 0x00000002;

    public int lStructSize = size();
    public Pointer hwndOwner, hInstance;
    public WString lpstrFilter, lpstrCustomFilter;
    public int nMaxCustFilter, nFilterIndex;
    public Pointer lpstrFile;
    public int nMaxFile;
    public WString lpstrFileTitle;
    public int nMaxFileTitle;
    public WString lpstrInitialDir, lpstrTitle;
    public int Flags;
    public short nFileOffset, nFileExtension;
    public WString lpstrDefExt;
    public Pointer lCustData, lpfnHook, lpTemplateName, pvReserved;
    public int dwReserved, FlagsEx;
}