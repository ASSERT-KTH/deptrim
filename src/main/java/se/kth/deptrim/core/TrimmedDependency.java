package se.kth.deptrim.core;

import lombok.Getter;

/**
 * This class represents the original coordinates of a dependency and the coordinates of its trimmed version.
 */
@Getter
public class TrimmedDependency {

  String originalGroupId;
  String originalDependencyId;
  String originalVersion;
  String trimmedGroupId;
  String trimmedDependencyId;
  String trimmedVersion;

  /**
   * Constructor for DependencyOriginalAndTrimmed.
   *
   * @param originalGroupId      The original group id of the dependency.
   * @param originalDependencyId The original id of the dependency.
   * @param originalVersion      The original version of the dependency.
   * @param trimmedGroupId       The standard group id of the dependency trimmed by deptrim.
   */
  public TrimmedDependency(
      String originalGroupId,
      String originalDependencyId,
      String originalVersion,
      String trimmedGroupId
  ) {
    this.originalGroupId = originalGroupId;
    this.originalDependencyId = originalDependencyId;
    this.originalVersion = originalVersion;
    this.trimmedGroupId = trimmedGroupId;
    this.trimmedDependencyId = originalDependencyId;
    this.trimmedVersion = originalVersion;
  }

  /**
   * toString method.
   *
   * @return the string representation of the object.
   */
  @Override
  public String toString() {
    return "TrimmedDependency{"
        + "originalGroupId='" + originalGroupId + '\''
        + ", originalDependencyId='" + originalDependencyId + '\''
        + ", originalVersion='" + originalVersion + '\''
        + ", trimmedGroupId='" + trimmedGroupId + '\''
        + ", trimmedDependencyId='" + trimmedDependencyId + '\''
        + ", trimmedVersion='" + trimmedVersion + '\''
        + '}';
  }

}
