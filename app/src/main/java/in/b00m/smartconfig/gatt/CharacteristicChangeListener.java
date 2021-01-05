package in.b00m.smartconfig.gatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface CharacteristicChangeListener {
    public void onCharacteristicChanged(String deviceAddress, BluetoothGattCharacteristic characteristic);
}