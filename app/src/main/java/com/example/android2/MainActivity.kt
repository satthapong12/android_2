package com.example.android2

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.util.UUID

private const val REQUEST_ENABLE_BLUETOOTH = 1
private const val REQUEST_BLUETOOTH_PERMISSION = 2
private const val TAG = "BluetoothExample"

class MainActivity : ComponentActivity() {

    private val textView: TextView by lazy { findViewById<TextView>(R.id.text_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonReceive = findViewById<Button>(R.id.button_receive)

        fun onReceiveClick(view: View) {
            // Check Bluetooth permissions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
                return
            }

            try {
                // Enable Bluetooth
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (!bluetoothAdapter.isEnabled) {
                    bluetoothAdapter.enable()
                }

                // Register broadcast receiver
                registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                // Device discovered
                                val device: BluetoothDevice? =
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                if (device != null) {
                                    // Start AsyncTask to connect and receive data
                                    ConnectAsyncTask(device).execute()
                                }
                            }
                        }
                    }
                }, IntentFilter(BluetoothDevice.ACTION_FOUND))

                // Start discovery
                bluetoothAdapter.startDiscovery()

            } catch (securityException: SecurityException) {
                // Notify the user that the operation cannot be performed due to a lack of Bluetooth permissions
                Toast.makeText(this, "You need to grant Bluetooth permission", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Set function for the Receive button
        buttonReceive.setOnClickListener(::onReceiveClick)
    }

    private inner class ConnectAsyncTask(private val device: BluetoothDevice) :
        AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String {
            return try {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Connect to the device
                    val socket: BluetoothSocket =
                        device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    socket.connect()

                    // Receive data
                    val inputStream: InputStream = socket.inputStream
                    val stringBuilder = StringBuilder()
                    inputStream.bufferedReader().use { reader ->
                        while (true) {
                            val data = reader.readLine() ?: break
// Append received data to StringBuilde

                            // Append received data to StringBuilder
                            stringBuilder.append(data).append("\n")
                        }
                    }

                    // Close the connection
                    socket.close()

                    // Return the received data
                    stringBuilder.toString()
                } else {
                    "Permission not granted"
                }
            } catch (e: IOException) {
                "Error: ${e.message}"
            }
        }

        override fun onPostExecute(result: String?) {
            // Update UI with received data
            textView.text = result
        }
    }
}