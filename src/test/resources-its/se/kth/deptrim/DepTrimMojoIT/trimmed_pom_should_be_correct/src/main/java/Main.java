import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class Main {

   public static void main(String[] args) {
      // Use guava
      Map<String, Integer> salary = ImmutableMap.<String, Integer>builder()
          .put("John", 1000)
          .put("Jane", 1500)
          .put("Adam", 2000)
          .put("Tom", 2000)
          .build();
      salary.forEach((key, value) -> System.out.println(key + " -> " + value));


      // Use commons-io
      System.out.println(FileUtils.getTempDirectory().getAbsolutePath());

   }
}