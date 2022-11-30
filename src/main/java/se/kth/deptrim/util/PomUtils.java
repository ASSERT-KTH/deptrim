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
import se.kth.deptrim.core.TrimmedDependency;

/**
 * Utility class for manipulating Maven pom.xml files.
 */
@Slf4j
public class PomUtils {

  Set<TrimmedDependency> trimmedDependencies;
  String debloatedPomPath;

  boolean createPomTrimmed;
  boolean createAllPomsTrimmed;

  /**
   * Constructor.
   *
   * @param trimmedDependencies  The set of trimmed dependencies.
   * @param debloatedPomPath     The path to the debloated pom.xml file.
   * @param createAllPomsTrimmed Whether to create all pom.xml files trimmed.
   */
  public PomUtils(Set<TrimmedDependency> trimmedDependencies, String debloatedPomPath, boolean createdPomTrimmed, boolean createAllPomsTrimmed) {
    this.trimmedDependencies = trimmedDependencies;
    this.debloatedPomPath = debloatedPomPath;
    this.createPomTrimmed = createdPomTrimmed;
    this.createAllPomsTrimmed = createAllPomsTrimmed;
  }

  /**
   * This method produces a new pom file for each combination of trimmed dependencies.
   */
  public void producePoms() {
    if (createAllPomsTrimmed) {
      produceAllCombinationsOfPomsTrimmed();
    }
    if (createPomTrimmed) {
      createSinglePomTrimmed();
    }
  }

  private void createSinglePomTrimmed() {
    try {
      String generatedPomFile = createSpecializedPomFromDebloatedPom(trimmedDependencies, 1);
      log.info("Produced " + new File(generatedPomFile).getName());
    } catch (Exception e) {
      log.error("Error producing specialized POM");
    }
  }

  private void produceAllCombinationsOfPomsTrimmed() {
    Set<Set<TrimmedDependency>> allCombinationsOfTrimmedDependencies = Sets.powerSet(trimmedDependencies);
    log.info("Number of specialized poms: " + allCombinationsOfTrimmedDependencies.size());
    int combinationNumber = 1;
    for (Set<TrimmedDependency> oneCombinationOfTrimmedDependencies : allCombinationsOfTrimmedDependencies) {
      // Producing POM for combination.
      // oneCombinationOfTrimmedDependencies.forEach(c -> log.info(c.toString()));
      try {
        String generatedPomFile = createSpecializedPomFromDebloatedPom(oneCombinationOfTrimmedDependencies, combinationNumber);
        log.info("Produced " + new File(generatedPomFile).getName());
        combinationNumber++;
      } catch (Exception e) {
        log.error("Error producing specialized POM");
      }
    }
  }

  /**
   * Creates a specialized pom from the debloated pom produced by DepClean.
   *
   * @param oneCombinationOfTrimmedDependencies A combination of trimmed dependencies
   * @return The path of the generated pom-debloated-spl.xml file
   * @throws Exception when any of this goes wrong :)
   */
  private String createSpecializedPomFromDebloatedPom(Set<TrimmedDependency> oneCombinationOfTrimmedDependencies, Integer combinationNumber) throws Exception {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new File(debloatedPomPath));
    document.getDocumentElement().normalize();
    int numberOfTrimmedDependencies = oneCombinationOfTrimmedDependencies.size();
    NodeList dependencies = document.getDocumentElement().getElementsByTagName("dependency");
    String debloatedAndSpecializedPom = debloatedPomPath.replace(".xml", "-spl.xml");
    for (TrimmedDependency thisDependency : oneCombinationOfTrimmedDependencies) {
      for (int i = 0; i < dependencies.getLength(); i++) {
        Element dependencyNode = (Element) dependencies.item(i);
        Node groupIdNode = dependencyNode.getElementsByTagName("groupId").item(0);
        Node artifactIdNode = dependencyNode.getElementsByTagName("artifactId").item(0);
        // When original groupId and artifactId are found in debloated pom,
        // replace with new coordinates
        if (groupIdNode.getTextContent().equals(thisDependency.getOriginalGroupId())
            && artifactIdNode.getTextContent().equals(thisDependency.getOriginalDependencyId())
        ) {
          // Found original dependency in debloated POM.
          // Replacing with specialized dependency.
          Node versionNode = dependencyNode.getElementsByTagName("version").item(0);
          groupIdNode.setTextContent(thisDependency.getTrimmedGroupId());
          artifactIdNode.setTextContent(thisDependency.getTrimmedDependencyId());
          versionNode.setTextContent(thisDependency.getTrimmedVersion());
        }
      }
    }
    if (createPomTrimmed) {
      debloatedAndSpecializedPom = debloatedPomPath.replace(".xml", "-spl.xml");
    }
    if (createAllPomsTrimmed) {
      debloatedAndSpecializedPom = debloatedAndSpecializedPom.replace(
          ".xml",
          "-" + combinationNumber + "_" + numberOfTrimmedDependencies + "_" + trimmedDependencies.size() + ".xml"
      );
    }

    saveUpdatedDomInANewPom(document, debloatedAndSpecializedPom);
    return debloatedAndSpecializedPom;
  }

  /**
   * Generates a new XML file based with the changes to the XML document.
   *
   * @param document                The XML document structure to save to file
   * @param debloatedSpecializedPom The path to the pom-debloated-spl.xml file to generate
   * @throws TransformerException If XML document cannot be saved to file
   */
  private void saveUpdatedDomInANewPom(Document document, String debloatedSpecializedPom) throws TransformerException {
    DOMSource dom = new DOMSource(document);
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    Transformer transformer = transformerFactory.newTransformer();
    StreamResult result = new StreamResult(new File(debloatedSpecializedPom));
    transformer.transform(dom, result);
  }
}
