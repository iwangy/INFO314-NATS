import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import org.xml.sax.SAXException;
import java.io.*;

public class StockBroker {
  private String name;
  private Connection nc = null;

  public StockBroker(String name) {

    this.name = name;
    
    this.nc = Nats.connect("nats://localhost:4222");
    Dispatcher d = nc.createDispatcher((msg) -> {
        String xml = new String(msg.getData());
        String receipt = this.order(xml);
        try {
          this.nc.publish(msg.getReplyTo(), receipt.getBytes());
        } catch (Exception e) {
          e.printStackTrace();
        }
    });
    d.subscribe("broker." + this.name);
  }

  private String order(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));

      document.getDocumentElement().normalize();
      Element root = document.getDocumentElement();

      NodeList buy = root.getElementsByTagName("buy");
      NodeList sell = root.getElementsByTagName("sell");
      
      String receipt = "";

      if (buy.getLength() == 0) {
        // this is sell
        String symbol = ((Element) sell.item(0)).getAttribute("symbol");
        int amount = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));
        receipt = orderReceiptBuilder(convertToXML(sell), "sell", symbol, amount);
        
      } else if (sell.getLength() == 0) {
        String symbol = ((Element) sell.item(0)).getAttribute("symbol");
        int amount = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));
        receipt = orderReceiptBuilder(convertToXML(buy), "buy", symbol, amount);

      }
      
     return receipt;
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String orderReceiptBuilder(String xml, String action, String symbol, int amount) {    

    int completeAmount;
    if (action.equals("sell")) {
    } else {

    }

    String xmlString = 
      "<OrderReceipt>" +
         xml +
         "<complete amount=" + completeAmount + "/>"+
      "</OrderReceipt>";
   
    System.out.println(xmlString);
    
    return xmlString;
  }

  private String convertToXML(NodeList nl) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      // create string from xml tree
      StringWriter sw = new StringWriter();
      for (int i = 0; i < nl.getLength(); i++) {
          transformer.transform(new DOMSource(nl.item(i)), new StreamResult(sw));
      }

      return sw.toString();
  }
}
