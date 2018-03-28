package nz.org.winters.android.aaplay.io;

public interface ObdProgressListener {

  void stateUpdate(final ObdCommandJob job);

}