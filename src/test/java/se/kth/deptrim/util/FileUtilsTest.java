package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class FileUtilsTest {

  @Test
  void testDeleteEmptyDirectories() throws IOException {
    // Create a temporary directory for testing
    File tempDir = Files.createTempDirectory("temp").toFile();
    // Create some empty directories within the temporary directory
    File dir1 = new File(tempDir, "dir1");
    dir1.mkdir();
    File dir2 = new File(tempDir, "dir2");
    dir2.mkdir();
    File dir3 = new File(dir1, "dir3");
    dir3.mkdir();
    // Verify that the temporary directory contains the expected number of directories
    Assertions.assertEquals(2, Objects.requireNonNull(tempDir.listFiles()).length);
    // Call the deleteEmptyDirectories method and verify that it deletes all empty directories
    FileUtils fileUtils = new FileUtils();
    fileUtils.deleteEmptyDirectories(tempDir);
    Assertions.assertEquals(3, fileUtils.getDeletedDirectories());
    Assertions.assertEquals(0, Objects.requireNonNull(tempDir.listFiles()).length);
    // Clean up the temporary directory
    tempDir.delete();
  }
}