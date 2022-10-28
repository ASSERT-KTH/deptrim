import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class Main {

  public static void main(String[] args) {
    Map<String, Integer> salary = ImmutableMap.<String, Integer>builder()
        .put("John", 1000)
        .put("Jane", 1500)
        .put("Adam", 2000)
        .put("Tom", 2000)
        .build();

    salary.forEach((key, value) -> System.out.println(key + " -> " + value));

  }
}