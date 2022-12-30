package se.kth.deptrim;

import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import se.kth.depclean.wrapper.MavenDependencyManager;

/**
 * This Maven mojo is the main class of DepTrim. DepTrim automatically removes unused types in the project's dependencies.
 */
@Mojo(name = "deptrim",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true)
@Slf4j
public class DepTrimMojo extends AbstractMojo {

  /**
   * The Maven project to analyze.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * The Maven session to analyze.
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  /**
   * Add a list of dependencies, identified by their coordinates, to be specialized by DepTrim during the execution. The format of each dependency is
   * <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "specializeDependencies")
  private Set<String> specializeDependencies;

  /**
   * If this is true, DepTrim creates aversion of the pom, named "pom-specialized.xml", in the root of the project.
   */
  @Parameter(property = "createSinglePomSpecialized", defaultValue = "false")
  private boolean createSinglePomSpecialized;
  /**
   * If this is true, DepTrim creates a version of the POM for each specialized dependency in the root of the project.
   */
  @Parameter(property = "createDependencySpecializedPerPom", defaultValue = "false")
  private boolean createDependencySpecializedPerPom;
  /**
   * If this is true, DepTrim creates all the combinations of the specialized poms in the root of the project.
   */
  @Parameter(property = "createAllPomSpecialized", defaultValue = "false")
  private boolean createAllPomSpecialized;

  /**
   * If this is true, DepTrim creates a JSON file with the result of the analysis. The file is called "deptrim-result.json" and it is located in /target.
   */
  @Parameter(property = "createResultJson", defaultValue = "false")
  private boolean createResultJson;

  /**
   * If this is true, DepTrim creates a CSV file with the result of the analysis with the columns: OriginClass,TargetClass,OriginDependency,TargetDependency.
   * The file is called deptrim-callgraph.csv" and it is located in /target.
   */
  @Parameter(property = "createCallGraphCsv", defaultValue = "false")
  private boolean createCallGraphCsv;

  /**
   * Add a list of dependencies, identified by their coordinates, to be ignored by DepTrim during the analysis and considered as fully used dependencies. Useful
   * to override incomplete result caused by bytecode-level analysis. Dependency format is <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "ignoreDependencies")
  private Set<String> ignoreDependencies;

  /**
   * Ignore dependencies with specific scopes.
   */
  @Parameter(property = "ignoreScopes")
  private Set<String> ignoreScopes;

  /**
   * Print plugin execution details to the console.
   */
  @Parameter(property = "verboseMode", defaultValue = "false")
  private boolean verboseMode;

  /**
   * Skip plugin execution completely.
   */
  @Parameter(property = "skipDepTrim", defaultValue = "false")
  private boolean skipDepTrim;

  /**
   * To build the dependency graph.
   */
  @Component(hint = "default")
  private DependencyGraphBuilder dependencyGraphBuilder;

  @SneakyThrows
  @Override
  public final void execute() {
    try {
      new DepTrimManager(
          new MavenDependencyManager(
              getLog(),
              project,
              session,
              dependencyGraphBuilder
          ),
          project,
          session,
          verboseMode,
          skipDepTrim,
          ignoreScopes,
          ignoreDependencies,
          specializeDependencies,
          createSinglePomSpecialized,
          createDependencySpecializedPerPom,
          createAllPomSpecialized
      ).execute();
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}