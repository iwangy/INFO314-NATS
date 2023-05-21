import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

import io.nats.client.*;


public class StockMonitor {
  public static void main(String... args) throws IOException, InterruptedException {  

    String natURL = "nats://localhost:4222";

    if (args.length > 0 && args[0] != null) {
      natURL = args[0];
    }

    // File file = new File("log.txt");
    // if (file.exists()) {
    //   file.delete();
    // }    
    Connection nc = Nats.connect(natURL);
    Dispatcher d = nc.createDispatcher((msg) -> {
      System.out.println(new String(msg.getData()));

      // Stock Monitor - NFLX, GOOG
      //System.out.println(msg.getSubject());
      String stockSymbol = msg.getSubject();
      stockSymbol = stockSymbol.replace("stock.", "");
      //System.out.println(stockSymbol);
      HashSet<String> symbolSet = new HashSet<String>();
      symbolSet.add(stockSymbol);
      
      for (String symbol: symbolSet) {
        try(FileWriter fw = new FileWriter(String.format("%s.txt", symbol), true)) {

          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          Document document = builder.parse(new ByteArrayInputStream(new String(msg.getData()).getBytes()));

          document.getDocumentElement().normalize();
          Element root = document.getDocumentElement();

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
      }
    });

    if (args.length > 0 && args[1] != null) {
      for(int i = 1; i < args.length; i++){
        d.subscribe("stock." + args[i]);
      }

      // for (String arg: args) {
      //   d.subscribe("stock." + arg);
      // }
      
    } else {
      d.subscribe("stock.*");
    }

  }

}
