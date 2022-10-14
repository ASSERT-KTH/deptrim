package se.kth.deptrim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.xml.sax.SAXException;
import se.kth.depclean.core.DepCleanManager;
import se.kth.depclean.core.analysis.DependencyTypes;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.model.ClassName;
import se.kth.depclean.core.model.Dependency;
import se.kth.depclean.util.MavenInvoker;
import se.kth.depclean.wrapper.MavenDependencyManager;
import se.kth.deptrim.util.MagicStrings;

/**
 * This Mojo gets executed when the DepTrim plugin is invoked within a Maven project.
 * It relies on the DepClean Maven plugin to analyze said Maven project, and produce a pom-debloated.xml file.
 * Next, it removes unused types within the used dependencies of the project, produces specialized jars,
 * and deploys them to the local Maven repository. Finally, it produces a pom-debloated-specialized.xml
 * file, that replaces the original dependency with the specialized one.
 */
@Mojo(name = "deptrim", threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyCollection = ResolutionScope.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class DeptrimMojo extends AbstractMojo {
  /**
   * The Maven project to analyze.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject mavenProject;

  /**
   * The Maven session to analyze.
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession mavenSession;

  /**
   * DepClean will create a debloated version of the pom without unused dependencies, called
   * "debloated-pom.xml", in root of the project.
   */
  private final boolean createPomDebloated = true;

  /**
   * If this is true, DepClean creates a JSON file with the result of the analysis. The file is called
   * "debloat-result.json" and it is located in /target.
   */
  @Parameter(property = "createResultJson", defaultValue = "true")
  private boolean createResultJson;


  /**
   * If this is true, DepClean creates a CSV file with the result of the analysis with the columns:
   * OriginClass,TargetClass,OriginDependency,TargetDependency. The file is called "depclean-callgraph.csv" and it is located in /target.
   */
  @Parameter(property = "createCallGraphCsv", defaultValue = "false")
  private boolean createCallGraphCsv;

  /**
   * The coordinates of the dependency to specialize with DepTrim.
   * TODO: This will be expanded to accept multiple libraries
   */
  @Parameter(property = "libraryToSpecialize")
  private String libraryToSpecialize;

  /**
   * Add a list of dependencies, identified by their coordinates, to be ignored by DepClean during the analysis and
   * considered as used dependencies. Useful to override incomplete result caused by bytecode-level analysis Dependency
   * format is <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "ignoreDependencies")
  private Set<String> ignoreDependencies;

  /**
   * Ignore dependencies with specific scopes from the DepClean analysis.
   */
  @Parameter(property = "ignoreScopes")
  private Set<String> ignoreScopes;

  /**
   * If this is true, DepClean will not analyze the test sources in the project, and, therefore, the dependencies that
   * are only used for testing will be considered unused. This property is useful to detect dependencies that have a
   * compile scope but are only used during testing. Hence, these dependencies should have a test scope.
   */
  @Parameter(property = "ignoreTests", defaultValue = "false")
  private boolean ignoreTests;

  /**
   * If this is true, and DepClean reported any unused direct dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedDirect", defaultValue = "false")
  private boolean failIfUnusedDirect;

  /**
   * If this is true, and DepClean reported any unused transitive dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedTransitive", defaultValue = "false")
  private boolean failIfUnusedTransitive;

  /**
   * If this is true, and DepClean reported any unused inherited dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedInherited", defaultValue = "false")
  private boolean failIfUnusedInherited;

  /**
   * Skip plugin execution completely.
   */
  @Parameter(property = "skipDepClean", defaultValue = "false")
  private boolean skipDepClean;

  /**
   * To build the dependency graph.
   */
  @Component(hint = "default")
  private DependencyGraphBuilder dependencyGraphBuilder;

  /**
   * This variable holds the path to the directory with the
   * flattened dependency tree.
   */
  private String projectDependencyDirectory;

  /**
   * Prepares for generation of specialized jar, and actually generates the
   * specialized jar.
   *
   * @param dependencyIdAndVersion The id and version of the original dependency
   * @param specializedDirectory The path where the specialized jar will be generated
   * @throws IOException if something goes wrong while creating specialized jar
   * @throws InterruptedException if Maven deploy command is interrupted during execution
   */
  private String makeSpecializedJarInLocalMavenRepoAndReturnItsName(String dependencyIdAndVersion,
                                                                    File specializedDirectory) throws IOException, InterruptedException {
    // Get local maven repository: .m2/repository
    getLog().info("Local Maven repository: "
            + mavenSession.getLocalRepository().getBasedir());

    // Create .m2/repository/specialized-by-deptrim directory
    String specializedDependencyDirectoryInLocalMavenRepo =
            mavenSession.getLocalRepository().getBasedir() + File.separator
                    + MagicStrings.deptrimSpecializedJarDirectoryInLocalMavenRepo;
    Files.createDirectories(Paths.get(specializedDependencyDirectoryInLocalMavenRepo));

    // Create a name for specialized jar with details of this project
    String mavenProjectIdentifier = (mavenProject.getGroupId() + "-" + mavenProject.getArtifactId() + "-"
            + mavenProject.getVersion()).replaceAll("\\s", "-");
    String outputJarName = (dependencyIdAndVersion + "-spl-" + mavenProjectIdentifier)
            .replaceAll("\\.", "-");

    // The specialized jar gets created in the .m2/repository/specialized-by-deptrim
    String fullOutputJarPath = specializedDependencyDirectoryInLocalMavenRepo
            + File.separator + outputJarName + ".jar";

    // Make the specialized jar
    JarMaker jarMaker = new JarMaker();
    jarMaker.make(specializedDirectory.getAbsolutePath(), fullOutputJarPath);
    getLog().info("New specialized jar: " + fullOutputJarPath);

    // It is not enough to generate specialized jar in local maven repo
    // It must be deployed to local maven repo so that it can be added as a dependency
    // in debloated-pom.xml
    // see https://github.com/apache/maven-deploy-plugin
    // and https://sookocheff.com/post/java/local-maven-repository/
    // can invoke cli
    // mvn deploy:deploy-file -Durl=file:///path/to/repo/ -Dfile=spl.jar
    // -DgroupId=se.kth.deptrim.spl -DartifactId=spl-dep-wrt-project -Dpackaging=jar -Dversion=1.0

    String deployCmd = String.format(
            "mvn deploy:deploy-file -Durl=%s -Dfile=%s "
                    + "-DgroupId=%s -DartifactId=%s -Dpackaging=jar -Dversion=%s",
            mavenSession.getLocalRepository().getUrl(),
            fullOutputJarPath,
            MagicStrings.deptrimSpecializedGroupId,
            outputJarName,
            MagicStrings.deptrimSpecializedVersion);
    getLog().info("Deploying specialized jar to local Maven repository: " + deployCmd);
    MavenInvoker.runCommand(deployCmd, null);
    return outputJarName;
  }

  /**
   * Creates a trimmed directory for a used dependency with unused types removed.
   * Uses the contents of this new directory to create a specialized jar.
   *
   * @param dependencyAndUnusedTypesMap A map of Dependency and a list of ClassNames for unused types within it
   * @throws Exception if something goes wrong while copying original dependency directory, etc
   */
  private void deleteUnusedClassFilesAndCreateSpecializedJar(
          Map<Dependency, List<ClassName>> dependencyAndUnusedTypesMap) throws Exception {
    File dir = new File(projectDependencyDirectory);
    // For each dependency and its unused types
    for (Map.Entry<Dependency, List<ClassName>> entry : dependencyAndUnusedTypesMap.entrySet()) {
      String dependencyIdAndVersion = entry.getKey().getDependencyId() + "-"
              + entry.getKey().getVersion();
      getLog().info("Working with dependency " + dependencyIdAndVersion);
      // Find the unique directory corresponding to this dependency
      Optional<File> dependencyFiles = Arrays.stream(
              Objects.requireNonNull(dir.listFiles((d, name) ->
                      name.startsWith(dependencyIdAndVersion)))).findFirst();
      if (dependencyFiles.isEmpty()) {
        continue;
      }
      File usedDependencyDirectory = dependencyFiles.get();
      getLog().info("usedDependencyDirectory: ");
      getLog().info(usedDependencyDirectory.getAbsolutePath());

      getLog().info("Will create specialized directory: "
              + usedDependencyDirectory.getAbsolutePath() + "-spl");
      File specializedDirectory = new File(usedDependencyDirectory.getAbsolutePath() + "-spl");
      specializedDirectory.mkdirs();

      Collection<File> allFilesOfDependency =
              FileUtils.listFiles(usedDependencyDirectory, null, true);
      getLog().info("Number of files in original library: "
              + allFilesOfDependency.size());

      // Copy the entire original directory of the dependency
      FileUtils.copyDirectory(usedDependencyDirectory, specializedDirectory);

      // But remove unused types
      for (ClassName unusedType : entry.getValue()) {
        File toDelete = new File(specializedDirectory + File.separator
                + unusedType.toString().replaceAll("\\.", File.separator) + ".class");
        boolean isDeleted = toDelete.delete();
        if (isDeleted) {
          getLog().debug("Deleted " + toDelete.getAbsolutePath());
        } else {
          getLog().error("Could not delete " + toDelete.getAbsolutePath());
        }
      }
      getLog().info("Number of files in specialized library: "
              + FileUtils.listFiles(specializedDirectory, null, true).size());

      // Make specialized jar for this dependency, with the contents of the specialized directory
      String specializedJarName =
              makeSpecializedJarInLocalMavenRepoAndReturnItsName(dependencyIdAndVersion,
                      specializedDirectory);


      // Replace original jar with spl jar in project debloated-pom
      PomManipulator pomManipulator = new PomManipulator();
      // DepClean creates debloated pom in base directory
      // see se.kth.depclean.util.MavenDebloater.writeFile
      String debloatedPomPath = mavenProject.getBasedir().getAbsolutePath()
              + File.separator + MagicStrings.depCleanDebloatedPomName;
      getLog().info("Debloated + specialized POM: "
              + pomManipulator.createSpecializedPomFromDebloatedPom(debloatedPomPath,
                      entry.getKey().getGroupId(),
                      entry.getKey().getDependencyId(),
                      specializedJarName));
    }

  }

  /**
   * Creates a map of dependencies and the types within them that are not used by this project.
   *
   * @param analysis The ProjectDependencyAnalysis object generated by DepClean for a project.
   * @return a map of Dependency and a list of ClassName that are unused within this dependency.
   */
  private Map<Dependency, List<ClassName>> getUnusedTypesWithinUsedDependency(ProjectDependencyAnalysis analysis) {
    Map<Dependency, List<ClassName>> dependencyAndListOfUnusedTypes = new LinkedHashMap<>();
    // Get all used dependencies
    Set<Dependency> allUsedDependencies = new LinkedHashSet<>();
    allUsedDependencies.addAll(analysis.getUsedDirectDependencies());
    allUsedDependencies.addAll(analysis.getUsedTransitiveDependencies());
    allUsedDependencies.addAll(analysis.getUsedInheritedDependencies());
    getLog().info("Number of used direct, inherited, or transitive dependencies: "
            + allUsedDependencies.size());

    // For each used dependency to specialize, get its unused types (classes)
    for (Map.Entry<Dependency, DependencyTypes> entry : analysis.getDependencyClassesMap().entrySet()) {
      if (entry.getKey().getDependencyId().equals(libraryToSpecialize)) {
        getLog().info("Number of classes within " + libraryToSpecialize + ": "
                + entry.getValue().getAllTypes().size());
        getLog().info("Number of used classes within " + libraryToSpecialize + ": "
                + entry.getValue().getUsedTypes().size());
        List<ClassName> unusedTypes = new ArrayList<>(entry.getValue().getAllTypes());
        List<ClassName> usedTypes = new ArrayList<>(entry.getValue().getUsedTypes());
        unusedTypes.removeAll(usedTypes);
        getLog().info("Number of unused classes within " + libraryToSpecialize + ": "
                + unusedTypes.size());
        getLog().info("==== UNUSED TYPES WITHIN " + libraryToSpecialize + ": " + unusedTypes);
        dependencyAndListOfUnusedTypes.put(entry.getKey(), unusedTypes);
      }
    }
    return dependencyAndListOfUnusedTypes;
  }

  @Override
  public void execute() {
    getLog().info("=== DepTrim Maven Plugin ===");
    if (libraryToSpecialize == null) {
      getLog().info("No library to specialize, will run DepClean and exit");
    } else {
      getLog().info("Attempting to specialize " + libraryToSpecialize);
    }
    getLog().info("I am running on " + mavenProject.getGroupId() + ":"
            + mavenProject.getArtifactId() + ":" + mavenProject.getVersion());
    getLog().info("This project has the following dependencies: "
            + mavenProject.getDependencies().toString());
    ProjectDependencyAnalysis analysis;
    try {
      analysis = new DepCleanManager(
              new MavenDependencyManager(
                      getLog(),
                      mavenProject,
                      mavenSession,
                      dependencyGraphBuilder
              ),
              skipDepClean,
              ignoreTests,
              ignoreScopes,
              ignoreDependencies,
              failIfUnusedDirect,
              failIfUnusedTransitive,
              failIfUnusedInherited,
              createPomDebloated,
              createResultJson,
              createCallGraphCsv
      ).execute();
      analysis.print();
      if (libraryToSpecialize != null) {
        // ./target/classes
        String buildOutputDirectory = mavenProject.getBuild().getOutputDirectory();
        projectDependencyDirectory = buildOutputDirectory.replace(
                File.separator + MagicStrings.mavenBuildClassesDirectory,
                File.separator + MagicStrings.mavenBuildDependencyDirectory);
        // ./target/dependency
        getLog().info("Dependency directory " + projectDependencyDirectory);

        // We create a debloated pom with DepClean, which discards unused dependencies
        // Then we only specialize used dependencies
        Map<Dependency, List<ClassName>> dependencyAndListOfUnusedTypesMap =
                getUnusedTypesWithinUsedDependency(analysis);
        deleteUnusedClassFilesAndCreateSpecializedJar(dependencyAndListOfUnusedTypesMap);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
