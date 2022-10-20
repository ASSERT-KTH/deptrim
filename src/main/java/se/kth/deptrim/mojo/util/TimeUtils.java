package se.kth.deptrim.mojo.util;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling time.
 */
public class TimeUtils {

  /**
   * Convert milliseconds to a human-readable format.
   *
   * @param millis The milliseconds to be converted.
   * @return A string representing the time.
   */
  public String toHumanReadableTime(long millis) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
    long seconds = (TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    return String.format("%smin %ss", minutes, seconds);
  }

}
