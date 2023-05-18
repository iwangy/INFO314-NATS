import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.*;
import io.nats.client.*;

import java.time.Duration;

public class StockBroker {

  private Connection nc;

  public StockBroker(String url, String name) throws Exception {
    this.nc = Nats.connect(url);
    try {
      Dispatcher d = nc.createDispatcher((msg) -> {
        System.out.println("got order: " + new String(msg.getData()));
        String receipt = order(new String(msg.getData()));
        this.nc.publish(msg.getReplyTo(), receipt.getBytes());
        System.out.println("sent receipt " + receipt);
      });
      d.subscribe("broker." + name);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String... args) {
    String url = "nats://127.0.0.1:4222";
    String name = "deeznuts"; // default name
    if (args.length > 0 && args[0] != null) {
      url = args[0];
    }
    if (args.length > 1 && args[1] != null) { // accepting the second parameter as the name
      name = args[1];
    }

    try {
      new StockBroker(url, name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
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
         String symbol = ((Element) buy.item(0)).getAttribute("symbol");
         int amount = Integer.valueOf(((Element) buy.item(0)).getAttribute("amount"));
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
     int currentPrice = 0;

     try {
       Subscription sub = this.nc.subscribe("stock." + symbol);
       Message message = sub.nextMessage(Duration.ofSeconds(100));

       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       DocumentBuilder builder = factory.newDocumentBuilder();
       Document document = builder.parse(new ByteArrayInputStream(new String(message.getData()).getBytes()));

       document.getDocumentElement().normalize();
       Element root = document.getDocumentElement();

       NodeList stockName = root.getElementsByTagName("name");
       String sn = stockName.item(0).getTextContent();

       if (sn.equals(symbol)) {
         NodeList adjustedPrice = root.getElementsByTagName("adjustedPrice");
         String adjp = adjustedPrice.item(0).getTextContent();
         currentPrice = Integer.valueOf(adjp);
       }

     } catch (Exception e) {
       e.printStackTrace();
     }
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
