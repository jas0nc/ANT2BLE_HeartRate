package chan.jas0n.ANT2BLE_HeartRate;

/**
 * Created by jachan on 4/28/2017.
 */

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

public abstract class ServiceFragment extends Fragment{
        public abstract BluetoothGattService getBluetoothGattService();
        public abstract ParcelUuid getServiceUUID();

        /**
         * Function to communicate to the ServiceFragment that a device wants to write to a
         * characteristic.
         *
         * The ServiceFragment should check that the value being written is valid and
         * return a code appropriately. The ServiceFragment should update the UI to reflect the change.
         * @param characteristic Characteristic to write to
         * @param value Value to write to the characteristic
         * @return {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} if the write operation
         * was completed successfully. See {@link android.bluetooth.BluetoothGatt} for GATT return codes.
         */
        public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
            throw new UnsupportedOperationException("Method writeCharacteristic not overridden");
        };

        /**
         * Function to notify to the ServiceFragment that a device has disabled notifications on a
         * CCC descriptor.
         *
         * The ServiceFragment should update the UI to reflect the change.
         * @param characteristic Characteristic written to
         */
        public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
            throw new UnsupportedOperationException("Method notificationsDisabled not overridden");
        };

        /**
         * Function to notify to the ServiceFragment that a device has enabled notifications on a
         * CCC descriptor.
         *
         * The ServiceFragment should update the UI to reflect the change.
         * @param characteristic Characteristic written to
         * @param indicate Boolean that says if it's indicate or notify.
         */
        public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
            throw new UnsupportedOperationException("Method notificationsEnabled not overridden");
        };

        /**
         * This interface must be implemented by activities that contain a ServiceFragment to allow an
         * interaction in the fragment to be communicated to the activity.
         */
        public interface ServiceFragmentDelegate {
            void sendNotificationToDevices(BluetoothGattCharacteristic characteristic);
        }
    }
