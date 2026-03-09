package illa4257.i4Framework.base.capabilities;

public class BluetoothAdvertiseSettings {
    public enum AdvertiseMode {
        LOW_LATENCY,
        BALANCED,
        LOW_POWER
    }

    public enum TxPowerLevel {
        HIGH,
        MEDIUM,
        LOW,
        ULTRA_LOW
    }

    public AdvertiseMode advertiseMode = AdvertiseMode.LOW_LATENCY;
    public TxPowerLevel txPowerLevel = TxPowerLevel.MEDIUM;
    public boolean connectable = true, discoverable = true;
    public int timeout = 0;

    public BluetoothAdvertiseSettings setAdvertiseMode(final AdvertiseMode mode) {
        this.advertiseMode = mode != null ? mode : AdvertiseMode.LOW_LATENCY;
        return this;
    }

    public BluetoothAdvertiseSettings setTxPowerLevel(final TxPowerLevel powerLevel) {
        this.txPowerLevel = powerLevel != null ? powerLevel : TxPowerLevel.MEDIUM;
        return this;
    }

    public BluetoothAdvertiseSettings setConnectable(final boolean connectable) {
        this.connectable = connectable;
        return this;
    }

    public BluetoothAdvertiseSettings setDiscoverable(final boolean discoverable) {
        this.discoverable = discoverable;
        return this;
    }

    public BluetoothAdvertiseSettings setTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }
}