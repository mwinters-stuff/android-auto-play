package nz.org.winters.android.aaplay;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.pressure.HybridBatteryPackRemaingLifeCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
import com.github.pires.obd.commands.protocol.DescribeProtocolCommand;
import com.github.pires.obd.commands.protocol.DescribeProtocolNumberCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.AvailableCommandNames;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import nz.org.winters.android.aaplay.io.AbstractGatewayService;
import nz.org.winters.android.aaplay.io.MockObdGatewayService_;
import nz.org.winters.android.aaplay.io.ObdCommandJob;
import nz.org.winters.android.aaplay.io.ObdGatewayService_;

@EFragment(R.layout.fragment_main)
public class MainFragment extends Fragment implements AbstractGatewayService.StateUpdateCallback {
  private static final String TAG = MainFragment.class.getSimpleName();
  @Pref
  AppSettings_ appSettings;
  @ViewById(R.id.text_bluetooth_device)
  TextView textBluetoothDevice;
  @ViewById(R.id.button_connect)
  Button buttonConnect;

  @ViewById(R.id.textViewAT)
  TextView textViewAT;

  @ViewById(R.id.textViewVIN)
  TextView textViewVIN;

  @ViewById(R.id.textViewSPD)
  TextView textViewSPD;

  @ViewById(R.id.textViewBAT)
  TextView textViewBAT;

  @ViewById(R.id.textViewBS)
  TextView textViewBS;

  Handler handler = new Handler();

  final ArrayList<String> deviceStrs = new ArrayList<>();
  final ArrayList<String> devices = new ArrayList<>();

  public MainFragment() {
    // Required empty public constructor
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    super.onDetach();
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
    startGetTimer();

  }

  Runnable timedInfo = new Runnable() {
    @Override
    public void run() {
      if (service != null) {
        service.queueJob(new ObdCommandJob(new SpeedCommand()));
        service.queueJob(new ObdCommandJob(new LoadCommand()));
        service.queueJob(new ObdCommandJob(new RPMCommand()));
        service.queueJob(new ObdCommandJob(new ThrottlePositionCommand()));
        service.queueJob(new ObdCommandJob(new ModuleVoltageCommand()));
        service.queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));
        service.queueJob(new ObdCommandJob(new HybridBatteryPackRemaingLifeCommand()));
        startGetTimer();
      }
    }
  };

  private void startGetTimer() {
    handler.postDelayed(timedInfo, 1000);
  }

  @Override
  public void onStop() {
    super.onStop();
    handler.removeCallbacks(timedInfo);
    doUnbindService();
  }


  @Click(R.id.button_bluetooth_device)
  void selectBluetoothDevice() {
    if (getContext() != null) {
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
      final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

      ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice,
          deviceStrs.toArray(new String[deviceStrs.size()]));

      alertDialog.setSingleChoiceItems(adapter, -1, (dialog, which) -> {
        dialog.dismiss();
        int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
        String deviceAddress = devices.get(position);
        appSettings.bluetoothAdapter().put(deviceAddress);
        appSettings.bluetoothDeviceString().put(deviceStrs.get(position));
        textBluetoothDevice.setText(deviceStrs.get(position));
        buttonConnect.setEnabled(!appSettings.bluetoothAdapter().get().isEmpty());
      });

      alertDialog.setTitle("Choose Bluetooth device");
      alertDialog.show();
    }
  }

  @Click(R.id.button_connect)
  void onConnect() {
    doBindService();
//    connectBackground();
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
      service.addStateUpdateCallback(MainFragment.this);
      Log.d(TAG, "Starting live data");
      try {
        service.startService();
        if (preRequisites) {
          textViewBS.setText(R.string.status_blue_connected);
        }
        service.queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));
        service.queueJob(new ObdCommandJob(new AvailablePidsCommand_01_20()));
        service.queueJob(new ObdCommandJob(new AvailablePidsCommand_21_40()));
        service.queueJob(new ObdCommandJob(new AvailablePidsCommand_41_60()));
        service.queueJob(new ObdCommandJob(new DescribeProtocolCommand()));
        service.queueJob(new ObdCommandJob(new DescribeProtocolNumberCommand()));
        service.queueJob(new ObdCommandJob(new ModuleVoltageCommand()));
        startGetTimer();
      } catch (IOException ioe) {
        Log.e(TAG, "Failure Starting live data");
        textViewBS.setText(R.string.status_blue_error_connect);
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
        service.removeStateUpdateCallback(MainFragment.this);
      }
      service = null;
      isServiceBound = false;
    }
  };

  private boolean preRequisites = true;

  private void doBindService() {
    if (!isServiceBound && getContext() != null) {
      Log.d(TAG, "Binding OBD service..");
      if (preRequisites) {
        textViewBS.setText(R.string.status_blue_connecting);

        Intent serviceIntent = ObdGatewayService_.intent(getContext()).get();
        getContext().bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
      } else {
        textViewBS.setText(R.string.status_blue_disabled);
        Intent serviceIntent = MockObdGatewayService_.intent(getContext()).get();
        getContext().bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
      }
    }
  }


  private void doUnbindService() {
    if (isServiceBound && service != null && getContext() != null) {
      if (service.isRunning()) {
        service.stopService();
        if (preRequisites)
          textViewBS.setText(R.string.status_blue_disconnected_ok);
      }
      Log.d(TAG, "Unbinding OBD service..");
      getContext().unbindService(serviceConn);
      isServiceBound = false;
      textViewBS.setText(R.string.status_blue_disconnected_ok);
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
        Log.d(TAG, "Command Result " + command.name() + " -> " + job.getCommand().getFormattedResult());
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
            valueView = textViewSPD;
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
            valueView = textViewVIN;
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
            valueView = textViewBAT;
            break;
        }
        if (valueView != null) {
          valueView.setText(job.getCommand().getFormattedResult());
        }
      }
    }
  }

}
