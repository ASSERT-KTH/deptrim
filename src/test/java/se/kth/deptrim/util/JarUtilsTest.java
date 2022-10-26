package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JarUtilsTest {

  @TempDir
  Path tempDir;

  File jarFile = new File("src/test/resources/test.jar");

  @BeforeEach
  void setUp() {
    try {
      // add folder to tempDir
      Files.createDirectory(tempDir.resolve("META-INF"));
      Path tempFile = Files.createFile(tempDir.resolve("META-INF/Manifest.mf"));
      Files.writeString(tempFile, "Manifest-Version: 1.0\n"
          + "Created-By: 1.7.0_06 (Oracle Corporation)");
    } catch (IOException e) {
      System.out.println("Error creating temporary folder for testing.");
    }
  }

  @Test
  void ifCreateJarFromDirectory_ThenJarShouldExist() throws Exception {
    JarUtils.createJarFromDirectory(tempDir.toFile(), jarFile);
    Assertions.assertTrue(jarFile.exists() && !jarFile.isDirectory());
  }

  @AfterEach
  void tearDown() {
    jarFile.delete();
  }
}