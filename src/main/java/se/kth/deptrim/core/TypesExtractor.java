package se.kth.deptrim.core;

import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import se.kth.depclean.core.model.Dependency;
import se.kth.depclean.core.util.JarUtils;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;

/**
 * A class that extracts all the types in all the dependencies.
 */
@Slf4j
public class TypesExtractor {

  private static final String DIRECTORY_TO_EXTRACT_DEPENDENCIES = "dependency";

  private DependencyManagerWrapper dependencyManager;

  /**
   * Constructor.
   *
   * @param dependencyManager The dependency manager.
   */
  public TypesExtractor(DependencyManagerWrapper dependencyManager) {
    this.dependencyManager = dependencyManager;
  }

  /**
   * Extracts all the types in all the dependencies.
   */
  @SneakyThrows
  public void extractAllTypes() {
    final File dependencyDirectory = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES).toFile();
    FileUtils.deleteDirectory(dependencyDirectory);
    dependencyManager.dependencyGraph().allDependencies()
        .forEach(jarFile -> copyDependencies(jarFile, dependencyDirectory));
    // TODO remove this workaround later
    if (dependencyManager.getBuildDirectory().resolve("libs").toFile().exists()) {
      try {
        FileUtils.copyDirectory(
            dependencyManager.getBuildDirectory().resolve("libs").toFile(),
            dependencyDirectory
        );
      } catch (IOException | NullPointerException e) {
        log.error("Error copying directory libs to dependency");
        throw new RuntimeException(e);
      }
    }
    if (dependencyDirectory.exists()) {
      JarUtils.decompress(dependencyDirectory.getAbsolutePath());
    }
  }

  private void copyDependencies(Dependency dependency, File destFolder) {
    copyDependencies(dependency.getFile(), destFolder);
  }

  @SneakyThrows
  private void copyDependencies(File jarFile, File destFolder) {
    FileUtils.copyFileToDirectory(jarFile, destFolder);
  }

}
