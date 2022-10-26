package se.kth.deptrim.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import se.kth.depclean.core.analysis.model.ProjectDependencyAnalysis;
import se.kth.depclean.core.wrapper.DependencyManagerWrapper;

/**
 * A class that writes the results of the DepClean dependency usage analysis to a JSON file.
 */
@Slf4j
public class JsonFile {

  /**
   * Writes the results of the DepClean dependency usage analysis to a JSON file.
   *
   * @param analysis           the analysis results of each dependency.
   * @param dependencyManager  the dependency manager wrapper.
   * @param createCallGraphCsv true if a callgraph file will be created.
   */
  public void createResultJson(ProjectDependencyAnalysis analysis, DependencyManagerWrapper dependencyManager, boolean createCallGraphCsv) {
    log.info("Creating depclean-results.json, please wait...");
    final File jsonFile = new File(dependencyManager.getBuildDirectory() + File.separator + "depclean-results.json");
    final File treeFile = new File(dependencyManager.getBuildDirectory() + File.separator + "tree.txt");
    final File csvFile = new File(dependencyManager.getBuildDirectory() + File.separator + "depclean-callgraph.csv");
    try {
      dependencyManager.generateDependencyTree(treeFile);
    } catch (IOException | InterruptedException e) {
      log.error("Unable to generate dependency tree.");
      // Restore interrupted state...
      Thread.currentThread().interrupt();
      return;
    }
    if (createCallGraphCsv) {
      log.info("Creating " + csvFile.getName() + ", please wait...");
      try {
        FileUtils.write(csvFile, "OriginClass,TargetClass,OriginDependency,TargetDependency\n", Charset.defaultCharset());
      } catch (IOException e) {
        log.error("Error writing the CSV header.");
      }
    }
    String treeAsJson = dependencyManager.getTreeAsJson(
        treeFile,
        analysis,
        csvFile,
        createCallGraphCsv
    );

    try {
      FileUtils.write(jsonFile, treeAsJson, Charset.defaultCharset());
    } catch (IOException e) {
      log.error("Unable to generate " + jsonFile.getName() + " file.");
    }
    if (jsonFile.exists()) {
      log.info(jsonFile.getName() + " file created in: " + jsonFile.getAbsolutePath());
    }
    if (csvFile.exists()) {
      log.info(csvFile.getName() + " file created in: " + csvFile.getAbsolutePath());
    }
  }

}
