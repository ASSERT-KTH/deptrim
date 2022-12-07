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
public class Specializer {

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
  public Specializer(String projectCoordinates, String mavenLocalRepoUrl, DependencyManagerWrapper dependencyManager, Set<String> ignoreScopes) {
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
  public Set<String> getAllDependencies(ProjectDependencyAnalysis analysis) {
    Set<String> dependenciesToSpecialize = new LinkedHashSet<>();
    analysis.getDependencyClassesMap()
        .forEach((dependency, types) -> {
          String dependencyCoordinates = dependency.getGroupId() + ":" + dependency.getDependencyId() + ":" + dependency.getVersion();
          dependenciesToSpecialize.add(dependencyCoordinates);
        });
    return dependenciesToSpecialize;
  }

  /**
   * Trim the unused classes from the dependencies specified by the user based on the usage analysis results.
   *
   * @param analysis         The dependency usage analysis results
   * @param specializeDependencies The dependencies to be trimmed, if empty then trims all the dependencies
   */
  @SneakyThrows
  public Set<SpecializedDependency> specialize(ProjectDependencyAnalysis analysis, Set<String> specializeDependencies) {
    Set<SpecializedDependency> deployedSpecializedDependencies = new LinkedHashSet<>();
    if (specializeDependencies.isEmpty()) {
      log.info("No dependencies specified, specializing all dependencies except the ignored dependencies.");
      specializeDependencies = getAllDependencies(analysis);
    }
    Set<String> finalSpecializedDependencies = specializeDependencies;
    analysis
        .getDependencyClassesMap()
        .forEach((dependency, types) -> {
          String dependencyCoordinates = dependency.getGroupId() + ":" + dependency.getDependencyId() + ":" + dependency.getVersion();
          // specializing only the dependencies provided by the user and if the scope is not ignored
          if (!types.getUsedTypes().isEmpty()
              && finalSpecializedDependencies.contains(dependencyCoordinates)
              && !ignoreScopes.contains(dependency.getScope())
              && !dependencyCoordinates.equals(projectCoordinates)
          ) {
            Set<ClassName> unusedTypes = new HashSet<>(types.getAllTypes());
            unusedTypes.removeAll(types.getUsedTypes());
            String dependencyDirName = dependency.getFile().getName().substring(0, dependency.getFile().getName().length() - 4);
            File srcDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            File destDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES + File.separator + dependencyDirName).toFile();

            // Copy all files from srcDir to destDir
            try {
              FileUtils.copyDirectory(srcDir, destDir);
            } catch (IOException e) {
              log.error("Error copying files from " + srcDir + " to " + destDir);
            }

            // Remove files in destDir.
            if (!unusedTypes.isEmpty()) {
              log.info("Specializing dependency " + dependencyCoordinates + ", removing " + unusedTypes.size() + "/" + types.getAllTypes().size() + " unused types.");
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

              // Create a new jar file with the debloated classes and move it to libs-specialized.
              Path libSpecializedPath = Paths.get("libs-specialized");
              String jarName = destDir.getName() + ".jar";
              File jarFile = libSpecializedPath.resolve(jarName).toFile();
              try {
                Files.createDirectories(libSpecializedPath); // create libs-deptrim directory if it does not exist
                se.kth.deptrim.util.JarUtils.createJarFromDirectory(destDir, jarFile);
              } catch (Exception e) {
                log.error("Error creating specialized jar for " + destDir.getName());
              }

              // Deploy specialized jars to the local Maven repository.
              try {
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
                SpecializedDependency specializedDependency = new SpecializedDependency(
                    dependency.getGroupId(),
                    dependency.getDependencyId(),
                    dependency.getVersion(),
                    GROUP_ID_OF_SPECIALIZED_JAR
                );
                deployedSpecializedDependencies.add(specializedDependency);
              } catch (IOException | InterruptedException e) {
                log.error("Error installing the specialized dependency JAR in the local repository.");
                Thread.currentThread().interrupt();
              }
            } else {
              log.info("Skipping specializing dependency " + dependencyCoordinates + " because all its types are used.");
            }
          }
        });
    return deployedSpecializedDependencies;
  }
}
