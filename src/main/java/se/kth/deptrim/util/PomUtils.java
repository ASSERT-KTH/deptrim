package se.kth.deptrim.util;

import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.kth.deptrim.core.DependencyOriginalAndTrimmed;

/**
 * Utility class for manipulating Maven pom.xml files.
 */
@Slf4j
public class PomUtils {
  Set<DependencyOriginalAndTrimmed> originalTrimmedDependencies;
  String debloatedPomPath;

  public PomUtils(Set<DependencyOriginalAndTrimmed> originalAndTrimmedDependencies,
                  String debloatedPomPath) {
    this.originalTrimmedDependencies = originalAndTrimmedDependencies;
    this.debloatedPomPath = debloatedPomPath;
  }

  /**
   * This method produces a new pom file for each combination of trimmed dependencies.
   */
  public void producePoms() {
    Set<Set<DependencyOriginalAndTrimmed>> allCombinationsOfTrimmedDependencies = Sets.powerSet(originalTrimmedDependencies);
    log.info("Power set of trimmed dependencies: " + allCombinationsOfTrimmedDependencies);
    log.info("Number of combinations: " + allCombinationsOfTrimmedDependencies.size());

    for (Set<DependencyOriginalAndTrimmed> oneCombinationOfOriginalAndTrimmedDependency : allCombinationsOfTrimmedDependencies) {
      log.info("Producing POM for combination");
      oneCombinationOfOriginalAndTrimmedDependency.forEach(c -> log.info(c.toString()));
      try {
        String generatedPomFile = createSpecializedPomFromDebloatedPom(oneCombinationOfOriginalAndTrimmedDependency);
        log.info("Produced " + generatedPomFile);
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Error producing POM");
      }
    }
  }

  /**
   * Creates a specialized pom from the debloated pom produced by DepClean.
   *
   * @param combinationOfOriginalTrimmedDependencies A combination of trimmed dependencies
   * @return The path of the generated pom-debloated-spl.xml file
   * @throws Exception when any of this goes wrong :)
   */
  public String createSpecializedPomFromDebloatedPom(Set<DependencyOriginalAndTrimmed> combinationOfOriginalTrimmedDependencies) throws Exception {
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = builder.parse(new File(debloatedPomPath));
    document.getDocumentElement().normalize();
    NodeList dependencies = document.getDocumentElement().getElementsByTagName("dependency");

    String debloatedAndSpecializedPom = debloatedPomPath.replace(".xml", "-spl.xml");
    StringBuilder pomName = new StringBuilder();

    for (DependencyOriginalAndTrimmed thisDependency : combinationOfOriginalTrimmedDependencies) {
      pomName.append(pomName).append("-").append(thisDependency.getOriginalDependencyId());
      for (int i = 0; i < dependencies.getLength(); i++) {
        Element dependencyNode = (Element) dependencies.item(i);
        Node groupIdNode = dependencyNode.getElementsByTagName("groupId").item(0);
        Node artifactIdNode = dependencyNode.getElementsByTagName("artifactId").item(0);
        // When original groupId and artifactId are found in debloated pom,
        // replace with new coordinates
        if (groupIdNode.getTextContent().equals(thisDependency.getOriginalGroupId())
                & artifactIdNode.getTextContent().equals(thisDependency.getOriginalDependencyId())) {
          log.info("Found original dependency in debloated POM");
          log.info("Replacing with specialized dependency");
          Node versionNode = dependencyNode.getElementsByTagName("version").item(0);
          groupIdNode.setTextContent(thisDependency.getTrimmedGroupId());
          artifactIdNode.setTextContent(thisDependency.getTrimmedDependencyId());
          versionNode.setTextContent(thisDependency.getTrimmedVersion());
        }
      }
    }

    debloatedAndSpecializedPom = debloatedAndSpecializedPom.replace(".xml", pomName + ".xml");
    saveUpdatedDomInANewPom(document, debloatedAndSpecializedPom);
    return debloatedAndSpecializedPom;
  }

  /**
   * Generates a new XML file based with the changes to the XML document.
   *
   * @param document The XML document structure to save to file
   * @param debloatedSpecializedPom The path to the pom-debloated-spl.xml file to generate
   * @throws TransformerException If XML document cannot be saved to file
   */
  private void saveUpdatedDomInANewPom(Document document,
                                       String debloatedSpecializedPom) throws TransformerException {
    DOMSource dom = new DOMSource(document);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();

    StreamResult result = new StreamResult(new File(debloatedSpecializedPom));
    transformer.transform(dom, result);
  }
}
