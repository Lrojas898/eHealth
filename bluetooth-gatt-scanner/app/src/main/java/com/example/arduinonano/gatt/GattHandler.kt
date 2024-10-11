package com.example.arduinonano.gatt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*
import android.content.pm.PackageManager
import android.Manifest




class GattHandler(private val context: Context) {
    val messageQueue = MessageQueue()

    private var bluetoothGatt: BluetoothGatt? = null
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val MY_CHARACTERISTIC_UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")

    @SuppressLint("MissingPermission")
    fun connectGatt(device: BluetoothDevice) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } else {
            // Handle permission not granted case
            Log.d("GattHandler", "Bluetooth connect permission not granted.")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } else {
            // Handle permission not granted case
            Log.d("GattHandler", "Bluetooth disconnect permission not granted.")
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d("GattHandler", "Connected to GATT server.")
                bluetoothGatt?.discoverServices()

                // Send notification when connection is successful
                sendNotification("Bluetooth Connected", "Connection established, data logging running")
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d("GattHandler", "Disconnected from GATT server.")
                bluetoothGatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        enableNotification(gatt, characteristic)
                        if (characteristic.uuid == MY_CHARACTERISTIC_UUID) {
                            readCharacteristic(gatt, characteristic)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let {
                val updatedValue = String(it.value, Charsets.UTF_8)
                Log.d("GattHandler", "Data received from Arduino: $updatedValue")
                processReceivedData(updatedValue)
            }
        }

        private fun processReceivedData(data: String) {
            // Placeholder for further data processing (e.g., OpenCV processing)
            Log.d("GattHandler", "Processing data: $data")
        }

        @SuppressLint("MissingPermission")
        private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(CCCD_UUID)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        @SuppressLint("MissingPermission")
        private fun readCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            gatt.readCharacteristic(characteristic)
        }
    }

    private fun sendNotification(title: String, message: String) {
        // Check if the notification permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Log a message or handle the case where the permission is not granted
                Log.d("GattHandler", "Notification permission not granted.")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, "CHANNEL_ID")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, builder.build())
    }

}
