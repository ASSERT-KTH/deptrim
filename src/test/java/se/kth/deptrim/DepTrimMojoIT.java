package se.kth.deptrim;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>
 * This class executes integration tests against the {@link se.kth.deptrim.DepTrimMojo}. The projects used for testing are in src/test/resources-its/se/kth/deptrim/DepTrimMojoIT. The results of the
 * DepTrim executions for each project are in target/maven-it/se/kth/deptrim/DepTrimMojoIT.
 * </p>
 *
 * @see <a https://khmarbaise.github.io/maven-it-extension/itf-documentation/background/background.html#_assertions_in_maven_tests</a>
 */
@MavenJupiterExtension
public class DepTrimMojoIT {

  @MavenTest
  @DisplayName("Test that DepTrim runs in an empty Maven project")
  void empty_project(MavenExecutionResult result) {
    System.out.println("Testing that DepTrim runs in an empty Maven project");
    assertThat(result).isSuccessful(); // should pass
  }

  @MavenTest
  @DisplayName("Test that DepTrim creates specialized poms")
  void all_pom_specialized(MavenExecutionResult result) {
    System.out.println("Testing that DepTrim pushes specialized dependencies to the local repository.");
    String LocalRepositoryAbsolutePath = result.getMavenCacheResult().getStdout().toFile().getAbsolutePath();
    String pathToSpecializedCommonsIO = "/se/kth/castor/deptrim/spl/commons-io/2.11.0/commons-io-2.11.0.jar";
    String pathToSpecializedGuava = "/se/kth/castor/deptrim/spl/guava/17.0/guava-17.0.jar";
    Assertions.assertTrue(Files.exists(new File(LocalRepositoryAbsolutePath + pathToSpecializedCommonsIO).toPath()));
    Assertions.assertTrue(Files.exists(new File(LocalRepositoryAbsolutePath + pathToSpecializedGuava).toPath()));

    System.out.println("Testing that DepTrim pushes specialized dependencies to /libs-specialized.");
    String pathToProject = result.getMavenProjectResult().getTargetBaseDirectory().getAbsolutePath();
    String pathToSpecializedCommonsIOInProject = "/project/libs-specialized/commons-io-2.11.0.jar";
    String pathToSpecializedGuavaInProject = "/project/libs-specialized/commons-io-2.11.0.jar";
    Assertions.assertTrue(Files.exists(new File(pathToProject + pathToSpecializedCommonsIOInProject).toPath()));
    Assertions.assertTrue(Files.exists(new File(pathToProject + pathToSpecializedGuavaInProject).toPath()));

    System.out.println("Testing that DepTrim produces four specialized POM files in the root of the project.");
    File pathToProjectDirectory = new File(result.getMavenProjectResult().getTargetBaseDirectory().getAbsolutePath() + "/project");
    File[] specializedPomFiles = pathToProjectDirectory.listFiles((dirFiles, filename) -> filename.startsWith("pom-specialized_") && filename.endsWith(".xml"));
    assert specializedPomFiles != null;
    Assertions.assertEquals(4, specializedPomFiles.length);

    System.out.println("Testing that the number of dependencies in the specialized POM files are correct.");
    for (File specializedPomFile : specializedPomFiles) {
      try {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document =  documentBuilder.parse(specializedPomFile);
        document.getDocumentElement().normalize();
        NodeList dependencies = document.getDocumentElement().getElementsByTagName("dependency");
        Assertions.assertEquals(2, dependencies.getLength());
      } catch (IOException | ParserConfigurationException | SAXException e) {
        System.out.println("Error parsing pom file: " + specializedPomFile.getAbsolutePath());
      }
    }
  }

}

