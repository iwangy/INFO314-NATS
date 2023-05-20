import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.nats.client.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.sql.Timestamp;
import java.util.Date;



public class StockBrokerClient {
  private Connection nc;

  private final String stockBrokerName;

  private Map<String, Integer> portfolio;

  private Map<String, Integer> sellStrategy = new HashMap<>(); // <symbol, above price>
  private Map<String, Pair> buyStrategy = new HashMap<>(); // <symbol, <price, amount>>

  public StockBrokerClient(String natURL, String stockBrokerName) {
    this.stockBrokerName = stockBrokerName;
    try {
      this.nc = Nats.connect(natURL);
      System.out.println("We are connecting to " + stockBrokerName);

      this.portfolio = this.setupPortfolio("Clients/portfolio-2.xml");
      this.setupStrategy("Clients/strategy-2.xml");

      Dispatcher dispatcher = nc.createDispatcher((msg) -> {
        String xmlData = new String(msg.getData());
        try {
          DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
          Document doc = dBuilder.parse(new InputSource(new StringReader(xmlData)));
          doc.getDocumentElement().normalize();

          String symbol = doc.getElementsByTagName("name").item(0).getTextContent();
          String price = doc.getElementsByTagName("adjustedPrice").item(0).getTextContent();

          if(this.sellStrategy.containsKey(symbol) &&
            this.sellStrategy.get(symbol) <= Integer.parseInt(price) &&
            this.portfolio.containsKey(symbol)) {
            System.out.println("Time to Sell: " + symbol + " at price: " + price + " amount: " + portfolio.get(symbol));
            this.sendXMLSellOrderRequest(symbol, Integer.parseInt(price));
          } else if(this.buyStrategy.containsKey(symbol) &&
                  this.buyStrategy.get(symbol).getKey() >= Integer.parseInt(price)) {
            System.out.println("Time to Buy: " + symbol + " at price: " + price + " current amount: " + buyStrategy.get(symbol).getValue());
            this.sendXMLBuyOrderRequest(symbol, this.buyStrategy.get(symbol).getValue(), Integer.parseInt(price));
          }
        } catch (ParserConfigurationException | SAXException | IOException e) {
          throw new RuntimeException(e);
        }

      });
      dispatcher.subscribe("stock.*");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void writePortFolio() {
    StringBuilder portfolioXML = new StringBuilder("<portfolio>\n");

    for(String symbol: portfolio.keySet()) {
      String stockElement = String.format("<stock symbol=\"%s\">%d</stock>\n", symbol, portfolio.get(symbol));
      portfolioXML.append(stockElement);
    }

    portfolioXML.append("</portfolio>");

    Path filePath = Paths.get("Clients/portfolio-2.xml");
    try {
      Files.write(filePath, portfolioXML.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      System.out.println("portfolio updated successfully.\n");
    } catch (IOException e) {
      System.err.println("Error writing file: " + e.getMessage());
    }
  }

  private void sendXMLBuyOrderRequest(String symbol, int amount, int price) {
    Date date = new Date();
    Timestamp ts = new Timestamp(date.getTime());

    String xml = String.format("<order><buy time=\"%s\" symbol=\"%s\" amount=\"%d\" price=\"%d\" /></order>", ts, symbol, amount, price);
    try {
      Message m = nc.request("broker." + stockBrokerName, xml.getBytes(), Duration.ofSeconds(100));
      System.out.println("Receipt: " + new String(m.getData()));

      // update portfolio
      portfolio.put(symbol, portfolio.getOrDefault(symbol, 0) + amount);
      this.writePortFolio();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendXMLSellOrderRequest(String symbol, int price) {
    Date date = new Date();
    Timestamp ts = new Timestamp(date.getTime());

    String xml = String.format("<order><sell time=\"%s\" symbol=\"%s\" amount=\"%d\" price=\"%d\"  /></order>", ts, symbol,
            portfolio.get(symbol), price);
    try {
      Message m = nc.request("broker." + stockBrokerName, xml.getBytes(), Duration.ofSeconds(100));
      System.out.println("Receipt: " + new String(m.getData()));

      // update portfolio
      portfolio.remove(symbol);
      this.writePortFolio();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void setupStrategy(String path) {
    File inputFile = new File(path); // replace with your XML file name
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(inputFile);
      doc.getDocumentElement().normalize();

      NodeList whenElements = doc.getElementsByTagName("when");
      for(int i = 0; i < whenElements.getLength(); i++) {
        Element whenElement = (Element) whenElements.item(i);
        Element stockElement = (Element) whenElement.getElementsByTagName("stock").item(0);

        NodeList aboveElements = whenElement.getElementsByTagName("above");
        NodeList belowElements = whenElement.getElementsByTagName("below");
        Element aboveElement = aboveElements.getLength() > 0 ? (Element) aboveElements.item(0) : null;
        Element belowElement = belowElements.getLength() > 0 ? (Element) belowElements.item(0) : null;

        if(aboveElement != null) {
          int price = Integer.parseInt(aboveElement.getTextContent());
          this.sellStrategy.put(stockElement.getTextContent(), price);
        }

        if(belowElement != null) {
          int price = Integer.parseInt(belowElement.getTextContent());
          int amount = Integer.parseInt(whenElement.getElementsByTagName("buy").item(0).getTextContent());

          this.buyStrategy.put(stockElement.getTextContent(), new Pair(price, amount));
        }
      }

    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Integer> setupPortfolio(String path) {
    Map<String, Integer> portfolio = new HashMap<>();

    File inputFile = new File(path); // replace with your XML file name
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(inputFile);
      doc.getDocumentElement().normalize();

      NodeList stockElements = doc.getElementsByTagName("stock");

      for (int i = 0; i < stockElements.getLength(); i++) {
        Node stockElement = stockElements.item(i);
        int stockNumber = Integer.parseInt(stockElement.getTextContent());
        String stockSymbol = ((Element) stockElement).getAttribute("symbol");
        portfolio.put(stockSymbol, stockNumber);
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {

      throw new RuntimeException(e);
    }

    return portfolio;
  }

  public static void main(String... args) {
    String natsURL = "nats://127.0.0.1:4222";
    String stockBrokerName = "ted";

    if (args.length > 0 && args[0] != null) {
      natsURL = args[0];
    }
    if (args.length > 1 && args[1] != null)  {
      stockBrokerName = args[1];
    }
    new StockBrokerClient(natsURL, stockBrokerName);
  }
}
