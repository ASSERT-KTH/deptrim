package se.kth.deptrim.mojo;

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
import se.kth.depclean.core.analysis.AnalysisFailureException;
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
   * Add a list of dependencies, identified by their coordinates, to be trimmed by DepTrim during the execution. The format of each dependency is <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "trimDependencies")
  private Set<String> trimDependencies;

  /**
   * If this is true, DepTrim creates version of the pom with the trimmed dependencies, called "trimmed-pom.xml", in root of the project.
   */
  @Parameter(property = "createPomTrimmed", defaultValue = "false")
  private boolean createPomTrimmed;

  /**
   * If this is true, DepTrim creates a JSON file with the result of the analysis. The file is called "trimming-result.json" and it is located in /target.
   */
  @Parameter(property = "createResultJson", defaultValue = "false")
  private boolean createResultJson;

  /**
   * If this is true, DepTrim creates a CSV file with the result of the analysis with the columns: OriginClass,TargetClass,OriginDependency,TargetDependency. The file is called deptrim-callgraph.csv"
   * and it is located in /target.
   */
  @Parameter(property = "createCallGraphCsv", defaultValue = "false")
  private boolean createCallGraphCsv;

  /**
   * Add a list of dependencies, identified by their coordinates, to be ignored by DepTrim during the analysis and considered as fully used dependencies. Useful to override incomplete result caused by
   * bytecode-level analysis. Dependency format is <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "ignoreDependencies")
  private Set<String> ignoreDependencies;

  /**
   * Ignore dependencies with specific scopes.
   */
  @Parameter(property = "ignoreScopes")
  private Set<String> ignoreScopes;

  /**
   * If this is true, DepTrim will not analyze the test sources in the project, and, therefore, the dependencies that are only used for testing will be considered as fully used. This property is
   * useful to  detect dependencies that have compilation scope but are only used during testing. Hence, these dependencies should have a test scope.
   */
  @Parameter(property = "ignoreTests", defaultValue = "false")
  private boolean ignoreTests;

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
          skipDepTrim,
          ignoreTests,
          ignoreScopes,
          ignoreDependencies,
          trimDependencies,
          createPomTrimmed,
          createResultJson,
          createCallGraphCsv
      ).execute();
    } catch (AnalysisFailureException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}