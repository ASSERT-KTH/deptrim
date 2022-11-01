package se.kth.deptrim.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.model.ClassName;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;
import se.kth.depclean.util.MavenInvoker;

/**
 * A class that trims the dependencies of a project.
 */
@Slf4j
public class Trimmer {

  private static final String DIRECTORY_TO_EXTRACT_DEPENDENCIES = "dependency";
  private static final String DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES = "dependency-debloated";
  private static final String GROUP_ID_OF_SPECIALIZED_JAR = "se.kth.castor.deptrim.spl";
  private DependencyManagerWrapper dependencyManager;
  private Set<String> ignoreScopes;

  public Trimmer(DependencyManagerWrapper dependencyManager, Set<String> ignoreScopes) {
    this.dependencyManager = dependencyManager;
    this.ignoreScopes = ignoreScopes;
  }

  /**
   * Get all the dependencies in the project if the trimDependencies flag is not set.
   *
   * @param analysis The dependency usage analysis results
   * @return The set of all dependencies
   */
  public Set<String> getAllDependenciesIfTrimDependenciesFlagIsEmpty(ProjectDependencyAnalysis analysis) {
    Set<String> dependenciesToTrim = new LinkedHashSet<>();
    analysis.getDependencyClassesMap()
            .forEach((key, value) -> {
              String dependencyCoordinates = key.getGroupId() + ":" + key.getDependencyId() + ":" + key.getVersion();
              dependenciesToTrim.add(dependencyCoordinates);
            });
    return dependenciesToTrim;
  }

  /**
   * Trim the unused classes from the dependencies specified by the user based on the usage analysis results.
   *
   * @param analysis                 The dependency usage analysis results
   * @param thisProjectCoordinates   The coordinates of the project being analyzed,
   *                                 so that deptrim does not throw an error when copying files
   * @param trimDependencies         The dependencies to be trimmed, if empty then trims all the dependencies
   * @param mavenLocalRepoUrl        The local Maven repository for deployment of specialized jars
   */
  @SneakyThrows
  public Set<DependencyOriginalAndTrimmed> trimLibClasses(ProjectDependencyAnalysis analysis,
                                                          Set<String> trimDependencies,
                                                          String thisProjectCoordinates,
                                                          String mavenLocalRepoUrl) {
    Set<DependencyOriginalAndTrimmed> deployedSpecializedDependencies = new LinkedHashSet<>();
    if (trimDependencies.size() == 0) {
      log.info("No dependencies specified, trimming all dependencies...");
      trimDependencies = getAllDependenciesIfTrimDependenciesFlagIsEmpty(analysis);
    }
    Set<String> finalTrimDependencies = trimDependencies;
    analysis
        .getDependencyClassesMap()
        .forEach((key, value) -> {
          String dependencyCoordinates = key.getGroupId() + ":" + key.getDependencyId() + ":" + key.getVersion();
          // debloating only the dependencies provided by the user and if the scope is not ignored
          if (finalTrimDependencies.contains(dependencyCoordinates) && !ignoreScopes.contains(key.getScope()) && !dependencyCoordinates.equals(thisProjectCoordinates)) {
            log.info("Trimming dependency " + dependencyCoordinates);
            Set<ClassName> unusedTypes = new HashSet<>(value.getAllTypes());
            if (value.getUsedTypes().size() == 0) {
              log.info("Skipping specialization for bloated dependency " + dependencyCoordinates);
              return;
            }
            unusedTypes.removeAll(value.getUsedTypes());
            log.info(key.getFile().getName() + " -> " + unusedTypes);
            String dependencyDirName = key.getFile().getName().substring(0, key.getFile().getName().length() - 4);
            File srcDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            File destDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            log.info("copying from files from " + srcDir.getAbsolutePath() + " to " + destDir.getAbsolutePath());

            // Copy all files from srcDir to destDir
            try {
              FileUtils.copyDirectory(srcDir, destDir);
            } catch (IOException e) {
              log.error("Error copying files from " + srcDir + " to " + destDir);
            }
            // Remove files in destDir.
            for (ClassName className : unusedTypes) {
              String fileName = className.toString().replace(".", File.separator) + ".class";
              File file = new File(destDir.getAbsolutePath() + File.separator + fileName);
              log.info("Removing file " + file.getAbsolutePath());
              try {
                Files.delete(file.toPath());
              } catch (IOException e) {
                log.error("Error deleting file " + file.getAbsolutePath());
              }
            }
            // Delete all empty directories in destDir.
            se.kth.deptrim.util.FileUtils fileUtils = new se.kth.deptrim.util.FileUtils();
            fileUtils.deleteEmptyDirectories(destDir);

            // Create a new jar file with the debloated classes and move it to libs-deptrim.
            Path libDeptrimPath = Paths.get("libs-deptrim");
            String jarName = destDir.getName() + ".jar";
            File jarFile = libDeptrimPath.resolve(jarName).toFile();
            try {
              Files.createDirectories(libDeptrimPath); // create libs-deptrim directory if it does not exist
              se.kth.deptrim.util.JarUtils.createJarFromDirectory(destDir, jarFile);
            } catch (Exception e) {
              log.error("Error creating trimmed jar for " + destDir.getName());
            }

            // Install the dependency in the local repository.
            try {
              log.info("Deploying specialized jar to local Maven repository " + mavenLocalRepoUrl);
              String mavenDeployCommand = "mvn deploy:deploy-file -Durl=" + mavenLocalRepoUrl
                      + " -Dpackaging=jar"
                      + " -Dfile=" + jarFile.getAbsolutePath()
                      + " -DgroupId=" + GROUP_ID_OF_SPECIALIZED_JAR
                      + " -DartifactId=" + key.getDependencyId()
                      + " -Dversion=" + key.getVersion();
              log.info(mavenDeployCommand);
              MavenInvoker.runCommand(mavenDeployCommand, null);
              // If successfully deployed
              DependencyOriginalAndTrimmed originalAndTrimmedDependency = new DependencyOriginalAndTrimmed(
                      key.getGroupId(), key.getDependencyId(), key.getVersion(), GROUP_ID_OF_SPECIALIZED_JAR);
              deployedSpecializedDependencies.add(originalAndTrimmedDependency);
            } catch (IOException | InterruptedException e) {
              log.error("Error installing the trimmed dependency jar in local repo");
              Thread.currentThread().interrupt();
            }
          }
        });
    return deployedSpecializedDependencies;
  }
}
