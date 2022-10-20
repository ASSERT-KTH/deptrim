package se.kth.deptrim.mojo.util;

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
  void ifGettingTheTimeThenTimeIsCorrect() {
    Assertions.assertEquals("1min 40s", timeUtils.toHumanReadableTime(100000));
  }
}