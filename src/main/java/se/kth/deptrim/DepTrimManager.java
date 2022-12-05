package se.kth.deptrim;

import java.io.File;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import se.kth.depclean.core.analysis.DefaultProjectDependencyAnalyzer;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.model.ProjectContext;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;
import se.kth.depclean.core.wrapper.LogWrapper;
import se.kth.deptrim.core.SpecializedDependency;
import se.kth.deptrim.core.Specializer;
import se.kth.deptrim.core.TypesExtractor;
import se.kth.deptrim.core.TypesUsageAnalyzer;
import se.kth.deptrim.io.ConsolePrinter;
import se.kth.deptrim.util.PomUtils;
import se.kth.deptrim.util.TimeUtils;

/**
 * Runs the DepTrim process, regardless of a specific dependency manager.
 */
@AllArgsConstructor
public class DepTrimManager {

  private static final String SEPARATOR = "-------------------------------------------------------";
  private static final String DEBLOATED_POM_NAME = "pom-debloated.xml";
  private final DependencyManagerWrapper dependencyManager;
  private final MavenProject project;
  private final MavenSession session;
  private final boolean verboseMode;
  private final boolean skipDepTrim;
  private final Set<String> ignoreScopes;
  private final Set<String> ignoreDependencies;
  private final Set<String> specializeDependencies;
  private final boolean createPomSpecialized;
  private final boolean createAllPomSpecialized;

  /**
   * Execute the DepTrim manager.
   */
  @SneakyThrows
  public ProjectDependencyAnalysis execute() {
    final long startTime = System.currentTimeMillis();

    // Skip DepTrim if the user has specified so.
    if (skipDepTrim) {
      getLog().info("Skipping DepTrim plugin execution.");
      return null;
    }
    // Skip the execution if the packaging is not a JAR or WAR.
    if (dependencyManager.isMaven() && dependencyManager.isPackagingPom()) {
      getLog().info("Skipping DepTrim because the packaging type is pom.");
      return null;
    }

    getLog().info(SEPARATOR);
    getLog().info("DEPTRIM IS ANALYZING DEPENDENCIES");
    getLog().info(SEPARATOR);
    // Extract all the dependencies in target/dependencies/.
    TypesExtractor typesExtractor = new TypesExtractor(dependencyManager);
    typesExtractor.extractAllTypes();
    // Analyze the dependencies extracted.
    DefaultProjectDependencyAnalyzer projectDependencyAnalyzer = new DefaultProjectDependencyAnalyzer();
    TypesUsageAnalyzer typesUsageAnalyzer = new TypesUsageAnalyzer(dependencyManager);
    ProjectContext projectContext = typesUsageAnalyzer.buildProjectContext(ignoreDependencies, ignoreScopes);
    ProjectDependencyAnalysis analysis = projectDependencyAnalyzer.analyze(projectContext);

    if (verboseMode) {
      ConsolePrinter consolePrinter = new ConsolePrinter();
      consolePrinter.printDependencyUsageAnalysis(analysis);
    }

    // Specializing dependencies.
    getLog().info(SEPARATOR);
    getLog().info("DEPTRIM IS SPECIALIZING DEPENDENCIES");
    getLog().info(SEPARATOR);
    String projectCoordinates = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    String mavenLocalRepoUrl = session.getLocalRepository().getUrl();
    Specializer specializer = new Specializer(projectCoordinates, mavenLocalRepoUrl, dependencyManager, ignoreScopes);
    Set<SpecializedDependency> specializedDependencies = specializer.specialize(analysis, specializeDependencies);
    getLog().info("Number of specialized dependencies: " + specializedDependencies.size());
    getLog().info(SEPARATOR);
    getLog().info("DEPTRIM IS CREATING SPECIALIZED POMS");
    getLog().info(SEPARATOR);

    if (createPomSpecialized || createAllPomSpecialized) {
      // The following code creates a pom-debloated.xml
      dependencyManager.getDebloater(analysis).write();
      String debloatedPomPath = project.getBasedir().getAbsolutePath()
          + File.separator
          + DEBLOATED_POM_NAME;
      // The following code creates a pom-specialized-*.xml from pom-debloated.xml
      PomUtils pomUtils = new PomUtils(specializedDependencies, debloatedPomPath, createPomSpecialized, createAllPomSpecialized);
      pomUtils.createPoms();
    }

    // Print execution time.
    final long stopTime = System.currentTimeMillis();
    TimeUtils timeUtils = new TimeUtils();
    getLog().info("DepTrim execution done in " + timeUtils.toHumanReadableTime(stopTime - startTime));

    return analysis;
  }

  private LogWrapper getLog() {
    return dependencyManager.getLog();
  }
}