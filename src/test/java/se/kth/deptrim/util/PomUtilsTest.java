package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import se.kth.deptrim.core.SpecializedDependency;

class PomUtilsTest {

  @Test
  void testCreateSinglePomSpecialized() throws IOException {
    Set<SpecializedDependency> specializedDependencies = new HashSet<>();
    specializedDependencies.add(new SpecializedDependency("com.fasterxml.jackson.core", "jackson-databind", "2.12.2", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("com.google.guava", "guava", "17.0", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("commons-io", "commons-io", "2.11.0", "se.kth.castor"));
    File pomPath = new File("src/test/resources/pom.xml");
    File debloatedPomPath = new File("src/test/resources/pom-debloated.xml");
    FileUtils.copyFile(pomPath, debloatedPomPath);
    boolean createSinglePomSpecialized = true;
    boolean createDependencySpecializedPerPom = false;
    boolean createAllPomSpecialized = false;
    PomUtils pomUtils = new PomUtils(
        specializedDependencies,
        debloatedPomPath.getAbsolutePath(),
        createSinglePomSpecialized,
        createDependencySpecializedPerPom,
        createAllPomSpecialized
    );
    pomUtils.createPoms();
    File specializedPomFile = new File(debloatedPomPath.getAbsolutePath().replace("-debloated.xml", "-specialized.xml"));
    Assertions.assertTrue(specializedPomFile.exists());
    specializedPomFile.delete();
    debloatedPomPath.delete();
  }

  @Test
  void testCreateDependencySpecializedPerPom() throws IOException {
    Set<SpecializedDependency> specializedDependencies = new HashSet<>();
    specializedDependencies.add(new SpecializedDependency("com.fasterxml.jackson.core", "jackson-databind", "2.12.2", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("com.google.guava", "guava", "17.0", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("commons-io", "commons-io", "2.11.0", "se.kth.castor"));
    File pomPath = new File("src/test/resources/pom.xml");
    File debloatedPomPath = new File("src/test/resources/pom-debloated.xml");
    FileUtils.copyFile(pomPath, debloatedPomPath);
    boolean createSinglePomSpecialized = false;
    boolean createDependencySpecializedPerPom = true;
    boolean createAllPomSpecialized = false;
    PomUtils pomUtils = new PomUtils(
        specializedDependencies,
        debloatedPomPath.getAbsolutePath(),
        createSinglePomSpecialized,
        createDependencySpecializedPerPom,
        createAllPomSpecialized
    );
    pomUtils.createPoms();
    File specializedPomFile1 = new File(debloatedPomPath.getAbsolutePath().replace("-debloated.xml", "-specialized_1_3.xml"));
    File specializedPomFile2 = new File(debloatedPomPath.getAbsolutePath().replace("-debloated.xml", "-specialized_2_3.xml"));
    File specializedPomFile3 = new File(debloatedPomPath.getAbsolutePath().replace("-debloated.xml", "-specialized_3_3.xml"));
    Assertions.assertTrue(specializedPomFile1.exists());
    Assertions.assertTrue(specializedPomFile2.exists());
    Assertions.assertTrue(specializedPomFile3.exists());
    debloatedPomPath.delete();
    specializedPomFile1.delete();
    specializedPomFile2.delete();
    specializedPomFile3.delete();
  }

  @Test
  void testCreateAllCombinationsOfSpecializedPoms() throws IOException {
    Set<SpecializedDependency> specializedDependencies = new HashSet<>();
    specializedDependencies.add(new SpecializedDependency("com.fasterxml.jackson.core", "jackson-databind", "2.12.2", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("com.google.guava", "guava", "17.0", "se.kth.castor"));
    specializedDependencies.add(new SpecializedDependency("commons-io", "commons-io", "2.11.0", "se.kth.castor"));
    File pomPath = new File("src/test/resources/pom.xml");
    File debloatedPomPath = new File("src/test/resources/pom-debloated.xml");
    FileUtils.copyFile(pomPath, debloatedPomPath);
    boolean createSinglePomSpecialized = false;
    boolean createDependencySpecializedPerPom = false;
    boolean createAllPomSpecialized = true;
    PomUtils pomUtils = new PomUtils(
        specializedDependencies,
        debloatedPomPath.getAbsolutePath(),
        createSinglePomSpecialized,
        createDependencySpecializedPerPom,
        createAllPomSpecialized
    );
    pomUtils.createPoms();
    File resources = new File("src/test/resources/");
    // count the number of files in the directory
    int count = 0;
    for (File file : resources.listFiles()) {
      if (file.isFile() && file.getName().contains("specialized")) {
        count++;
        file.delete();
      }
    }
    Assertions.assertEquals(8, count);
    debloatedPomPath.delete();
  }

}