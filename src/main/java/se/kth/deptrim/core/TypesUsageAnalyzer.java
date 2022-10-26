package se.kth.deptrim.core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import se.kth.depclean.core.model.ClassName;
import se.kth.depclean.core.model.Dependency;
import se.kth.depclean.core.model.ProjectContext;
import se.kth.depclean.core.model.Scope;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;

/**
 * A class that analyses the types used by the project in each dependency.
 */
public class TypesUsageAnalyzer {

  private DependencyManagerWrapper dependencyManager;

  /**
   * Constructor.
   *
   * @param dependencyManager the dependency manager.
   */
  public TypesUsageAnalyzer(DependencyManagerWrapper dependencyManager) {
    this.dependencyManager = dependencyManager;
  }

  /**
   * Creates a project dependency analysis.
   *
   * @return the {@link ProjectContext} with the types used by the project in each dependency.
   */
  public ProjectContext buildProjectContext(boolean ignoreTests, Set<String> ignoreDependencies, Set<String> ignoreScopes) {
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

}
