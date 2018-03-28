package nz.org.winters.android.aaplay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarToast;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.MenuController;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.sdk.notification.CarNotificationExtender;


/**
 * Created by dirkvranckaert on 09/10/2017.
 */

public class HelloAndroidAutoActivity extends CarActivity {
  private static final String TAG = "MainCarActivity";

  static final String MENU_HOME = "home";
  static final String MENU_DEBUG = "debug";
  static final String MENU_DEBUG_LOG = "log";
  static final String MENU_DEBUG_TEST_NOTIFICATION = "test_notification";
  private static final int TEST_NOTIFICATION_ID = 1;
  private static final String NOTIFICATION_CHANNEL_ID = "car";

  @Override
  public void onCreate(Bundle bundle) {
    setTheme(R.style.AppTheme_Car);
    super.onCreate(bundle);
    setContentView(R.layout.activity_hello_aa);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
          getString(R.string.notification_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT);
      mChannel.setDescription(getString(R.string.notification_channel_description));
      NotificationManager mNotificationManager =
          (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      assert mNotificationManager != null;
      mNotificationManager.createNotificationChannel(mChannel);
    }


    CarUiController carUiController = getCarUiController();
    carUiController.getStatusBarController().showTitle();

    getCarUiController().getStatusBarController().setTitle("Hello AA");

    ListMenuAdapter mainMenu = new ListMenuAdapter();
    mainMenu.setCallbacks(mMenuCallbacks);
    mainMenu.addMenuItem(MENU_HOME, new MenuItem.Builder()
        .setTitle(getString(R.string.demo_title))
        .setType(MenuItem.Type.ITEM)
        .build());
    mainMenu.addMenuItem(MENU_DEBUG, new MenuItem.Builder()
        .setTitle(getString(R.string.menu_debug_title))
        .setType(MenuItem.Type.SUBMENU)
        .build());

    ListMenuAdapter debugMenu = new ListMenuAdapter();
    debugMenu.setCallbacks(mMenuCallbacks);
    debugMenu.addMenuItem(MENU_DEBUG_LOG, new MenuItem.Builder()
        .setTitle(getString(R.string.menu_exlap_stats_log_title))
        .setType(MenuItem.Type.ITEM)
        .build());
    debugMenu.addMenuItem(MENU_DEBUG_TEST_NOTIFICATION, new MenuItem.Builder()
        .setTitle(getString(R.string.menu_test_notification_title))
        .setType(MenuItem.Type.ITEM)
        .build());
    mainMenu.addSubmenu(MENU_DEBUG, debugMenu);

    MenuController menuController = carUiController.getMenuController();
    menuController.setRootMenuAdapter(mainMenu);
    menuController.showMenuButton();


  }

  private final ListMenuAdapter.MenuCallbacks mMenuCallbacks = new ListMenuAdapter.MenuCallbacks() {
    @Override
    public void onMenuItemClicked(String name) {
      switch (name) {
        case MENU_HOME:
//          switchToFragment(FRAGMENT_DEMO);
          break;
        case MENU_DEBUG_LOG:
//          switchToFragment(FRAGMENT_LOG);
          break;
        case MENU_DEBUG_TEST_NOTIFICATION:
          showTestNotification();
          break;
      }
    }


    @Override
    public void onEnter() {
    }

    @Override
    public void onExit() {
//      updateStatusBarTitle();
    }
  };


  Handler mHandler = new Handler();
  private void showTestNotification() {
    CarToast.makeText(this, "Will show notification in 5 seconds", Toast.LENGTH_SHORT).show();
    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        Notification notification = new NotificationCompat.Builder(HelloAndroidAutoActivity.this,
            NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Test notification")
            .setContentText("This is a test notification")
            .setAutoCancel(true)
            .extend(new CarNotificationExtender.Builder()
                .setTitle("Test")
                .setSubtitle("This is a test notification")
                .setActionIconResId(R.mipmap.ic_launcher)
                .setShouldShowAsHeadsUp(true)
                .setThumbnail(getCarBitmap(HelloAndroidAutoActivity.this,
                    R.mipmap.ic_launcher, R.color.car_primary, 128))

                    .build())
            .build();

        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.notify(TAG, TEST_NOTIFICATION_ID, notification);

//        CarNotificationSoundPlayer soundPlayer = new CarNotificationSoundPlayer(
//            MainCarActivity.this, R.raw.bubble);
//        soundPlayer.play();
      }
    }, 5000);
  }

  public static Bitmap getCarBitmap(Context context, @DrawableRes int id, @ColorRes int tint, int size) {
    Drawable drawable = context.getResources().getDrawable(id, context.getTheme());
    drawable.setTint(ContextCompat.getColor(context, tint));
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    Bitmap bitmap = Bitmap.createBitmap(metrics, size, size, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }
}
