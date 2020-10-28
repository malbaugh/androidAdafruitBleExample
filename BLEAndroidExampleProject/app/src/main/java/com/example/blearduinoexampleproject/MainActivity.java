package com.example.blearduinoexampleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.app.ActionBar.DISPLAY_SHOW_CUSTOM;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_BLUETOOTH_ADMIN_ID = 1;
    private static final int REQUEST_LOCATION_ID = 2;
    private static final int REQUEST_BLUETOOTH_ID = 3;
    private BluetoothAdapter bleAdapter;

    private String deviceName;
    private String deviceAddress;
    private BluetoothLeService bleService;
    private boolean connected = false;
    private ImageView reconnectView;
    private ProgressBar batteryStatus;
    private ProgressBar scanProgressBar;
    private TextView scanView;
    private SharedPreferences sharedPrefBLE;

    private ProgressBar xAccProgressView;
    private ProgressBar yAccProgressView;
    private ProgressBar zAccProgressView;
    private TextView xAccView;
    private TextView yAccView;
    private TextView zAccView;
    private TextView activityView;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefBLE = getSharedPreferences(getString(R.string.ble_device_key),Context.MODE_PRIVATE);
        deviceName = sharedPrefBLE.getString("name",null);
        deviceAddress = sharedPrefBLE.getString("address",null);

        bleCheck();
        locationCheck();

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bleAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeLayout();
        establishServiceConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleService != null) {
            unbindService(serviceConnection);
            bleService = null;
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(deviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

    // Handles various events fired by the Service.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                scanProgressBar.setVisibility(View.VISIBLE);
                scanView.setVisibility(View.GONE);
                reconnectView.setVisibility((View.GONE));

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                scanProgressBar.setVisibility(View.GONE);
                scanView.setVisibility(View.GONE);
                reconnectView.setVisibility((View.VISIBLE));

            }

//            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//        }
            else if (BluetoothLeService.ACTION_DATA_READ_COMPLETED.equals(action)) {
                Log.d(TAG, "Data Read Completed");

                updateUI();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLeService.ACTION_BATTERY_LEVEL) != null) {
                    Log.d(TAG, "Battery level on main activity: " + intent.getStringExtra(BluetoothLeService.ACTION_BATTERY_LEVEL));
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_READ_COMPLETED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void establishServiceConnection() {
        if (deviceName != null && deviceAddress != null) {
            scanProgressBar.setVisibility(View.VISIBLE);
            scanView.setVisibility(View.GONE);
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    private void bleCheck() {
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth permission has not been granted.
            ActivityCompat.requestPermissions(this,new String[]{BLUETOOTH},REQUEST_BLUETOOTH_ID);
        }
        if (ActivityCompat.checkSelfPermission(this, BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Bluetooth admin permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_ADMIN_ID);
        }
    }

    private void locationCheck() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Location permission has not been granted.
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, REQUEST_LOCATION_ID);
        }
    }

    private void updateUI() {
        SharedPreferences sharedPrefAgm = getSharedPreferences(getString(R.string.agm_key),Context.MODE_PRIVATE);
        String xAccAvg = sharedPrefAgm.getString("x_acc_avg", null);
        String yAccAvg = sharedPrefAgm.getString("y_acc_avg", null);
        String zAccAvg = sharedPrefAgm.getString("z_acc_avg", null);

        xAccView.setText(xAccAvg);
        yAccView.setText(yAccAvg);
        zAccView.setText(zAccAvg);

        xAccProgressView.setProgress(Integer.parseInt(xAccAvg));
        yAccProgressView.setProgress(Integer.parseInt(yAccAvg));
        zAccProgressView.setProgress(Integer.parseInt(zAccAvg));

        if (Integer.parseInt(xAccAvg) > 50 || Integer.parseInt(yAccAvg) > 50 || Integer.parseInt(zAccAvg) > 50) {
            activityView.setText("Moving");
        } else {
            activityView.setText("Still");
        }

        scanProgressBar.setVisibility(View.GONE);
    }

    public void openBleScanner() {
        Intent i = new Intent(this, RecyclerBleDeviceActivity.class);
        startActivity(i);
    }

    @SuppressLint("WrongConstant")
    public void initializeLayout() {
        this.getSupportActionBar().setDisplayOptions(DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);

        final View actionView = getSupportActionBar().getCustomView();

        batteryStatus = actionView.findViewById(R.id.batteryProgressBar);
        scanView = actionView.findViewById(R.id.scan);
        scanProgressBar = actionView.findViewById(R.id.scanInProgress);
        reconnectView = actionView.findViewById(R.id.reconnect);

        xAccView = findViewById(R.id.x_acc);
        yAccView = findViewById(R.id.y_acc);
        zAccView = findViewById(R.id.z_acc);

        xAccProgressView = findViewById(R.id.x_acc_progress);
        yAccProgressView = findViewById(R.id.y_acc_progress);
        zAccProgressView = findViewById(R.id.z_acc_progress);

        activityView = findViewById(R.id.activity_summary);

        scanView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openBleScanner();
            }
        });

        reconnectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanProgressBar.setVisibility(View.VISIBLE);
                scanView.setVisibility(View.GONE);
                reconnectView.setVisibility(View.GONE);
                bleService.connect(deviceAddress);
            }
        });
    }
}