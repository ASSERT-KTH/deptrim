package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;

class JarUtilsTest {

  JarUtils jarUtils;
  TemporaryFolder temporaryFolder;
  File jarFile;
  @TempDir
  File parentFolder;

  @BeforeEach
  void setUp() {
    jarUtils = new JarUtils();
    jarFile = new File("test.jar");
    parentFolder = new File("src/test/resources");
    temporaryFolder = new TemporaryFolder(parentFolder);
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