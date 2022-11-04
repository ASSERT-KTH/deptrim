import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UseJcabiXMLTest {

  @Test
  void test() {
    XML xml = new XMLDocument("<orders><order id=\"4\">Coffee to go</order></orders>");

    String id = xml.xpath("//order/@id").get(0);
    List<String> xpath = xml.xpath("//order[@id=4]/text()");

    Assertions.assertEquals("[Coffee to go]", xpath.toString());
    Assertions.assertEquals("4", id.toString());

  }

}