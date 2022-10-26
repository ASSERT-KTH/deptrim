package se.kth.deptrim;

import static com.soebes.itf.extension.assertj.MavenITAssertions.assertThat;

import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.DisplayName;

/**
 * <p>
 * This class executes integration tests against the {@link se.kth.deptrim.DepTrimMojo}.
 * The projects used for testing are in src/test/resources-its/se/kth/deptrim/DepTrimMojoIT.
 * The results of the DepTrim executions for each project are in target/maven-it/se/kth/deptrim/DepTrimMojoIT.
 * </p>
 *
 * @see <a https://khmarbaise.github.io/maven-it-extension/itf-documentation/background/background.html#_assertions_in_maven_tests</a>
 */
@MavenJupiterExtension
public class DepTrimMojoIT {

  @MavenTest
  @DisplayName("Test that DepTrim runs in an empty Maven project")
  void ifProjectIsEmpty_ThenDeptrimShouldNotBreakTheBuild(MavenExecutionResult result) {
    assertThat(result).isSuccessful(); // should pass
  }

}

