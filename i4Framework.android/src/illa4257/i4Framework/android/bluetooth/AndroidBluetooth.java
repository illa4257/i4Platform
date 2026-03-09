package illa4257.i4Framework.android.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import illa4257.i4Framework.base.capabilities.Bluetooth;
import illa4257.i4Framework.base.capabilities.BluetoothDevice;
import illa4257.i4Framework.base.components.Window;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AndroidBluetooth implements Bluetooth {
    public final BluetoothManager mgr;

    public AndroidBluetooth(final Activity activity) {
        this.mgr = activity.getSystemService(BluetoothManager.class);
    }

    @Override
    public CompletableFuture<BluetoothDevice> requestDevice(final Window window) {
        return CompletableFuture.completedFuture(new AndroidBluetoothDevice(mgr.getAdapter()));
    }

    @Override
    public List<BluetoothDevice> getDevices() {
        return Collections.singletonList(new AndroidBluetoothDevice(mgr.getAdapter()));
    }
}