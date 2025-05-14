## Example

Usage:
```java
package illa4257.i4Notepad;

import illa4257.i4Framework.swing.SwingFramework;
import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.web.WebClient;
import illa4257.i4Utils.web.WebRequest;
import illa4257.i4Utils.web.i4URI;

import java.nio.charset.StandardCharsets;

public class Main {
    public static final i4Logger logger = new i4Logger("MyApp");

    public static void main(final String[] args) throws Exception {
        logger.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out));
        i4Logger.INSTANCE.unregisterAllHandlers().registerHandler(logger);
        System.setOut(new PrintStream(logger.newOutputStream(INFO)));
        System.setErr(new PrintStream(logger.newOutputStream(ERROR)));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.log(e));

        final WebClient client = new WebClient();

        System.out.println("Open connection");
        final WebRequest r = client.newBuilder("POST", new i4URI("http://127.0.0.1/upload-page"))
                .setHeader("User-Agent", "userAgent/dev")
                .setBody("Hello, world!".getBytes(StandardCharsets.UTF_8))
                .run().join();

        System.out.println("Read response");
        r.run().join();

        System.out.println("STATUS: " + r.responseCode + " " + r.responseStatus);

        System.out.println(r.serverHeaders);

        System.out.println(new String(IO.readFully(r.getInputStream())));
    }
}
```