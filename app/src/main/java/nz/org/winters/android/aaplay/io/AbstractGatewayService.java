package nz.org.winters.android.aaplay.io;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nz.org.winters.android.aaplay.MainAppActivity;


public abstract class AbstractGatewayService extends Service {
  public static final int NOTIFICATION_ID = 1;
  private static final String TAG = AbstractGatewayService.class.getName();
  private final IBinder binder = new AbstractGatewayServiceBinder();
  protected NotificationManagerCompat notificationManager;
  protected boolean isRunning = false;
  protected Long queueCounter = 0L;
  protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
  // Run the executeQueue in a different thread to lighten the UI thread
  Thread t = new Thread(new Runnable() {
    @Override
    public void run() {
      try {
        executeQueue();
      } catch (InterruptedException e) {
        t.interrupt();
      }
    }
  });

  private final List<StateUpdateCallback> stateUpdateCallbacks = new ArrayList<>();

  public interface StateUpdateCallback {
    void stateUpdate(@NonNull ObdCommandJob job);
  }

  public void addStateUpdateCallback(@NonNull StateUpdateCallback stateUpdateCallback) {
    stateUpdateCallbacks.add(stateUpdateCallback);
  }

  public void removeStateUpdateCallback(@NonNull StateUpdateCallback stateUpdateCallback) {
    stateUpdateCallbacks.remove(stateUpdateCallback);
  }

  protected void doCallbacks(@NonNull ObdCommandJob job) {
    for (StateUpdateCallback callback : stateUpdateCallbacks) {
      callback.stateUpdate(job);
    }
  }


  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    notificationManager = NotificationManagerCompat.from(getApplicationContext());
    Log.d(TAG, "Creating service..");
    t.start();
    Log.d(TAG, "Service created.");

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "Destroying service...");
    notificationManager.cancel(NOTIFICATION_ID);
    t.interrupt();
    Log.d(TAG, "Service destroyed.");
  }

  public boolean isRunning() {
    return isRunning;
  }

  public boolean queueEmpty() {
    return jobsQueue.isEmpty();
  }

  /**
   * This method will add a job to the queue while setting its ID to the
   * internal queue counter.
   *
   * @param job the job to queue.
   */
  public void queueJob(ObdCommandJob job) {
    queueCounter++;
    Log.d(TAG, "Adding job[" + queueCounter + "] to queue..");

    job.setId(queueCounter);
    try {
      jobsQueue.put(job);
      Log.d(TAG, "Job queued successfully.");
    } catch (InterruptedException e) {
      job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
      Log.e(TAG, "Failed to queue job.");
    }
  }

  /**
   * Show a notification while this service is running.
   */
  protected void showNotification(String contentTitle, String contentText, int icon, boolean ongoing, boolean notify, boolean vibrate) {
    final PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainAppActivity.class), 0);
    final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "CAR");
    notificationBuilder.setContentTitle(contentTitle)
        .setContentText(contentText).setSmallIcon(icon)
        .setContentIntent(contentIntent)
        .setWhen(System.currentTimeMillis());
    // can cancel?
    if (ongoing) {
      notificationBuilder.setOngoing(true);
    } else {
      notificationBuilder.setAutoCancel(true);
    }
    if (vibrate) {
      notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
    }
    if (notify) {
      notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
  }

  abstract protected void executeQueue() throws InterruptedException;

  abstract public void startService() throws IOException;

  abstract public void stopService();

  public class AbstractGatewayServiceBinder extends Binder {
    public AbstractGatewayService getService() {
      return AbstractGatewayService.this;
    }
  }
}
