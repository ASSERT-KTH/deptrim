package se.kth.deptrim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import se.kth.depclean.core.analysis.AnalysisFailureException;
import se.kth.depclean.core.analysis.DefaultProjectDependencyAnalyzer;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.model.ClassName;
import se.kth.depclean.core.model.Dependency;
import se.kth.depclean.core.model.ProjectContext;
import se.kth.depclean.core.model.Scope;
import se.kth.depclean.core.util.JarUtils;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;
import se.kth.depclean.core.wrapper.LogWrapper;
import se.kth.depclean.util.MavenInvoker;
import se.kth.deptrim.io.ConsolePrinter;
import se.kth.deptrim.util.TimeUtils;

/**
 * Runs the DepTrim process, regardless of a specific dependency manager.
 */
@AllArgsConstructor
public class DepTrimManager {

  private static final String SEPARATOR = "-------------------------------------------------------";
  private static final String DIRECTORY_TO_EXTRACT_DEPENDENCIES = "dependency";
  private static final String DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES = "dependency-debloated";
  private final DependencyManagerWrapper dependencyManager;
  private final boolean skipDepTrim;
  private final boolean ignoreTests;
  private final Set<String> ignoreScopes;
  private final Set<String> ignoreDependencies;
  private final Set<String> trimDependencies;
  private final boolean createPomTrimmed;
  private final boolean createResultJson;
  private final boolean createCallGraphCsv;

  /**
   * Execute the DepTrim manager.
   */
  @SneakyThrows
  public ProjectDependencyAnalysis execute() throws AnalysisFailureException {
    final long startTime = System.currentTimeMillis();

    // Skip DepTrim if the user has specified so.
    if (skipDepTrim) {
      getLog().info("Skipping DepTrim plugin execution");
      return null;
    }

    getLog().info(SEPARATOR);
    getLog().info("Starting DepTrim dependency analysis");

    // Skip the execution if the packaging is not a JAR or WAR.
    if (dependencyManager.isMaven() && dependencyManager.isPackagingPom()) {
      getLog().info("Skipping DepTrim because the packaging type is pom");
      return null;
    }

    // Extract all the dependencies in target/dependencies.
    extractLibClasses();

    // Analyze the dependencies extracted.
    getLog().info("Analyzing dependencies...");
    final DefaultProjectDependencyAnalyzer projectDependencyAnalyzer = new DefaultProjectDependencyAnalyzer();
    final ProjectDependencyAnalysis analysis = projectDependencyAnalyzer.analyze(buildProjectContext());
    ConsolePrinter consolePrinter = new ConsolePrinter();
    consolePrinter.printDependencyUsageAnalysis(analysis);

    // Trimming dependencies.
    getLog().info("STARTING TRIMMING DEPENDENCIES");
    trimLibClasses(analysis, trimDependencies);
    consolePrinter.printDependencyUsageAnalysis(analysis);

    // Print execution time.
    final long stopTime = System.currentTimeMillis();
    TimeUtils timeUtils = new TimeUtils();
    getLog().info("DepTrim execution done in " + timeUtils.toHumanReadableTime(stopTime - startTime));

    return analysis;
  }

