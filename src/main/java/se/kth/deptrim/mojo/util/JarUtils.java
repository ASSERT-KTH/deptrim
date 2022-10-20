package se.kth.deptrim.mojo.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Utility class for creating jar files.
 */
public class JarUtils {

  /**
   * Create a Jar file from a directory.
   *
   * @param directory The directory to be archived.
   * @param jarFile   The jar file to be created.
   * @throws Exception if an error occurs.
   */
  public static void createJarFromDirectory(File directory, File jarFile) throws Exception {
    JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile));
    for (File file : directory.listFiles()) {
      addFile("", file, target);
    }
    target.close();
  }

  /**
   * Add a file to the jar file.
   *
   * @param parents The parent directories of the file.
   * @param source  The file to be added.
   * @param target  The jar file.
   * @throws Exception if an error occurs.
   */
  private static void addFile(String parents, File source, JarOutputStream target) throws Exception {
    BufferedInputStream in = null;
    try {
      String name = (parents + source.getName()).replace("\\", "/");
      if (source.isDirectory()) {
        if (!name.isEmpty()) {
          if (!name.endsWith("/")) {
            name += "/";
          }
          JarEntry entry = new JarEntry(name);
          entry.setTime(source.lastModified());
          target.putNextEntry(entry);
          target.closeEntry();
        }
        for (File nestedFile : source.listFiles()) {
          addFile(name, nestedFile, target);
        }
        return;
      }
      JarEntry entry = new JarEntry(name);
      entry.setTime(source.lastModified());
      target.putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(source));
      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1) {
          break;
        }
        target.write(buffer, 0, count);
      }
      target.closeEntry();
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

}
