package chan.jas0n.ANT2BLE_HeartRate;/*
This software is subject to the license described in the License.txt file
included with this software distribution. You may not use this file except in compliance
with this license.

Copyright (c) Dynastream Innovations Inc. 2013
All rights reserved.
 */

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.DataState;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc.IHeartRateDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;
import android.widget.EditText;


//---BLE API----//
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;

/**
 * Base class to connects to Heart Rate Plugin and display all the event data.
 */
public class MainActivity extends AppCompatActivity {

    //----------------Define for ANT+-------------------//
    AntPlusHeartRatePcc hrPcc = null;
    protected PccReleaseHandle<AntPlusHeartRatePcc> releaseHandle = null;
    //----------------Define for BLE-------------------//

    UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    UUID HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    int HEART_RATE_MEASUREMENT_VALUE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    int EXPENDED_ENERGY_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT16;
    int INITIAL_EXPENDED_ENERGY = 0;

    String HEART_RATE_MEASUREMENT_DESCRIPTION = "Used to send a heart rate " +
            "measurement";
    UUID BODY_SENSOR_LOCATION_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
    int LOCATION_OTHER = 0;
    UUID HEART_RATE_CONTROL_POINT_UUID = UUID.fromString("00002A39-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mHeartRateMeasurementCharacteristic;
    private BluetoothGattCharacteristic mBodySensorLocationCharacteristic;
    private BluetoothGattCharacteristic mHeartRateControlPoint;
    private BluetoothGattService mHeartRateService;
    private BluetoothManager mBluetoothManagerHr;
    private BluetoothAdapter mBluetoothAdapterHr;
    private AdvertiseSettings mAdvSettings;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private BluetoothLeAdvertiser mAdvertiser;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private ServiceFragment mCurrentServiceFragment;

    int heartRateMeasurementValue = 0;
    int expendedEnergy = 0;


    private EditText mEditTextHeartRateMeasurement;

    //--------Define TextView--------//
    TextView textView_status; //needed, basic info
    TextView textView_BLEHR;
    TextView textView_BLE_Status;

    //-----------Define Button--------------//
    Button button_ant_reset;
    Button button_reset_BLE;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-4678027768868902~5651473673");
        //MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        AdView mAdView2 = (AdView) findViewById(R.id.adView2);
        AdRequest adRequest2 = new AdRequest.Builder().build();
        mAdView2.loadAd(adRequest2);

        mBluetoothDevices = new HashSet<>();

        //-----------------Set reading to textview items--------------------------------------------//
        textView_status = (TextView)findViewById(R.id.textView_Status);
        textView_BLEHR = (TextView)findViewById(R.id.textView_BLEHR);
        textView_BLE_Status = (TextView)findViewById(R.id.textView_BLE_status);

        button_ant_reset = (Button)findViewById(R.id.button_ant_reset);
        button_reset_BLE = (Button)findViewById(R.id.button_reset_BLE);
        button_ant_reset.setEnabled(true);
        button_reset_BLE.setEnabled(false);
        //-----------------button action--------------//
        button_ant_reset.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        handleReset();
                    }
                });

        //----------------notifyButtonListener-----------//
        button_reset_BLE.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        disconnectFromDevices();
                        advertise();
                    }
                });
        handleReset();
    }


    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    protected void handleReset()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }

        requestAccessToPcc();
    }

    protected void showDataDisplay(String status)
    {
        

        //Reset the text display
        textView_status.setText(status);
    }

    protected void requestAccessToPcc()
    {
        Intent intent = getIntent();
        if (intent.hasExtra(Activity_MultiDeviceSearchSampler.EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT))
        {
            // device has already been selected through the multi-device search
            MultiDeviceSearchResult result = intent
                    .getParcelableExtra(Activity_MultiDeviceSearchSampler.EXTRA_KEY_MULTIDEVICE_SEARCH_RESULT);
            releaseHandle = AntPlusHeartRatePcc.requestAccess(this, result.getAntDeviceNumber(), 0,
                    base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
        } else
        {
            // starts the plugins UI search
            releaseHandle = AntPlusHeartRatePcc.requestAccess(this, this,
                    base_IPluginAccessResultReceiver, base_IDeviceStateChangeReceiver);
        }
    }
    /**
     * Switches the active view to the data display and subscribes to all the data events
     */
    public void subscribeToHrEvents()
    {
        button_reset_BLE.setEnabled(true);
        advertise();

        hrPcc.subscribeHeartRateDataEvent(new IHeartRateDataReceiver()
        {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                           final int computedHeartRate, final long heartBeatCount,
                                           final BigDecimal heartBeatEventTime, final DataState dataState)
            {
                // Mark heart rate with asterisk if zero detected
                final String textHeartRate = String.valueOf(computedHeartRate)
                        + ((DataState.ZERO_DETECTED.equals(dataState)) ? "*" : "");

                // Mark heart beat count and heart beat event time with asterisk if initial value
                final String textHeartBeatCount = String.valueOf(heartBeatCount)
                        + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");
                final String textHeartBeatEventTime = String.valueOf(heartBeatEventTime)
                        + ((DataState.INITIAL_VALUE.equals(dataState)) ? "*" : "");

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        heartRateMeasurementValue = computedHeartRate;
                        putvalue();
                    }
                });
            }
        });
    }

    protected IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new IPluginAccessResultReceiver<AntPlusHeartRatePcc>()
            {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                             DeviceState initialDeviceState)
                {
                    //showDataDisplay("Connecting...");
                    switch(resultCode)
                    {
                        case SUCCESS:
                            hrPcc = result;
                            textView_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                            subscribeToHrEvents();
                            //if(!result.supportsRssi()) tv_rssi.setText("N/A");
                            break;
                        case CHANNEL_NOT_AVAILABLE:
                            Toast.makeText(MainActivity.this, "Channel Not Available", Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                        case ADAPTER_NOT_DETECTED:
                            Toast.makeText(MainActivity.this, "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.", Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                        case BAD_PARAMS:
                            //Note: Since we compose all the params ourself, we should never see this result
                            Toast.makeText(MainActivity.this, "Bad request parameters.", Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                        case OTHER_FAILURE:
                            Toast.makeText(MainActivity.this, "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                        case DEPENDENCY_NOT_INSTALLED:
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            AlertDialog.Builder adlgBldr = new AlertDialog.Builder(MainActivity.this);
                            adlgBldr.setTitle("Missing Dependency");
                            adlgBldr.setMessage("The required service\n\"" + AntPlusHeartRatePcc.getMissingDependencyName() + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                            adlgBldr.setCancelable(true);
                            adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    Intent startStore = null;
                                    startStore = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + AntPlusHeartRatePcc.getMissingDependencyPackageName()));
                                    startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    MainActivity.this.startActivity(startStore);
                                }
                            });
                            adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            final AlertDialog waitDialog = adlgBldr.create();
                            waitDialog.show();
                            break;
                        case USER_CANCELLED:
                            textView_status.setText("Cancelled. Please reset ANT+ connection.");
                            break;
                        case UNRECOGNIZED:
                            Toast.makeText(MainActivity.this,
                                    "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                                    Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                        default:
                            Toast.makeText(MainActivity.this, "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                            textView_status.setText("Error. Please reset ANT+ connection.");
                            break;
                    }
                }
            };

    //Receives state changes and shows it on the status display line
    protected  IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new IDeviceStateChangeReceiver()
            {
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            textView_status.setText(hrPcc.getDeviceName() + ": " + newDeviceState);
                            if(newDeviceState.equals("DEAD")){
                                handleReset();
                            }
                        }
                    });


                }
            };

    @Override
    protected void onDestroy()
    {
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }
        super.onDestroy();
    }

    //-------------------ant+ part end---------//
    //-------------------BLE Part Start---------//
    private void advertise() {
        mHeartRateMeasurementCharacteristic =
                new BluetoothGattCharacteristic(HEART_RATE_MEASUREMENT_UUID,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            /* No permissions */ 0);

        mBodySensorLocationCharacteristic =
                new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        mHeartRateControlPoint =
                new BluetoothGattCharacteristic(HEART_RATE_CONTROL_POINT_UUID,
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_WRITE);

        mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mHeartRateService.addCharacteristic(mHeartRateMeasurementCharacteristic);
        mHeartRateService.addCharacteristic(mBodySensorLocationCharacteristic);
        mHeartRateService.addCharacteristic(mHeartRateControlPoint);

        mBluetoothManagerHr = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapterHr = mBluetoothManagerHr.getAdapter();
        mBluetoothAdapterHr.setName("ANT+2BLE HeartRate");
        mGattServer = mBluetoothManagerHr.openGattServer(this, mGattServerCallback);
        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        if (mGattServer == null) {
            ensureBleFeaturesAvailable();
            return;
        }
        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer.addService(mHeartRateService);
        //---- define the settings that are used when advertising ---//
        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(true)
                .setTimeout(0)
                .build();
        //---- define a UUID that is used for identifying your packets ---//
        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid)));
        //---- create the ParcelUuid, AdvertiseData object, and broadcast some additional data as an additional service ---//
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(pUuid)
                .addServiceData(pUuid, "Data".getBytes(Charset.forName("UTF-8")))
                .build();
        //---- to advertise over Bluetooth LE with your device is create a callback that listens for success or failure when advertising ---//
        mAdvCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE", "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
            }
        };
        mAdvData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(mHeartRateService.getUuid()))
                .build();
        mAdvScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        //---- retrieve the BluetoothLeAdvertiser from the Android BluetoothAdapter ---//
        mAdvertiser = mBluetoothAdapterHr.getBluetoothLeAdvertiser();
        //--- start advertising over the BluetoothLeAdvertiser ---//
        mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
        button_reset_BLE.setEnabled(true);
        textView_BLE_Status.setText("Heart Rate Sensor Connected, please search for 'ANT+2BLE HeartRate'.\n If it is idled for too long, please reset BLE connection.");
    }
    private void putvalue(){
        mHeartRateMeasurementCharacteristic.setValue(new byte[]{0b00001000, 0, 0, 0});
        // Characteristic Value: [flags, 0, 0, 0]
        mHeartRateMeasurementCharacteristic.setValue(heartRateMeasurementValue,
                HEART_RATE_MEASUREMENT_VALUE_FORMAT,
        /* offset */ 1);
        // Characteristic Value: [flags, heart rate value, 0, 0]
        mHeartRateMeasurementCharacteristic.setValue(expendedEnergy,
                EXPENDED_ENERGY_FORMAT,
        /* offset */ 2);
        // Characteristic Value: [flags, heart rate value, energy expended (LSB), energy expended (MSB)]
        //mEditTextEnergyExpended.setText(Integer.toString(expendedEnergy));

        textView_BLEHR.setText(Integer.toString(heartRateMeasurementValue));
        sendNotificationToDevices(mHeartRateMeasurementCharacteristic);
    }

    String TAG = MainActivity.class.getCanonicalName();

    UUID CHARACTERISTIC_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");
    UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Not broadcasting: " + errorCode);
            int statusText;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    statusText = R.string.status_advertising;
                    Log.w(TAG, "App was already advertising");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    statusText = R.string.status_advDataTooLarge;
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    statusText = R.string.status_advFeatureUnsupported;
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    statusText = R.string.status_advInternalError;
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    statusText = R.string.status_advTooManyAdvertisers;
                    break;
                default:
                    statusText = R.string.status_notAdvertising;
                    Log.wtf(TAG, "Unhandled error: " + errorCode);
            }
            textView_BLE_Status.setText(statusText);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
            textView_BLE_Status.setText("Broadcasting");
        }
    };

    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Disconnected from device");
                }
            } else {
                mBluetoothDevices.remove(device);
                updateConnectedDevicesStatus();
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                final String errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
            int status = mCurrentServiceFragment.writeCharacteristic(characteristic, offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            int status = BluetoothGatt.GATT_SUCCESS;
            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                boolean supportsNotifications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
                boolean supportsIndications = (characteristic.getProperties() &
                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

                if (!(supportsNotifications || supportsIndications)) {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                } else if (value.length != 2) {
                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsDisabled(characteristic);
                    descriptor.setValue(value);
                } else if (supportsNotifications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
                    descriptor.setValue(value);
                } else if (supportsIndications &&
                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
                    descriptor.setValue(value);
                } else {
                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                }
            } else {
                status = BluetoothGatt.GATT_SUCCESS;
                descriptor.setValue(value);
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };
    private void updateConnectedDevicesStatus() {
        final String message = getString(R.string.status_devicesConnected) + " "
                + mBluetoothManagerHr.getConnectedDevices(BluetoothGattServer.GATT).size();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
    //@Override
    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic) {
        boolean indicate = (characteristic.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                == BluetoothGattCharacteristic.PROPERTY_INDICATE;
        for (BluetoothDevice device : mBluetoothDevices) {
            // true for indication (acknowledge) and false for notification (unacknowledge).
            mGattServer.notifyCharacteristicChanged(device, characteristic, indicate);
            textView_BLE_Status.setText("Sending Data to BLE Device.");
        }
    }
    private void ensureBleFeaturesAvailable() {
        if (mBluetoothAdapterHr == null) {
            Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
            finish();
        } else if (!mBluetoothAdapterHr.isEnabled()) {
            // Make sure bluetooth is enabled.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            //advertise();
        }
    }
    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        for (BluetoothDevice device : mBluetoothManagerHr.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
            mGattServer.cancelConnection(device);
        }
    }
    //-------------BLE part end---------//
}
