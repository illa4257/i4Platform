package illa4257.i4Framework.base.capabilities;

public interface BluetoothAdvertise {
    default BluetoothAdvertiseSettings newSettings() { return new BluetoothAdvertiseSettings(); }
    BluetoothAdvertiseData newData();

    void startAdvertising(final BluetoothAdvertiseSettings settings, final BluetoothAdvertiseData data);
}