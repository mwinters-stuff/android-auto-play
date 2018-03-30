package nz.org.winters.android.aaplay.io;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.util.UUID;

public class BluetoothManager {

  private static final String TAG = BluetoothManager.class.getName();
  /*
   * http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
   * #createRfcommSocketToServiceRecord(java.util.UUID)
   *
   * "Hint: If you are connecting to a Bluetooth serial board then try using the
   * well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you
   * are connecting to an Android peer then please generate your own unique
   * UUID."
   */
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  /**
   * Instantiates a BluetoothSocket for the remote device and connects it.
   * <p/>
   * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
   *
   * @param dev The remote device to connect to
   * @return The BluetoothSocket
   */
  public static BluetoothSocket connect(BluetoothDevice dev) {
    BluetoothSocket sock = null;

    Log.d(TAG, "Starting Bluetooth connection..");
    try {
      sock = dev.createRfcommSocketToServiceRecord(MY_UUID);
      sock.connect();
    } catch (Exception e1) {
      Log.e(TAG, "There was an error while establishing Bluetooth connection...", e1);
    }
    return sock;
  }
}