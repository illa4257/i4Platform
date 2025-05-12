package illa4257.i4Utils.web;

import java.util.concurrent.CompletableFuture;

public interface IWebClientFactory {
    CompletableFuture<WebRequest> open(final WebRequest builder);
}