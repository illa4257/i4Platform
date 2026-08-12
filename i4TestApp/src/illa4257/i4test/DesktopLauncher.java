package illa4257.i4test;

import illa4257.i4Framework.swing.SwingFramework;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static illa4257.i4test.i4Test.L;

public class DesktopLauncher {

    public static ExecutorService sslPool = Executors.newFixedThreadPool(2);

    public static void main(final String[] args) throws Exception {
        L.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out));
        i4Test.init(new SwingFramework("illa4257.i4Test"));

        i4Test.start();
    }
}
