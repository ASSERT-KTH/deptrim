package se.kth.deptrim.mojo.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for handling files.
 */
public class FileUtils {

  /**
   * Delete all empty directories in the given directory.
   *
   * @param directory the directory to delete empty directories from
   */
  public int deleteEmptyDirectories(File directory) {
    List<File> toBeDeleted = Arrays.stream(directory.listFiles()).sorted()
        .filter(File::isDirectory)
        .filter(f -> f.listFiles().length == deleteEmptyDirectories(f))
        .collect(Collectors.toList());
    int size = toBeDeleted.size();
    toBeDeleted.forEach(t -> {
      final String path = t.getAbsolutePath();
      final boolean delete = t.delete();
    });
    return size; // the number of deleted directories.
  }

}
