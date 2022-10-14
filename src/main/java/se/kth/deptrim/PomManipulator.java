package se.kth.deptrim;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.deptrim.util.MagicStrings;

/**
 * This class generates a specialized pom from the debloated pom.
 */
public class PomManipulator {
  /**
   * Creates a specialized pom from the debloated pom produced by DepClean.
   *
   * @param debloatedPomPath The path of pom-debloated.xml file
   * @param originalDependencyGroupId The groupId of the original non-specialized dependency
   * @param originalDependencyArtifactId The groupId of the original non-specialized dependency
   * @param specializedJarName The name of the specialized jar
   * @return The path of the generated pom-debloated-spl.xml file
   * @throws Exception when any of this goes wrong :)
   */
  public String createSpecializedPomFromDebloatedPom(String debloatedPomPath,
                                                     String originalDependencyGroupId,
                                                     String originalDependencyArtifactId,
                                                     String specializedJarName) throws Exception {
    String debloatedAndSpecializedPom = debloatedPomPath.replace(".xml", "-spl.xml");

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(new File(debloatedPomPath));
    doc.getDocumentElement().normalize();

    // Replace original dependency with specialized dependency
    NodeList dependencies = doc.getDocumentElement().getElementsByTagName("dependency");
    for (int i = 0; i < dependencies.getLength(); i++) {
      Element dependencyNode = (Element) dependencies.item(i);
      Node groupIdNode = dependencyNode.getElementsByTagName("groupId").item(0);
      Node artifactIdNode = dependencyNode.getElementsByTagName("artifactId").item(0);
      // When original groupId and artifactId are found in debloated pom,
      // replace with new coordinates
      if (groupIdNode.getTextContent().equals(originalDependencyGroupId)
              & artifactIdNode.getTextContent().equals(originalDependencyArtifactId)) {
        System.out.println("Found original dependency in debloated pom");
        System.out.println("Replacing with specialized dependency");
        Node versionNode = dependencyNode.getElementsByTagName("version").item(0);
        groupIdNode.setTextContent(MagicStrings.deptrimSpecializedGroupId);
        artifactIdNode.setTextContent(specializedJarName);
        versionNode.setTextContent(MagicStrings.deptrimSpecializedVersion);
        saveUpdatedDomInANewPom(doc, debloatedAndSpecializedPom);
        break;
      }
    }
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
