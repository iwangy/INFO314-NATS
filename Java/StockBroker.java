import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.io.*;
import io.nats.client.*;
import org.xml.sax.SAXException;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class StockBroker {

  private Connection nc;

  public StockBroker(String url, String name) throws Exception {
    System.out.println("[StockBroker]: " + name + " is running!");
    this.nc = Nats.connect(url);
    try {
      Dispatcher d = nc.createDispatcher((msg) -> {
        System.out.println("[Got Order]: " + new String(msg.getData()) + "\n");
        String receipt = order(new String(msg.getData()), name);
        this.nc.publish(msg.getReplyTo(), receipt.getBytes());
        System.out.println("[Sent Receipt]: " + receipt + "\n");
      });
      d.subscribe("broker." + name);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String... args) {
    String url = "nats://127.0.0.1:4222";
    String name = "ted"; // default name
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

   private String order(String xml, String brokerName) throws InterruptedException {
    //sleep
     Random waiter = new Random();
     int time = waiter.nextInt(5) * 1000 + 1000;
//     System.out.println("[Broker]: " + "I am sleeping for " + time);
     Thread.sleep(time);
//     System.out.println("[Broker]: I woke up");

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
//         String ts = ((Element) sell.item(0)).getAttribute(("time"));
//         String symbol = ((Element) sell.item(0)).getAttribute("symbol");
         int amount = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));
         int price = Integer.valueOf(((Element) sell.item(0)).getAttribute("price"));
         receipt = orderReceiptBuilder(convertToXML(sell),"sell", amount, price, brokerName);
        
       } else if (sell.getLength() == 0) {
         // this is buy
//         String ts = ((Element) sell.item(0)).getAttribute(("time"));
//         String symbol = ((Element) buy.item(0)).getAttribute("symbol");
         int amount = Integer.valueOf(((Element) buy.item(0)).getAttribute("amount"));
         int price = Integer.valueOf(((Element) buy.item(0)).getAttribute("price"));
         receipt = orderReceiptBuilder(convertToXML(buy),"buy", amount, price, brokerName);

       }
     } catch (Exception e) {
       e.printStackTrace();
     }
     return receipt;
   }

   private String orderReceiptBuilder(String xml, String action, int amount, int price, String brokerName) throws IOException, SAXException, ParserConfigurationException {

     // new dispatcher here that subscribes to the symbol
     // get the price

    // StockPublisher.setPrice(symbol, 100);
    //  int currentPrice = StockPublisher.getPrice(symbol);
    //  System.out.println("[Stock Price]: " + currentPrice + "\n");

//     try {
//       Subscription sub = this.nc.subscribe("stock." + symbol);
//       Message message = sub.nextMessage(Duration.ofSeconds(100));
//
//       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//       DocumentBuilder builder = factory.newDocumentBuilder();
//       Document document = builder.parse(new ByteArrayInputStream(new String(message.getData()).getBytes()));
//
//       document.getDocumentElement().normalize();
//       Element root = document.getDocumentElement();
//
//       NodeList stockName = root.getElementsByTagName("name");
//       String sn = stockName.item(0).getTextContent();
//
//       if (sn.equals(symbol)) {
//        NodeList adjustedPrice = root.getElementsByTagName("adjustedPrice");
//        String adjp = adjustedPrice.item(0).getTextContent();
//        NodeList adjustment = root.getElementsByTagName("adjustment");
//        String adj = adjustment.item(0).getTextContent();
//        currentPrice = Integer.valueOf(adjp) - Integer.valueOf(adj);
//       }
//
//     } catch (Exception e) {
//       e.printStackTrace();
//     }

     double completeAmount;
     if (action.equals("sell")) {
       // (current price of stock * number of shares) * 0.9
       // get current price of stock
       // xml data contains (symbol and amount)
       completeAmount = (price * amount) * .9;
     } else {
       // (current price of stock * number of shares) * 1.1
       completeAmount = (price * amount) * 1.1;
     }

     Date date = new Date();
     Timestamp timeStamp = new Timestamp(date.getTime());

     String xmlString =
       "<OrderReceipt sent=\""+timeStamp+"\" broker=\""+brokerName+"\">" +
          xml +
          "<complete amount=\"" + completeAmount + "\"/>"+
       "</OrderReceipt>";

     return xmlString;
   }

   private String convertToXML(NodeList nl) throws TransformerException {
     TransformerFactory transformerFactory = TransformerFactory.newInstance();
     Transformer transformer = transformerFactory.newTransformer();
     transformer.setOutputProperty(OutputKeys.INDENT, "yes");
     transformer.setOutputProperty("omit-xml-declaration", "yes"); 

       // create string from xml tree
       StringWriter sw = new StringWriter();
       for (int i = 0; i < nl.getLength(); i++) {
           transformer.transform(new DOMSource(nl.item(i)), new StreamResult(sw));
       }

       return sw.toString();
   }
}
