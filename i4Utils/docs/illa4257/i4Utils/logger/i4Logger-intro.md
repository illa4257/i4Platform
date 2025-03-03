## Example

Usage:
```java
import illa4257.i4Utils.logger.AnsiColoredPrintStreamLogHandler;
import illa4257.i4Utils.logger.i4Logger;

import static illa4257.i4Utils.logger.Level.*;

public class Main {
    public static final i4Logger logger = new i4Logger("MyApp");

    public static void main(final String... args) {
        logger.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out, INFO, System.out));
        logger.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out, DEBUG, System.out));
        logger.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out, WARN, System.out));
        logger.registerHandler(new AnsiColoredPrintStreamLogHandler(System.out, ERROR, System.out));
        
        i4Logger.setParent(logger);
        System.setOut(logger.newPrintStream(INFO));
        System.setErr(logger.newPrintStream(ERROR));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.log(e));

        logger.log(INFO, "Hello, world!");
    }
}
```