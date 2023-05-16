import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import java.io.*;

import io.nats.client.*;


public class StockMonitor {
  public static void main(String... args) throws IOException, InterruptedException {  

    File file = new File("log.txt");
    if (file.exists()) {
      file.delete();
    }
    
    Connection nc = Nats.connect("nats://localhost:4222");
    Dispatcher d = nc.createDispatcher((msg) -> {
      System.out.println(new String(msg.getData()));


      try(FileWriter fw = new FileWriter("log.txt", true)) {
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(new String(msg.getData()).getBytes()));

        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        System.out.println(root.getNodeName());

        NodeList timestamp = root.getElementsByTagName("message");
        String ts = root.getAttribute("sent");

        NodeList stockName = root.getElementsByTagName("name"); 
        String sn = stockName.item(0).getTextContent();

        NodeList adjustment = root.getElementsByTagName("adjustment");
        String adj = adjustment.item(0).getTextContent();

        NodeList adjustedPrice = root.getElementsByTagName("adjustedPrice");
        String adjp = adjustedPrice.item(0).getTextContent();

        fw.write(new String(ts + ", " + sn + ", " + adj + ", " + adjp + "\n"));
        fw.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    if (args.length > 0) {
      for (String arg: args) {
        d.subscribe(arg);
      }
    } else {
      d.subscribe("stock.*");
    }


  }

}
