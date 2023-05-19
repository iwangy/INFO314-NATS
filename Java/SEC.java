import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import java.io.*;
import io.nats.client.*;
import java.util.Date;
import java.sql.Timestamp;

public class SEC {
  public static void main(String... args) throws Exception {
    Connection nc = Nats.connect("nats://localhost:4222");
    Dispatcher d = nc.createDispatcher((msg) -> {

      //csv format
      // timestamp, client, broker, order, amount
      // System.out.println(new String(msg.getData()));
      // System.out.println(msg.getSubject());

      try(FileWriter fw = new FileWriter("suspicious.log", true)) {
        String timestamp = "";
        String broker = "";
        String client = "";
        
        if (msg.getSubject().matches("broker\\..*")) {
          Date date = new Date();
          Timestamp ts = new Timestamp(date.getTime());
          timestamp = ts.toString();
          String sub = msg.getSubject();
          int dotIndex = sub.lastIndexOf(".") + 1;
          broker = sub.substring(dotIndex);
        } 
        if (msg.getSubject().matches("_INBOX\\..*")) {
          client = msg.getSubject();
          
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          Document document = builder.parse(new ByteArrayInputStream(new String(msg.getData()).getBytes()));
  
          document.getDocumentElement().normalize();
          
          Element root = document.getDocumentElement();
          System.out.println(root.getNodeName());

          NodeList receipt = root.getElementsByTagName("buy");
          String amount = root.getAttribute("amount");
        }

        fw.write(new String(timestamp + ", " + client + ", " + broker + ", " + "\n"));
        fw.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    d.subscribe("broker.*");
  }
}
