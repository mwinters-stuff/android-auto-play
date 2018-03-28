package nz.org.winters.android.aaplay.io;

import android.util.Log;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import org.androidannotations.annotations.EService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */

@EService
public class MockObdGatewayService extends AbstractGatewayService {

  private static final String TAG = MockObdGatewayService.class.getName();

  public void startService() {
    Log.d(TAG, "Starting " + this.getClass().getName() + " service..");

    // Let's configure the connection.
    Log.d(TAG, "Queing jobs for connection configuration..");
    queueJob(new ObdCommandJob(new ObdResetCommand()));
    queueJob(new ObdCommandJob(new EchoOffCommand()));

    /*
     * Will send second-time based on tests.
     *
     * TODO this can be done w/o having to queue jobs by just issuing
     * command.run(), command.getResult() and validate the result.
     */
    queueJob(new ObdCommandJob(new EchoOffCommand()));
    queueJob(new ObdCommandJob(new LineFeedOffCommand()));
    queueJob(new ObdCommandJob(new TimeoutCommand(62)));

    // For now set protocol to AUTO
    queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.AUTO)));

    // Job for returning dummy data
    queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

    queueCounter = 0L;
    Log.d(TAG, "Initialization jobs queued.");

    isRunning = true;
  }


  /**
   * Runs the queue until the service is stopped
   */
  protected void executeQueue() {
    Log.d(TAG, "Executing queue..");
    long tick = System.currentTimeMillis();
    while (!Thread.currentThread().isInterrupted()) {
      ObdCommandJob job = null;
      try {
        job = jobsQueue.poll(10, TimeUnit.MILLISECONDS);
        if (job != null) {
          try {
            Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

            if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
              Log.d(TAG, "Job state is NEW. Run it..");
              job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
              Log.d(TAG, job.getCommand().getName());
              job.getCommand().run(new ByteArrayInputStream("41 00 00 00>41 00 00 00>41 00 00 00>".getBytes()), new ByteArrayOutputStream());
            } else {
              Log.e(TAG, "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            }

          } catch (Exception e) {
            e.printStackTrace();
            job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
            Log.e(TAG, "Failed to run command. -> " + e.getMessage());
          }

          Log.d(TAG, "Job is finished.");
          job.setState(ObdCommandJob.ObdCommandJobState.FINISHED);
          doCallbacks(job);
        }
      } catch (InterruptedException i) {
        Thread.currentThread().interrupt();
      }

      if (System.currentTimeMillis() - tick >= 1000) {
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));
        tick = System.currentTimeMillis();
      }

    }
    Log.d(TAG, "Exiting queue processor..");
  }


  /**
   * Stop OBD connection and queue processing.
   */
  public void stopService() {
    Log.d(TAG, "Stopping service..");

    notificationManager.cancel(NOTIFICATION_ID);
    jobsQueue.clear();
    isRunning = false;

    // kill service
    stopSelf();
  }

}
