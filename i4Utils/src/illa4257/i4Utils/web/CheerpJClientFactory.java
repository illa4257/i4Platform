package illa4257.i4Utils.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CheerpJClientFactory implements IWebClientFactory {
    static {
        System.loadLibrary("i4Utils.cheerpj");
    }

    public static native Object fetch0(final WebRequest request);
    public static native void fetch1(final WebRequest request, final Object object);

    @Override
    public CompletableFuture<WebRequest> open(final WebRequest builder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Object o = fetch0(builder);
                return builder.setRunner(ignored -> CompletableFuture.supplyAsync(() -> {
                    try {
                        fetch1(builder, o);
                        return builder;
                    } catch (final Exception ex) {
                        throw new CompletionException(ex);
                    }
                }));
            } catch (final Exception ex) {
                throw new CompletionException(ex);
            }
        });
    }
}
