package illa4257.i4Framework.base.capabilities;

public interface BluetoothAdvertiseData {
    BluetoothAdvertiseData addManufacturerData(final int manufacturerId, final byte[] payload);
}