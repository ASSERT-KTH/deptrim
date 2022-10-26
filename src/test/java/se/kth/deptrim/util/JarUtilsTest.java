package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

class JarUtilsTest {

  @Rule
  TemporaryFolder temporaryFolder = new TemporaryFolder(new File("src/test/resources"));
  File jarFile = new File("test.jar");

  @BeforeEach
  void setUp() {
    try {
      temporaryFolder.create();
      temporaryFolder.newFolder("META-INF");
      temporaryFolder.newFile("META-INF/Manifest.mf");
    } catch (IOException e) {
      System.out.println("Error creating temporary folder for testing.");
    }
  }

  @Test
  void ifCreateJarFromDirectoryThenJarShouldExist() throws Exception {
    JarUtils.createJarFromDirectory(temporaryFolder.getRoot(), jarFile);
    Assertions.assertTrue(jarFile.exists() && !jarFile.isDirectory());
  }

  @AfterEach
  void tearDown() {
    temporaryFolder.delete();
    jarFile.delete();
  }
}