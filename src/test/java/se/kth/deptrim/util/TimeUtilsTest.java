package se.kth.deptrim.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeUtilsTest {

  TimeUtils timeUtils;

  @BeforeEach
  void setUp() {
    timeUtils = new TimeUtils();
  }

  @Test
  void ifGettingTheTime_ThenTimeIsCorrect() {
    Assertions.assertEquals("1min 40s", timeUtils.toHumanReadableTime(100000));
  }
}