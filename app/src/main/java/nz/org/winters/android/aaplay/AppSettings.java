package nz.org.winters.android.aaplay;

import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value= SharedPref.Scope.UNIQUE)
public interface AppSettings {
  @DefaultString("")
  String bluetoothAdapter();

  @DefaultString("")
  String bluetoothDeviceString();
}
