package com.example.arduinonano

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arduinonano.gatt.GattHandler

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesAdapter: ArrayAdapter<String>
    private val devicesList = mutableListOf<BluetoothDevice>()
    private var gattHandler: GattHandler? = null

    private lateinit var scanButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var deviceListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check for notification permission on Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        requestPermissions()

        // Notification Channel setup for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BluetoothChannel"
            val descriptionText = "Channel for Bluetooth notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        scanButton = findViewById(R.id.scanButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        deviceListView = findViewById(R.id.deviceListView)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        devicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        deviceListView.adapter = devicesAdapter

        scanButton.setOnClickListener { startBluetoothScan() }
        disconnectButton.setOnClickListener { gattHandler?.disconnectGatt() }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val device = devicesList[position]
            gattHandler = GattHandler(this)
            gattHandler?.connectGatt(device)
        }
    }

    private fun requestPermissions() {
        val permissionHandler = PermissionHandler(this) { granted ->
            if (!granted) {
                showToast("Bluetooth permissions are required.")
            }
        }
        permissionHandler.checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showToast("Notification permission is required for proper app functionality.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        gattHandler?.disconnectGatt()
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        devicesAdapter.clear()
        devicesList.clear()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        showToast("Scanning for devices...")
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    devicesList.add(it)
                    devicesAdapter.add("${it.name ?: "Unknown"} - ${it.address}")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
