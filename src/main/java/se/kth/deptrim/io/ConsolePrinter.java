package se.kth.deptrim.io;

import java.util.HashSet;
import java.util.Set;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.model.ClassName;

/**
 * A class that prints messages to the console.
 */
public class ConsolePrinter {

  /**
   * Prints the results of the DepClean dependency usage analysis.
   *
   * @param projectDependencyAnalysis the analysis results of each dependency.
   */
  public void printDependencyUsageAnalysis(ProjectDependencyAnalysis projectDependencyAnalysis) {
    printString("ALL TYPES");
    projectDependencyAnalysis.getDependencyClassesMap().forEach((key, value) -> printString(key.getFile().getName() + " -> " + value.getAllTypes()));
    printString("USED TYPES");
    projectDependencyAnalysis.getDependencyClassesMap().forEach((key, value) -> printString(key.getFile().getName() + " -> " + value.getUsedTypes()));
    printString("UNUSED TYPES");
    projectDependencyAnalysis.getDependencyClassesMap().forEach((key, value) -> {
      Set<ClassName> tmp = new HashSet<>();
      tmp.addAll(value.getAllTypes());
      tmp.removeAll(value.getUsedTypes());
      printString(key.getFile().getName() + " -> " + tmp);
    });
  }

  /**
   * Prints a string to the console.
   *
   * @param string the string to print.
   */
  private void printString(final String string) {
    System.out.println(string); //NOSONAR avoid a warning of non-used logger
  }
}
