package de.pbma.nearfly;

import android.os.Environment;

import java.io.File;

/** A set of constants used within the app. */
public class Constants {
  /** A tag for logging. Use 'adb logcat -s NearbyConnections' to follow the logs. */
  public static final String TAG = "NearbyConnections";

  public static final String fileDirectory = Environment.getExternalStorageDirectory() + File.separator
                            + Environment.DIRECTORY_DOWNLOADS + File.separator + "Nearby";
}
