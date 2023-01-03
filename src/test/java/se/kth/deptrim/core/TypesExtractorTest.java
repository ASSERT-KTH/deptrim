package se.kth.deptrim.core;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import se.kth.depclean.core.analysis.graph.DependencyGraph;
import se.kth.depclean.core.model.Dependency;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;

class TypesExtractorTest {

  File fakeProjectRoot = new File("src/test/resources/project");
  File extractedTarget = new File("src/test/resources/target");
  File extractedTypes = new File("src/test/resources/target/dependency");

  @Test
  void testExtractAllTypes() throws Exception {
    // Set up mock DependencyManagerWrapper and Dependency
    DependencyManagerWrapper dependencyManager = mock(DependencyManagerWrapper.class);
    DependencyGraph dependencyGraph = mock(DependencyGraph.class);
    Dependency dependency1 = mock(Dependency.class);
    Dependency dependency2 = mock(Dependency.class);
    HashSet<Dependency> dependencies = new HashSet<>(Arrays.asList(dependency1, dependency2));
    File libsDirectory = new File(fakeProjectRoot.getAbsolutePath() + File.separator + "libs");
    File targetDirectory = new File(fakeProjectRoot.getAbsolutePath() + File.separator + "target");
    File dependencyDirectory = new File(
        fakeProjectRoot.getAbsolutePath() + File.separator + "project" + File.separator + "target" + File.separator + "dependency"
    );
    targetDirectory.mkdir();
    dependencyDirectory.mkdir();
    libsDirectory.mkdir();
    File jarFile1 = new File("src/test/resources" + File.separator + "auto-value-annotations-1.8.1.jar");
    File jarFile2 = new File("src/test/resources" + File.separator + "checker-qual-3.8.0.jar");
    FileUtils.copyFileToDirectory(jarFile1, dependencyDirectory);
    FileUtils.copyFileToDirectory(jarFile2, libsDirectory);
    jarFile1 = new File(dependencyDirectory + File.separator + "auto-value-annotations-1.8.1.jar");
    jarFile2 = new File(libsDirectory + File.separator + "checker-qual-3.8.0.jar");
    when(dependency1.getFile()).thenReturn(jarFile1);
    when(dependency2.getFile()).thenReturn(jarFile2);
    when(dependencyManager.dependencyGraph()).thenReturn(dependencyGraph);
    when(dependencyGraph.allDependencies()).thenReturn(dependencies);
    when(dependencyManager.getBuildDirectory()).thenReturn(Paths.get("src/test/resources/target/"));
    // Create TypesExtractor and extract types
    TypesExtractor extractor = new TypesExtractor(dependencyManager);
    extractor.extractAllTypes();
    // Verify that the dependency directory was deleted and the jar files were copied
    assertTrue(dependencyDirectory.exists());
    assertTrue(new File(dependencyDirectory, "auto-value-annotations-1.8.1.jar").exists());
    assertTrue(new File(libsDirectory, "checker-qual-3.8.0.jar").exists());
    // Verify that the libs directory was copied
    assertTrue(new File(fakeProjectRoot, "libs").exists());
    // Verify that the jar files were decompressed
    assertTrue(
        new File(extractedTypes.getAbsolutePath() + File.separator + "auto-value-annotations-1.8.1/com/google/auto/value/AutoValue$Builder.class")
            .exists()
    );
    assertTrue(
        new File(extractedTypes.getAbsolutePath() + File.separator + "checker-qual-3.8.0/org/checkerframework/framework/qual/DefaultFor.class")
            .exists()
    );
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(fakeProjectRoot);
    FileUtils.deleteDirectory(extractedTarget);
    FileUtils.deleteDirectory(extractedTypes);
  }
}