package nz.org.winters.android.aaplay;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.fastaccess.permission.base.PermissionHelper;
import com.fastaccess.permission.base.callback.OnPermissionCallback;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import nz.org.winters.android.aaplay.AppSettings_;


/**
 * A login screen that offers login via email/password.
 */
@EActivity(R.layout.main_app_activity)
public class MainAppActivity extends AppCompatActivity implements OnPermissionCallback {
  private static final String TAG = MainAppActivity.class.getSimpleName();

  private PermissionHelper permissionHelper;

  @Pref
  AppSettings_ appSettings;
  @ViewById(R.id.text_bluetooth_device)
  TextView textBluetoothDevice;
  @ViewById(R.id.button_connect)
  Button buttonConnect;

  final ArrayList<String> deviceStrs = new ArrayList<>();
  final ArrayList<String> devices = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    permissionHelper = PermissionHelper.getInstance(this, this);
    permissionHelper.setForceAccepting(true).request(Manifest.permission.ACCESS_COARSE_LOCATION);
  }

  @AfterViews
  void onAfterViews() {
    textBluetoothDevice.setText(appSettings.bluetoothDeviceString().get());
    buttonConnect.setEnabled(!appSettings.bluetoothAdapter().get().isEmpty());
  }


  @Click(R.id.button_bluetooth_device)
  void selectBluetoothDevice() {

    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    deviceStrs.clear();
    devices.clear();
    if (pairedDevices.size() > 0) {
      for (BluetoothDevice device : pairedDevices) {
        deviceStrs.add(device.getName() + "\n" + device.getAddress());
        devices.add(device.getAddress());
      }
    }

    // show list
    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice,
        deviceStrs.toArray(new String[deviceStrs.size()]));

    alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
        String deviceAddress = devices.get(position);
        appSettings.bluetoothAdapter().put(deviceAddress);
        appSettings.bluetoothDeviceString().put(deviceStrs.get(position));
        textBluetoothDevice.setText(deviceStrs.get(position));
        buttonConnect.setEnabled(!appSettings.bluetoothAdapter().get().isEmpty());
      }
    });

    alertDialog.setTitle("Choose Bluetooth device");
    alertDialog.show();
  }

  @Click(R.id.button_connect)
  void onConnect() {
    connectBackground();
  }

  @Background
  void connectBackground(){
    try {
      BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

      BluetoothDevice device = btAdapter.getRemoteDevice(appSettings.bluetoothAdapter().get());

      UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

      BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
      socket.connect();

      new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());

      new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());

      new TimeoutCommand(62).run(socket.getInputStream(), socket.getOutputStream());

      new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onPermissionGranted(@NonNull String[] permissionName) {
    Log.d(TAG, "onPermissionGranted: " + permissionName[0]);
  }

  @Override
  public void onPermissionDeclined(@NonNull String[] permissionName) {
    Log.d(TAG, "onPermissionDeclined: " + permissionName[0]);
  }

  @Override
  public void onPermissionPreGranted(@NonNull String permissionsName) {
    Log.d(TAG, "onPermissionPreGranted: " + permissionsName);
  }

  @Override
  public void onPermissionNeedExplanation(@NonNull String permissionName) {
    Log.d(TAG, "onPermissionNeedExplanation: " + permissionName);
  }

  @Override
  public void onPermissionReallyDeclined(@NonNull String permissionName) {
    Log.d(TAG, "onPermissionReallyDeclined: " + permissionName);
  }

  @Override
  public void onNoPermissionNeeded() {
    Log.d(TAG, "onNoPermissionNeeded");
  }
}

