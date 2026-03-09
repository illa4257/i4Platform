package illa4257.i4Framework.android.bluetooth;

import android.bluetooth.le.AdvertiseData;
import illa4257.i4Framework.base.capabilities.BluetoothAdvertiseData;

public class AndroidAdvertiseData implements BluetoothAdvertiseData {
    public final AdvertiseData.Builder b = new AdvertiseData.Builder();

    @Override
    public BluetoothAdvertiseData addManufacturerData(final int manufacturerId, final byte[] payload) {
        b.addManufacturerData(manufacturerId, payload);
        return this;
    }
}