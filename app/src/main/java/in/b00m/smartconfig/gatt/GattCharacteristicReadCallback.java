package in.b00m.smartconfig.gatt;
public interface GattCharacteristicReadCallback {
    void call(byte[] characteristic);
}