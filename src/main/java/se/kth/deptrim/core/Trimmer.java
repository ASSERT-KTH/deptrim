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
  /**
   * The coordinates of the project being analyzed, so that deptrim does not throw an error when copying files.
   */
  private String projectCoordinates;
  /**
   * The local Maven repository for deployment of specialized jars.
   */
  private String mavenLocalRepoUrl;
  private Set<String> ignoreScopes;

  /**
   * Constructor.
   *
   * @param dependencyManager  The dependency manager wrapper.
   * @param projectCoordinates The coordinates of the project being analyzed, so that deptrim does not throw an error when copying files.
   * @param mavenLocalRepoUrl  The local Maven repository for deployment of specialized jars.
   * @param ignoreScopes       The scopes to ignore.
   */
  public Trimmer(String projectCoordinates, String mavenLocalRepoUrl, DependencyManagerWrapper dependencyManager, Set<String> ignoreScopes) {
    this.projectCoordinates = projectCoordinates;
    this.mavenLocalRepoUrl = mavenLocalRepoUrl;
    this.dependencyManager = dependencyManager;
    this.ignoreScopes = ignoreScopes;
  }

  /**
   * Get all the dependencies in the project if the trimDependencies flag is not set.
   *
   * @param analysis The dependency usage analysis results
   * @return The set of all dependencies
   */
  public Set<String> getAllDependenciesIfTrimDependenciesFlagIsEmpty(ProjectDependencyAnalysis analysis, Set<String> ignoreScopes) {
    Set<String> dependenciesToTrim = new LinkedHashSet<>();
    analysis.getDependencyClassesMap()
        .forEach((dependency, types) -> {
          String dependencyCoordinates = dependency.getGroupId() + ":" + dependency.getDependencyId() + ":" + dependency.getVersion();
          dependenciesToTrim.add(dependencyCoordinates);
        });
    return dependenciesToTrim;
  }

  /**
   * Trim the unused classes from the dependencies specified by the user based on the usage analysis results.
   *
   * @param analysis         The dependency usage analysis results
   * @param trimDependencies The dependencies to be trimmed, if empty then trims all the dependencies
   */
  @SneakyThrows
  public Set<TrimmedDependency> trimLibClasses(ProjectDependencyAnalysis analysis, Set<String> trimDependencies) {
    Set<TrimmedDependency> deployedSpecializedDependencies = new LinkedHashSet<>();
    if (trimDependencies.isEmpty()) {
      log.info("No dependencies specified, trimming all dependencies except the ignored dependencies.");
      trimDependencies = getAllDependenciesIfTrimDependenciesFlagIsEmpty(analysis, ignoreScopes);
    }

    Set<String> finalTrimDependencies = trimDependencies;
    analysis
        .getDependencyClassesMap()
        .forEach((dependency, types) -> {
          String dependencyCoordinates = dependency.getGroupId() + ":" + dependency.getDependencyId() + ":" + dependency.getVersion();
          // debloating only the dependencies provided by the user and if the scope is not ignored
          if (!types.getUsedTypes().isEmpty()
              && finalTrimDependencies.contains(dependencyCoordinates)
              && !ignoreScopes.contains(dependency.getScope())
              && !dependencyCoordinates.equals(projectCoordinates)) {
            log.info("Trimming dependency " + dependencyCoordinates);
            Set<ClassName> unusedTypes = new HashSet<>(types.getAllTypes());
            unusedTypes.removeAll(types.getUsedTypes());
            log.info(dependency.getFile().getName() + " -> " + unusedTypes);
            String dependencyDirName = dependency.getFile().getName().substring(0, dependency.getFile().getName().length() - 4);
            File srcDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            File destDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            log.info("Copying files from " + srcDir.getPath() + " to " + destDir.getPath());

            // Copy all files from srcDir to destDir
            try {
              FileUtils.copyDirectory(srcDir, destDir);
            } catch (IOException e) {
              log.error("Error copying files from " + srcDir + " to " + destDir);
            }
            // Remove files in destDir.
            log.info("Removing unused types: " + unusedTypes.size());
            for (ClassName className : unusedTypes) {
              String fileName = className.toString().replace(".", File.separator) + ".class";
              File file = new File(destDir.getAbsolutePath() + File.separator + fileName);
              try {
                Files.delete(file.toPath());
              } catch (IOException e) {
                log.error("Error deleting file " + file.getPath());
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
              log.info("Deploying specialized jar to local Maven repository: " + mavenLocalRepoUrl);
              String mavenDeployCommand = "mvn deploy:deploy-file -Durl="
                  + mavenLocalRepoUrl
                  + " -Dpackaging=jar"
                  + " -Dfile=" + jarFile.getAbsolutePath()
                  + " -DgroupId=" + GROUP_ID_OF_SPECIALIZED_JAR
                  + " -DartifactId=" + dependency.getDependencyId()
                  + " -Dversion=" + dependency.getVersion();
              log.info(mavenDeployCommand);
              MavenInvoker.runCommand(mavenDeployCommand, null);
              // If successfully deployed
              TrimmedDependency trimmedDependency = new TrimmedDependency(
                  dependency.getGroupId(),
                  dependency.getDependencyId(),
                  dependency.getVersion(),
                  GROUP_ID_OF_SPECIALIZED_JAR
              );
              deployedSpecializedDependencies.add(trimmedDependency);
            } catch (IOException | InterruptedException e) {
              log.error("Error installing the trimmed dependency jar in local repository.");
              Thread.currentThread().interrupt();
            }
          }
        });
    return deployedSpecializedDependencies;
  }
}
