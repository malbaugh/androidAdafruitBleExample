package com.example.blearduinoexampleproject;

import java.util.HashMap;

/*
 * This is where you keep all your UUIDs and some other items to differentiate BLE services
 * and characteristics.
 *
 * */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";

    public static String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_STATUS = "00002a1b-0000-1000-8000-00805f9b34fb";

    public static String ACCELEROMETER_SERVICE = "00000001-627e-47e5-a3fc-ddabd97aa966";
    public static String X_ACCELERATION_MEASUREMENT = "00000002-627e-47e5-a3fc-ddabd97aa966";
    public static String Y_ACCELERATION_MEASUREMENT = "00000003-627e-47e5-a3fc-ddabd97aa966";
    public static String Z_ACCELERATION_MEASUREMENT = "00000004-627e-47e5-a3fc-ddabd97aa966";
    public static String X_GYROSCOPE_MEASUREMENT = "00000005-627e-47e5-a3fc-ddabd97aa966";
    public static String Y_GYROSCOPE_MEASUREMENT = "00000006-627e-47e5-a3fc-ddabd97aa966";
    public static String Z_GYROSCOPE_MEASUREMENT = "00000007-627e-47e5-a3fc-ddabd97aa966";
    public static String ACCELEROMETER_TIME_MEASUREMENT = "00000008-627e-47e5-a3fc-ddabd97aa966";

    public static String DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";

    public static String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static String FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static String HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static String SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";

    public static String SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb";

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final int X_ACCELERATION_READ = 1;
    public static final int Y_ACCELERATION_READ = 2;
    public static final int Z_ACCELERATION_READ = 3;
    public static final int X_GYROSCOPE_READ = 4;
    public static final int Y_GYROSCOPE_READ = 5;
    public static final int Z_GYROSCOPE_READ = 6;
    public static final int ACCELEROMETER_TIME_READ = 7;
    public static final int BATTERY_LEVEL_READ = 9;

    static {
        // Services
        attributes.put(ACCELEROMETER_SERVICE, "Accelerometer Service");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(GENERIC_ACCESS, "Generic Access");
        attributes.put(DEVICE_INFORMATION_SERVICE, "Device Information Service");
        attributes.put(GENERIC_ATTRIBUTE, "Generic Attribute");

        // Characteristics
        attributes.put(X_ACCELERATION_MEASUREMENT, "X Accelerometer Type");
        attributes.put(Y_ACCELERATION_MEASUREMENT, "Y Accelerometer Measurement");
        attributes.put(Z_ACCELERATION_MEASUREMENT, "Z Accelerometer Measurement");
        attributes.put(X_GYROSCOPE_MEASUREMENT, "X Gyroscope Impedance Measurement");
        attributes.put(Y_GYROSCOPE_MEASUREMENT, "Y Gyroscope Measurement");
        attributes.put(Z_GYROSCOPE_MEASUREMENT, "Z Gyroscope Measurement");
        attributes.put(ACCELEROMETER_TIME_MEASUREMENT, "Accelerometer Time Measurement");

        attributes.put(BATTERY_LEVEL, "Battery Level");
        attributes.put(BATTERY_STATUS, "Battery Status");

        attributes.put(DEVICE_NAME, "Device Name");
        attributes.put(APPEARANCE, "Appearance");
        attributes.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "Peripheral Preferred Connection Parameters");

        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name String");
        attributes.put(MODEL_NUMBER_STRING, "Model Number String");
        attributes.put(SERIAL_NUMBER_STRING, "Serial Number String");
        attributes.put(HARDWARE_REVISION_STRING, "Hardware Revision String");
        attributes.put(FIRMWARE_REVISION_STRING, "Firmware Revision String");
        attributes.put(SOFTWARE_REVISION_STRING, "Software Revision String");

        attributes.put(SERVICE_CHANGED, "Service Changed");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
