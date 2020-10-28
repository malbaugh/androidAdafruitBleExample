package com.example.blearduinoexampleproject;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.example.blearduinoexampleproject.GattAttributes.ACCELEROMETER_TIME_READ;
import static com.example.blearduinoexampleproject.GattAttributes.BATTERY_LEVEL_READ;
import static com.example.blearduinoexampleproject.GattAttributes.X_ACCELERATION_READ;
import static com.example.blearduinoexampleproject.GattAttributes.X_GYROSCOPE_READ;
import static com.example.blearduinoexampleproject.GattAttributes.Y_ACCELERATION_READ;
import static com.example.blearduinoexampleproject.GattAttributes.Y_GYROSCOPE_READ;
import static com.example.blearduinoexampleproject.GattAttributes.Z_ACCELERATION_READ;
import static com.example.blearduinoexampleproject.GattAttributes.Z_GYROSCOPE_READ;

/*
 * This is a service to handle the BLE interactions.
 *
 * */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private String bleDeviceAddress;
    private BluetoothGatt bleGatt;
    private BluetoothGattCharacteristic notifyCharacteristics;
    private int connectionState = STATE_DISCONNECTED;
    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    private AccelerometerData accelerometerData;
    private ArrayList<Integer> xAcc = new ArrayList<Integer>();
    private ArrayList<Integer> yAcc = new ArrayList<Integer>();
    private ArrayList<Integer> zAcc = new ArrayList<Integer>();
    private ArrayList<Integer> xGyro = new ArrayList<Integer>();
    private ArrayList<Integer> yGyro = new ArrayList<Integer>();
    private ArrayList<Integer> zGyro = new ArrayList<Integer>();
    private ArrayList<Date> accTime = new ArrayList<Date>();

    private BatteryData batteryData;
    private int batteryLevel;
    private boolean sweepComplete = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ_COMPLETED = "ACTION_DATA_READ_COMPLETED";
    public final static String ACTION_BATTERY_LEVEL = "ACTION_BATTERY_LEVEL";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";

    public final static UUID UUID_X_ACCELERATION = UUID.fromString(GattAttributes.X_ACCELERATION_MEASUREMENT);
    public final static UUID UUID_Y_ACCELERATION = UUID.fromString(GattAttributes.Y_ACCELERATION_MEASUREMENT);
    public final static UUID UUID_Z_ACCELERATION = UUID.fromString(GattAttributes.Z_ACCELERATION_MEASUREMENT);
    public final static UUID UUID_X_GYROSCOPE = UUID.fromString(GattAttributes.X_GYROSCOPE_MEASUREMENT);
    public final static UUID UUID_Y_GYROSCOPE = UUID.fromString(GattAttributes.Y_GYROSCOPE_MEASUREMENT);
    public final static UUID UUID_Z_GYROSCOPE = UUID.fromString(GattAttributes.Z_GYROSCOPE_MEASUREMENT);
    public final static UUID UUID_ACCELERATION_TIME = UUID.fromString(GattAttributes.ACCELEROMETER_TIME_MEASUREMENT);

    public final static UUID UUID_BATTERY_LEVEL = UUID.fromString(GattAttributes.BATTERY_LEVEL);
    public final static UUID UUID_BATTERY_STATUS = UUID.fromString(GattAttributes.BATTERY_STATUS);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;

                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Attempting to start service discovery.");
                gatt.discoverServices();
                Log.i(TAG, "Connected to GATT server.");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);

            } else {
                Log.i(TAG, "Other State");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                clearDataArrays();

                List<BluetoothGattService> services = gatt.getServices();

                // Loops through available GATT Services.
                for (BluetoothGattService gattService : services) {
                    List<BluetoothGattCharacteristic> gattCharacteristicsList = gattService.getCharacteristics();

                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristicsList) {
                        if (isDataCharacteristic(gattCharacteristic) != 0) {
                            chars.add(gattCharacteristic);
                        }
                    }
                }

                requestCharacteristics(gatt);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        public void requestCharacteristics(BluetoothGatt gatt) {
            gatt.readCharacteristic(chars.get(chars.size()-1));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

                switch (isDataCharacteristic(characteristic)) {
                    case X_ACCELERATION_READ:
                    case Y_ACCELERATION_READ:
                    case Z_ACCELERATION_READ:
                    case X_GYROSCOPE_READ:
                    case Y_GYROSCOPE_READ:
                    case Z_GYROSCOPE_READ:
                    case ACCELEROMETER_TIME_READ:
                        if (sweepComplete) {
                            chars.remove(chars.get(chars.size() - 1));
                            sweepComplete = false;
                        }

                        break;

                    default:
                        chars.remove(chars.get(chars.size() - 1));
                        break;
                }

                if (chars.size() > 0) {
                    requestCharacteristics(gatt);

                } else {
                    Log.i(TAG, "Gatt server data read completed.");
                    saveAgmData();
                    broadcastUpdate(ACTION_DATA_READ_COMPLETED);
                    disconnect();
                }
            }
        }
    };

    public int isDataCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            return BATTERY_LEVEL_READ;

        } else if (UUID_X_ACCELERATION.equals(characteristic.getUuid())) {
            return X_ACCELERATION_READ;

        } else if (UUID_Y_ACCELERATION.equals(characteristic.getUuid())) {
            return Y_ACCELERATION_READ;

        } else if (UUID_Z_ACCELERATION.equals(characteristic.getUuid())) {
            return Z_ACCELERATION_READ;

        } else if (UUID_X_GYROSCOPE.equals(characteristic.getUuid())) {
            return X_GYROSCOPE_READ;

        } else if (UUID_Y_GYROSCOPE.equals(characteristic.getUuid())) {
            return Y_GYROSCOPE_READ;

        } else if (UUID_Z_GYROSCOPE.equals(characteristic.getUuid())) {
            return Z_GYROSCOPE_READ;

        } else if (UUID_ACCELERATION_TIME.equals(characteristic.getUuid())) {
            return ACCELEROMETER_TIME_READ;

        } else {
            return 0;
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);
        int charWhat = isDataCharacteristic(characteristic);
        int count;

        switch (charWhat) {
            case BATTERY_LEVEL_READ:
                batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                Log.d(TAG, String.format("Received battery level: %d", batteryLevel));
                intent.putExtra(ACTION_BATTERY_LEVEL, String.valueOf(batteryLevel));

                break;
            case X_ACCELERATION_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                xAcc.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!xAcc.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received x acceleration level: %d", xAcc.get(count)));

                break;
            case Y_ACCELERATION_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                yAcc.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!yAcc.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received y acceleration level: %d", yAcc.get(count)));

                break;
            case Z_ACCELERATION_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                zAcc.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!zAcc.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received z acceleration level: %d", zAcc.get(count)));

                break;
            case X_GYROSCOPE_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                xGyro.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!xGyro.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received x gyroscope level: %d", xGyro.get(count)));

                break;
            case Y_GYROSCOPE_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                yGyro.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!yGyro.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received y gyroscope level: %d", yGyro.get(count)));

                break;
            case Z_GYROSCOPE_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
                zGyro.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                if (!zGyro.contains(null)) {
                    sweepComplete = true;
                }
                Log.d(TAG, String.format("Received z gyroscope level: %d", zGyro.get(count)));

                break;
            case ACCELEROMETER_TIME_READ:
                count = Integer.parseInt(characteristic.getStringValue(0).split(",")[1]);
//                accTime.set(count,Integer.parseInt(characteristic.getStringValue(0).split(",")[0]));

                accTime.set(count,new Date());

                if (!accTime.contains(null)) {
                    sweepComplete = true;
                }


                break;

            default:
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();

                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);

                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                }
                break;
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private void saveAgmData() {
        accelerometerData = new AccelerometerData(xAcc, yAcc, zAcc, xGyro, yGyro, zGyro, accTime);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.agm_key),Context.MODE_PRIVATE);
        SharedPreferences.Editor prefBleDeviceEditor = sharedPref.edit();

        prefBleDeviceEditor.putString("x_acc_avg", String.valueOf(accelerometerData.getXAccelerationAvg()));
        prefBleDeviceEditor.putString("y_acc_avg", String.valueOf(accelerometerData.getYAccelerationAvg()));
        prefBleDeviceEditor.putString("z_acc_avg", String.valueOf(accelerometerData.getZAccelerationAvg()));

        prefBleDeviceEditor.apply();

    }

    private void clearDataArrays() {
        xAcc = new ArrayList<Integer>();
        yAcc = new ArrayList<Integer>();
        zAcc = new ArrayList<Integer>();
        xGyro = new ArrayList<Integer>();
        yGyro = new ArrayList<Integer>();
        zGyro = new ArrayList<Integer>();
        accTime = new ArrayList<Date>();

        for (int i=0; i < 5; i++) {
            xAcc.add(i,null);
            yAcc.add(i,null);
            zAcc.add(i,null);
            xGyro.add(i,null);
            yGyro.add(i,null);
            zGyro.add(i,null);
            accTime.add(i,null);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bleManager == null) {
            bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (bleManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bleAdapter = bleManager.getAdapter();

        if (bleAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (bleAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (bleDeviceAddress != null && address.equals(bleDeviceAddress)
                && bleGatt != null) {
            Log.d(TAG, "Trying to use an existing bleGatt for connection.");

            if (bleGatt.connect()) {
                connectionState = STATE_CONNECTING;
                Log.d(TAG, "Connecting...");
                return true;

            } else {
                Log.d(TAG, "Can't connect...");
                return false;
            }
        }

        final BluetoothDevice device = bleAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bleGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bleDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bleGatt.disconnect();
    }
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (bleGatt == null) {
            return;
        }
        bleGatt.close();
        bleGatt = null;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        bleGatt.readCharacteristic(characteristic);
    }
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bleAdapter == null || bleGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        bleGatt.setCharacteristicNotification(characteristic, enabled);

        // For only characteristics that are meant to notify
        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())
                || UUID_BATTERY_STATUS.equals(characteristic.getUuid()))
        {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bleGatt.writeDescriptor(descriptor);
        }
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bleGatt == null) return null;
        return bleGatt.getServices();
    }
}