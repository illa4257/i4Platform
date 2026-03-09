package illa4257.i4Framework.base.capabilities;

import illa4257.i4Framework.base.components.Window;

import java.util.concurrent.CompletableFuture;

public interface BluetoothDevice {
    default CompletableFuture<BluetoothAdvertise> getAdvertise(final Window window) {
        return CompletableFuture.completedFuture(null);
    }
}