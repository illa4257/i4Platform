package illa4257.i4Framework.android.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import illa4257.i4Framework.android.AndroidWindow;
import illa4257.i4Framework.base.capabilities.BluetoothAdvertise;
import illa4257.i4Framework.base.capabilities.BluetoothAdvertiseData;
import illa4257.i4Framework.base.capabilities.BluetoothAdvertiseSettings;
import illa4257.i4Framework.base.capabilities.BluetoothDevice;
import illa4257.i4Framework.base.components.Window;

import java.util.concurrent.CompletableFuture;

public class AndroidBluetoothDevice implements BluetoothDevice {
    public final BluetoothAdapter adapter;

    public AndroidBluetoothDevice(final BluetoothAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public CompletableFuture<BluetoothAdvertise> getAdvertise(final Window window) {
        final CompletableFuture<BluetoothAdvertise> future = new CompletableFuture<>();
        final AndroidWindow aw = (AndroidWindow) window.frameworkWindow.get();
        final Activity a = aw.activity.get();
        final int code = Manifest.permission.BLUETOOTH_ADVERTISE.hashCode() & 0xFFFF;
        aw.permissionRequests.put(code, b -> {
            future.complete(b ? new BluetoothAdvertise() {
                final BluetoothLeAdvertiser a = adapter.getBluetoothLeAdvertiser();

                @Override
                public BluetoothAdvertiseData newData() {
                    return new AndroidAdvertiseData();
                }

                @Override
                public void startAdvertising(final BluetoothAdvertiseSettings settings, final BluetoothAdvertiseData data) {
                    if (!(data instanceof AndroidAdvertiseData))
                        throw new IllegalArgumentException("Not android's data");
                    final AdvertiseSettings.Builder b = new AdvertiseSettings.Builder()
                            .setConnectable(settings.connectable)
                            .setDiscoverable(settings.discoverable)
                            .setTimeout(settings.timeout);
                    switch (settings.advertiseMode) {
                        case LOW_LATENCY: b.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);break;
                        case BALANCED: b.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);break;
                        case LOW_POWER: b.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);break;
                    }
                    switch (settings.txPowerLevel) {
                        case HIGH: b.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);break;
                        case MEDIUM: b.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);break;
                        case LOW: b.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);break;
                        case ULTRA_LOW: b.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW);break;
                    }
                    a.startAdvertising(b.build(), ((AndroidAdvertiseData) data).b.build(), new AdvertiseCallback() {
                        @Override
                        public void onStartFailure(final int errorCode) {
                            super.onStartFailure(errorCode);
                            System.out.println("Fail " + errorCode);
                        }

                        @Override
                        public void onStartSuccess(final AdvertiseSettings settingsInEffect) {
                            super.onStartSuccess(settingsInEffect);
                            System.out.println("Success!");
                        }
                    });
                }
            } : null);
        });
        a.runOnUiThread(() -> a.requestPermissions(new String[]{ Manifest.permission.BLUETOOTH_ADVERTISE }, code));
        return future;
    }
}