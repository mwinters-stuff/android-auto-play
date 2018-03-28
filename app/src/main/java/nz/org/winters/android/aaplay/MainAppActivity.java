package nz.org.winters.android.aaplay;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.github.pires.obd.enums.AvailableCommandNames;
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

import nz.org.winters.android.aaplay.io.AbstractGatewayService;
import nz.org.winters.android.aaplay.io.MockObdGatewayService_;
import nz.org.winters.android.aaplay.io.ObdCommandJob;
import nz.org.winters.android.aaplay.io.ObdGatewayService_;


/**
 * A login screen that offers login via email/password.
 */
@EActivity(R.layout.main_app_activity)
public class MainAppActivity extends AppCompatActivity implements OnPermissionCallback, AbstractGatewayService.StateUpdateCallback {
  private static final String TAG = MainAppActivity.class.getSimpleName();

  private PermissionHelper permissionHelper;

  @Pref
  AppSettings_ appSettings;
  @ViewById(R.id.text_bluetooth_device)
  TextView textBluetoothDevice;
  @ViewById(R.id.button_connect)
  Button buttonConnect;

  @ViewById(R.id.textViewAT)
  TextView textViewAT;


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

  @Override
  public void onResume() {
    super.onResume();
    final BluetoothAdapter btAdapter = BluetoothAdapter
        .getDefaultAdapter();

    preRequisites = btAdapter != null && btAdapter.isEnabled();
    if (!preRequisites) {
      preRequisites = btAdapter != null && btAdapter.enable();
    }
    if (!preRequisites) {
      buttonConnect.setEnabled(true);
    }

  }

  @Override
  public void onStop() {
    super.onStop();
    doUnbindService();
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
    doBindService();
//    connectBackground();
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


  private boolean isServiceBound;
  @Nullable
  private AbstractGatewayService service;
  private ServiceConnection serviceConn = new ServiceConnection() {
    @Override

    public void onServiceConnected(ComponentName className, IBinder binder) {
      Log.d(TAG, className.toString() + " service is bound");
      isServiceBound = true;
      service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
      service.addStateUpdateCallback(MainAppActivity.this);
      Log.d(TAG, "Starting live data");
      try {
        service.startService();
//        if (preRequisites)
//          btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
      } catch (IOException ioe) {
        Log.e(TAG, "Failure Starting live data");
//        btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
        doUnbindService();
      }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    // This method is *only* called when the connection to the service is lost unexpectedly
    // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
    // So the isServiceBound attribute should also be set to false when we unbind from the service.
    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.d(TAG, className.toString() + " service is unbound");
      if (service != null) {
        service.removeStateUpdateCallback(MainAppActivity.this);
      }
      service = null;
      isServiceBound = false;
    }
  };
  private boolean preRequisites = true;

  private void doBindService() {
    if (!isServiceBound) {
      Log.d(TAG, "Binding OBD service..");
      if (preRequisites) {
//        btStatusTextView.setText(getString(R.string.status_bluetooth_connecting));

        Intent serviceIntent = ObdGatewayService_.intent(this).get();
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
      } else {
//        btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
        Intent serviceIntent = MockObdGatewayService_.intent(this).get();
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
      }
    }
  }


  private void doUnbindService() {
    if (isServiceBound && service != null) {
      if (service.isRunning()) {
        service.stopService();
//        if (preRequisites)
//          btStatusTextView.setText(getString(R.string.status_bluetooth_ok));
      }
      Log.d(TAG, "Unbinding OBD service..");
      unbindService(serviceConn);
      isServiceBound = false;
//      obdStatusTextView.setText(getString(R.string.status_obd_disconnected));
    }
  }

  @Override
  public void stateUpdate(@NonNull ObdCommandJob job) {
    Log.d(TAG, "Job Result -> " + job.getCommand().getFormattedResult());
    doUpdateUIThread(job);
  }

  @org.androidannotations.annotations.UiThread
  void doUpdateUIThread(@NonNull ObdCommandJob job) {
    TextView valueView = null;

    if (job.getState() == ObdCommandJob.ObdCommandJobState.FINISHED) {
      AvailableCommandNames command = null;
      for (AvailableCommandNames acn : AvailableCommandNames.values()) {
        if (acn.getValue().equals(job.getCommand().getName())) {
          command = acn;
          break;
        }
      }
      if (command != null) {

        switch (command) {
          case AIR_INTAKE_TEMP:
            break;
          case AMBIENT_AIR_TEMP:
            valueView = textViewAT;
            break;
          case ENGINE_COOLANT_TEMP:
            break;
          case BAROMETRIC_PRESSURE:
            break;
          case FUEL_PRESSURE:
            break;
          case INTAKE_MANIFOLD_PRESSURE:
            break;
          case ENGINE_LOAD:
            break;
          case ENGINE_RUNTIME:
            break;
          case ENGINE_RPM:
            break;
          case SPEED:
            break;
          case MAF:
            break;
          case THROTTLE_POS:
            break;
          case TROUBLE_CODES:
            break;
          case PENDING_TROUBLE_CODES:
            break;
          case PERMANENT_TROUBLE_CODES:
            break;
          case FUEL_LEVEL:
            break;
          case FUEL_TYPE:
            break;
          case FUEL_CONSUMPTION_RATE:
            break;
          case TIMING_ADVANCE:
            break;
          case DTC_NUMBER:
            break;
          case EQUIV_RATIO:
            break;
          case DISTANCE_TRAVELED_AFTER_CODES_CLEARED:
            break;
          case CONTROL_MODULE_VOLTAGE:
            break;
          case ENGINE_FUEL_RATE:
            break;
          case FUEL_BASIC_RAIL_PRESSURE:
            break;
          case FUEL_RAIL_PRESSURE:
            break;
          case VIN:
            break;
          case DISTANCE_TRAVELED_MIL_ON:
            break;
          case TIME_TRAVELED_MIL_ON:
            break;
          case TIME_SINCE_TC_CLEARED:
            break;
          case REL_THROTTLE_POS:
            break;
          case PIDS_01_20:
            break;
          case PIDS_21_40:
            break;
          case PIDS_41_60:
            break;
          case ABS_LOAD:
            break;
          case ENGINE_OIL_TEMP:
            break;
          case AIR_FUEL_RATIO:
            break;
          case WIDEBAND_AIR_FUEL_RATIO:
            break;
          case DESCRIBE_PROTOCOL:
            break;
          case DESCRIBE_PROTOCOL_NUMBER:
            break;
          case IGNITION_MONITOR:
            break;
          case FUEL_INJECTION_TIMING:
            break;
          case HYBRID_BATTERY_PACK_REMAING_LIFE:
            break;
        }
        if (valueView != null) {
          valueView.setText(job.getCommand().getFormattedResult());
        }
      }
    }
  }

}

