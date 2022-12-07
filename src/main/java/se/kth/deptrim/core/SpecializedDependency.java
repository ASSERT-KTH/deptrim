package se.kth.deptrim.core;

import lombok.Getter;

/**
 * This class represents the original coordinates of a dependency and the coordinates of its trimmed version.
 */
@Getter
public class SpecializedDependency {

  String originalGroupId;
  String originalArtifactId;
  String originalVersion;
  String specializedGroupId;
  String specializedArtifactId;
  String specializedVersion;

  /**
   * Constructor.
   *
   * @param originalGroupId    The original group id of the dependency.
   * @param originalArtifactId The original id of the dependency.
   * @param originalVersion    The original version of the dependency.
   * @param specializedGroupId The standard group id of the dependency trimmed by deptrim.
   */
  public SpecializedDependency(
      String originalGroupId,
      String originalArtifactId,
      String originalVersion,
      String specializedGroupId
  ) {
    this.originalGroupId = originalGroupId;
    this.originalArtifactId = originalArtifactId;
    this.originalVersion = originalVersion;
    this.specializedGroupId = specializedGroupId;
    this.specializedArtifactId = originalArtifactId;
    this.specializedVersion = originalVersion;
  }

  /**
   * toString method.
   *
   * @return the string representation of the object.
   */
  @Override
  public String toString() {
    return "SpecializedDependency{"
        + "originalGroupId='" + originalGroupId + '\''
        + ", originalArtifactId='" + originalArtifactId + '\''
        + ", originalVersion='" + originalVersion + '\''
        + ", specializedGroupId='" + specializedGroupId + '\''
        + ", specializedArtifactId='" + specializedArtifactId + '\''
        + ", specializedVersion='" + specializedVersion + '\''
        + '}';
  }

}
