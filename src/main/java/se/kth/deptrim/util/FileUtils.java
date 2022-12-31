package se.kth.deptrim.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for handling files.
 */
@Slf4j
@Getter
public class FileUtils {

  private int deletedDirectories = 0;

  /**
   * Delete all empty directories in the given directory.
   *
   * @param directory the directory to delete empty directories from
   */
  public int deleteEmptyDirectories(File directory) {
    List<File> toBeDeleted = Arrays.stream(Objects.requireNonNull(directory.listFiles())).sorted()
        .filter(File::isDirectory)
        .filter(f -> f.listFiles().length == deleteEmptyDirectories(f)).toList();
    int size = toBeDeleted.size();
    toBeDeleted.forEach(t -> {
      try {
        Files.delete(t.toPath());
        deletedDirectories++;
      } catch (IOException e) {
        log.error("Error deleting file " + t.getAbsolutePath());
      }
    });
    return size; // the number of deleted directories.
  }

}
