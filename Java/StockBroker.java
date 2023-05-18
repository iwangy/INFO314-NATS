import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import org.xml.sax.SAXException;
import java.io.*;
import io.nats.client.*;
import java.util.concurrent.atomic.AtomicInteger;

public class StockBroker {
  private final String name;
  private final Connection nc;

  public StockBroker(String name, String url) {
    this.name = name;
    try {
      this.nc = Nats.connect(url);
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
    catch(Exception e) {
      e.printStackTrace();
    }
    
  }

  public static void main(String... args) {
    String url = "nats://localhost:4222";
    if (args[1] != null) {
      url = args[1];
    }
    StockBroker sb = new StockBroker(args[0], url);
  }

  private String order(String xml) {
    String receipt = "";
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));

      document.getDocumentElement().normalize();
      Element root = document.getDocumentElement();

      NodeList buy = root.getElementsByTagName("buy");
      NodeList sell = root.getElementsByTagName("sell");
      

      if (buy.getLength() == 0) {
        // this is sell
        String symbol = ((Element) sell.item(0)).getAttribute("symbol");
        int amount = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));
        receipt = orderReceiptBuilder(convertToXML(sell), "sell", symbol, amount);
        
      } else if (sell.getLength() == 0) {
        // this is buy
        String symbol = ((Element) sell.item(0)).getAttribute("symbol");
        int amount = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));
        receipt = orderReceiptBuilder(convertToXML(buy), "buy", symbol, amount);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return receipt;
  }

  private String orderReceiptBuilder(String xml, String action, String symbol, int amount) {    

    // new dispatcher here that subscribes to the symbol
    // get the price
    // unsubcribe
    int currentPrice = 0;
    AtomicInteger currentPriceWrapper = new AtomicInteger(currentPrice);
    Dispatcher d = this.nc.createDispatcher((msg) -> {
        // String liveXML = new String(msg.getData());
        
        try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          Document document = builder.parse(new ByteArrayInputStream(new String(msg.getData()).getBytes()));
  
          document.getDocumentElement().normalize();
          Element root = document.getDocumentElement();
          System.out.println(root.getNodeName());
  
          NodeList stockName = root.getElementsByTagName("name"); 
          String sn = stockName.item(0).getTextContent();
  
          if (sn.equals(symbol)) {
            NodeList adjustedPrice = root.getElementsByTagName("adjustedPrice");
            String adjp = adjustedPrice.item(0).getTextContent();
            int currentPrice2 = Integer.valueOf(adjp);
            currentPriceWrapper.set(currentPrice2);
            // d.unsubcribe("stock." + symbol);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

    });
    d.subscribe("stock." + symbol);

    currentPrice = currentPriceWrapper.get();

    double completeAmount;
    if (action.equals("sell")) {
      // (current price of stock * number of shares) * 0.10
      // get current price of stock
      // xml data contains (symbol and amount)
      
      completeAmount = (currentPrice * amount) * .9;


    } else {
      // (current price of stock * number of shares) * 0.90
      completeAmount = (currentPrice * amount) * 1.1;
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