  @SneakyThrows
  private void extractLibClasses() {
    final File dependencyDirectory =
        dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES).toFile();
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
        getLog().error("Error copying directory libs to dependency");
        throw new RuntimeException(e);
      }
    }

    /* Decompress dependencies */
    if (dependencyDirectory.exists()) {
      JarUtils.decompress(dependencyDirectory.getAbsolutePath());
    }
  }

  /**
   * Trim the unused classes from the dependencies specified by the user based on the usage analysis results.
   *
   * @param analysis         The dependency usage analysis results
   * @param trimDependencies The dependencies to be trimmed, if empty then trims all the dependencies.
   */
  @SneakyThrows
  private void trimLibClasses(ProjectDependencyAnalysis analysis, Set<String> trimDependencies) {
    analysis
        .getDependencyClassesMap()
        .forEach((key, value) -> {
          String dependencyCoordinates = key.getGroupId() + ":" + key.getDependencyId() + ":" + key.getVersion();
          // debloating only the dependencies provided by the user and if the scope is not ignored
          if (trimDependencies.contains(dependencyCoordinates) && !ignoreScopes.contains(key.getScope())) {
            getLog().info("Trimming dependency " + dependencyCoordinates);
            Set<ClassName> unusedTypes = new HashSet<>(value.getAllTypes());
            unusedTypes.removeAll(value.getUsedTypes());
            getLog().info(key.getFile().getName() + " -> " + unusedTypes);
            String dependencyDirName = key.getFile().getName().substring(0, key.getFile().getName().length() - 4);
            File srcDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_EXTRACT_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            File destDir = dependencyManager.getBuildDirectory().resolve(DIRECTORY_TO_LOCATE_THE_DEBLOATED_DEPENDENCIES + File.separator + dependencyDirName).toFile();
            getLog().info("copying from files from " + srcDir.getAbsolutePath() + " to " + destDir.getAbsolutePath());

            // Copy all files from srcDir to destDir
            try {
              FileUtils.copyDirectory(srcDir, destDir);
            } catch (IOException e) {
              getLog().error("Error copying files from " + srcDir + " to " + destDir);
            }
            // Remove files in destDir.
            for (ClassName className : unusedTypes) {
              String fileName = className.toString().replace(".", File.separator) + ".class";
              File file = new File(destDir.getAbsolutePath() + File.separator + fileName);
              getLog().info("Removing file " + file.getAbsolutePath());
              file.delete();
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
              getLog().error("Error creating trimmed jar for " + destDir.getName());
            }

            // Install the dependency in the local repository.
            try {
              MavenInvoker.runCommand(
                  "mvn deploy:deploy-file -Durl=" + libDeptrimPath
                      + " -Dpackaging=jar"
                      + " -Dfile=" + jarFile.getAbsolutePath()
                      + " -DgroupId=" + key.getGroupId()
                      + " -DartifactId=" + key.getDependencyId()
                      + " -Dversion=" + key.getVersion(),
                  null);
            } catch (IOException | InterruptedException e) {
              getLog().error("Error installing the trimmed dependency jar in local repo");
            }
          }
        });
  }

  private void copyDependencies(Dependency dependency, File destFolder) {
    copyDependencies(dependency.getFile(), destFolder);
  }

  @SneakyThrows
  private void copyDependencies(File jarFile, File destFolder) {
    FileUtils.copyFileToDirectory(jarFile, destFolder);
  }

  private ProjectContext buildProjectContext() {
    if (ignoreTests) {
      ignoreScopes.add("test");
    }

    // Consider are used all the classes declared in Maven processors
    Set<ClassName> allUsedClasses = new HashSet<>();
    Set<ClassName> usedClassesFromProcessors = dependencyManager
        .collectUsedClassesFromProcessors().stream()
        .map(ClassName::new)
        .collect(Collectors.toSet());

    // Consider as used all the classes located in the imports of the source code
    Set<ClassName> usedClassesFromSource = dependencyManager.collectUsedClassesFromSource(
            dependencyManager.getSourceDirectory(),
            dependencyManager.getTestDirectory())
        .stream()
        .map(ClassName::new)
        .collect(Collectors.toSet());

    allUsedClasses.addAll(usedClassesFromProcessors);
    allUsedClasses.addAll(usedClassesFromSource);

    return new ProjectContext(
        dependencyManager.dependencyGraph(),
        dependencyManager.getOutputDirectories(),
        dependencyManager.getTestOutputDirectories(),
        dependencyManager.getSourceDirectory(),
        dependencyManager.getTestDirectory(),
        dependencyManager.getDependenciesDirectory(),
        ignoreScopes.stream().map(Scope::new).collect(Collectors.toSet()),
        toDependency(dependencyManager.dependencyGraph().allDependencies(), ignoreDependencies),
        allUsedClasses
    );
  }

  /**
   * Returns a set of {@code DependencyCoordinate}s that match given string representations.
   *
   * @param allDependencies    all known dependencies
   * @param ignoreDependencies string representation of dependencies to return
   * @return a set of {@code Dependency} that match given string representations
   */
  private Set<Dependency> toDependency(Set<Dependency> allDependencies, Set<String> ignoreDependencies) {
    return ignoreDependencies.stream()
        .map(dependency -> findDependency(allDependencies, dependency))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Dependency findDependency(Set<Dependency> allDependencies, String dependency) {
    return allDependencies.stream()
        .filter(dep -> dep.toString().toLowerCase().contains(dependency.toLowerCase()))
        .findFirst()
        .orElse(null);
  }

  private LogWrapper getLog() {
    return dependencyManager.getLog();
  }
}