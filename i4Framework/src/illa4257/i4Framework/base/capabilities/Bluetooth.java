package illa4257.i4Framework.base.capabilities;

import illa4257.i4Framework.base.components.Window;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Bluetooth {
    default CompletableFuture<BluetoothDevice> requestDevice(final Window window) {
        return CompletableFuture.completedFuture(null);
    }

    default BluetoothDevice getDevice() {
        final Iterator<BluetoothDevice> iter = getDevices().iterator();
        return iter.hasNext() ? iter.next() : null;
    }

    default List<BluetoothDevice> getDevices() { return Collections.emptyList(); }
}