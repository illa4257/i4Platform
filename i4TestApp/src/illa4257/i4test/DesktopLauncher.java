package illa4257.i4test;

import illa4257.i4Framework.swing.SwingFramework;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;

public class DesktopLauncher {
    public static void main(final String[] args) {
        i4Test.L.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out));
        i4Test.init(SwingFramework.INSTANCE);
    }
}
