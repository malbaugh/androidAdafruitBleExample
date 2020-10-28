package com.example.blearduinoexampleproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.app.ActionBar.DISPLAY_SHOW_CUSTOM;

/*
 * This is a recycler activity that displays the list of the BLE devices available in the scan.
 *
 * */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RecyclerBleDeviceActivity extends AppCompatActivity implements BleRecyclerAdapter.ItemClickListener {

    private BleRecyclerAdapter recyclerAdapter;
    private BluetoothAdapter bleAdapter;
    private boolean scanning;

    private Handler handler;
    private static final int REQUEST_ENABLE_BT = 4;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;
    private TextView scanView;
    private ImageView stopView;
    private ProgressBar scanProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_ble_device);

        handler = new Handler();

        initializeLayout();

        // Initializes a Bluetooth recyclerAdapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanning = false;
        bleAdapter.stopLeScan(mBleScanCallback);
        invalidateScanMenu();
    }

    @Override
    public void onItemClick(View view, int position) {
        onPause();
        invalidateScanMenu();

        String name;
        if (recyclerAdapter.getDevice(position).getName() == null) {
            name = "Unknown Device";
        } else {
            name = recyclerAdapter.getDevice(position).getName();
        }

        final BluetoothDevice device = recyclerAdapter.getDevice(position);
        if (device == null) return;

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.ble_device_key),Context.MODE_PRIVATE);

        SharedPreferences.Editor prefBleDeviceEditor = sharedPref.edit();
        prefBleDeviceEditor.putString("name",device.getName());
        prefBleDeviceEditor.putString("address",device.getAddress());
        prefBleDeviceEditor.apply();

        final Intent intent = new Intent(this, MainActivity.class);

        if (scanning) {
            bleAdapter.stopLeScan(mBleScanCallback);
            scanning = false;
        }

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBleOn();
        // Clear list view recyclerAdapter.
        recyclerAdapter.clear();
        scanBleDevice(true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanBleDevice(false);
    }

    private BluetoothAdapter.LeScanCallback mBleScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recyclerAdapter.addDevice(device);
                    recyclerAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private void scanBleDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bleAdapter.stopLeScan(mBleScanCallback);
                    invalidateScanMenu();
                }
            }, SCAN_PERIOD);

            scanning = true;
            bleAdapter.startLeScan(mBleScanCallback);

        } else {
            scanning = false;
            bleAdapter.stopLeScan(mBleScanCallback);
            invalidateScanMenu();
        }
    }

    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    private void isBleOn() {
        if (!bleAdapter.isEnabled()) {
            if (!bleAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void invalidateScanMenu() {
        scanProgressBar.setVisibility(View.GONE);
        stopView.setVisibility(View.GONE);
        scanView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("WrongConstant")
    public void initializeLayout() {
        // Initialize the action bar
        this.getSupportActionBar().setDisplayOptions(DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.actionbar);

        // On click listeners initialized
        final View actionView = getSupportActionBar().getCustomView();

        scanView = actionView.findViewById(R.id.scan);
        stopView = actionView.findViewById(R.id.stop);
        scanProgressBar = actionView.findViewById(R.id.scanInProgress);

        scanView.setVisibility(View.GONE);
        stopView.setVisibility(View.VISIBLE);
        scanProgressBar.setVisibility(View.VISIBLE);

        scanView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onResume();
                scanProgressBar.setVisibility(View.VISIBLE);
                scanView.setVisibility(View.GONE);
                stopView.setVisibility(View.VISIBLE);
            }
        });

        stopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPause();
                invalidateScanMenu();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.bleRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdapter = new BleRecyclerAdapter(this);
        recyclerAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerAdapter);
    }
}