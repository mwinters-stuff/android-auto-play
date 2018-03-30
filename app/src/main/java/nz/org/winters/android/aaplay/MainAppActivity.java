package nz.org.winters.android.aaplay;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.fastaccess.permission.base.PermissionHelper;
import com.fastaccess.permission.base.callback.OnPermissionCallback;

import org.androidannotations.annotations.EActivity;


/**
 * A login screen that offers login via email/password.
 */
@EActivity(R.layout.main_app_activity)
public class MainAppActivity extends AppCompatActivity implements OnPermissionCallback {
  private static final String TAG = MainAppActivity.class.getSimpleName();

  private PermissionHelper permissionHelper;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    permissionHelper = PermissionHelper.getInstance(this, this);
    permissionHelper.setForceAccepting(true).request(Manifest.permission.ACCESS_COARSE_LOCATION);
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

