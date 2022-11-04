package se.kth.deptrim;

import java.io.File;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import se.kth.depclean.core.analysis.DefaultProjectDependencyAnalyzer;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;
import se.kth.depclean.core.wrapper.LogWrapper;
import se.kth.deptrim.core.DependencyOriginalAndTrimmed;
import se.kth.deptrim.core.Trimmer;
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
  public ProjectDependencyAnalysis execute() {
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
    TypesExtractor typesExtractor = new TypesExtractor(dependencyManager);
    typesExtractor.extractAllTypes();

    // Analyze the dependencies extracted.
    getLog().info("Analyzing dependencies...");
    TypesUsageAnalyzer typesUsageAnalyzer = new TypesUsageAnalyzer(dependencyManager);
    final DefaultProjectDependencyAnalyzer projectDependencyAnalyzer = new DefaultProjectDependencyAnalyzer();
    final ProjectDependencyAnalysis analysis = projectDependencyAnalyzer.analyze(typesUsageAnalyzer.buildProjectContext(ignoreTests, ignoreDependencies, ignoreScopes));
    ConsolePrinter consolePrinter = new ConsolePrinter();
    // consolePrinter.printDependencyUsageAnalysis(analysis);

    // Trimming dependencies.
    getLog().info("STARTING TRIMMING DEPENDENCIES");
    Trimmer trimmer = new Trimmer(dependencyManager, ignoreScopes);
    String mavenLocalRepoUrl = session.getLocalRepository().getUrl();
    String thisProjectCoordinates = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    Set<DependencyOriginalAndTrimmed> originalAndTrimmedDependencies =
        trimmer.trimLibClasses(analysis, trimDependencies, thisProjectCoordinates, mavenLocalRepoUrl);

    // If POMs with specialized jars have to be produced
    if (createPomTrimmed) {
      // create pom-debloated.xml
      dependencyManager.getDebloater(analysis).write();
      String debloatedPomPath = project.getBasedir().getAbsolutePath()
          + File.separator
          + DEBLOATED_POM_NAME;
      // create pom-debloated-spl-*.xml
      PomUtils pomUtils = new PomUtils(originalAndTrimmedDependencies, debloatedPomPath);
      pomUtils.producePoms();
    }

    // consolePrinter.printDependencyUsageAnalysis(analysis);

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