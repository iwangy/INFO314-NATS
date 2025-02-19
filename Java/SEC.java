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
//       System.out.println(new String(msg.getData()));
//       System.out.println(msg.getSubject());
//       System.out.println();

       if (msg.getSubject().contains("_INBOX")) {
//         System.out.println(new String(msg.getData()));
         String xml = new String(msg.getData());
         try{
           FileWriter fw = new FileWriter("suspicious.log", true);
           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = factory.newDocumentBuilder();
           Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));

           document.getDocumentElement().normalize();
           Element root = document.getDocumentElement();

           NodeList buy = root.getElementsByTagName("buy");
           NodeList sell = root.getElementsByTagName("sell");

           int price = -1;
           String timestamp = root.getAttribute("sent");
           String broker = root.getAttribute("broker");

           double transaction = -1;

            // if (buy.getLength() == 0) {
            //  // this is sell
            //   price = Integer.valueOf(((Element) sell.item(0)).getAttribute("price"));

            // } else if (sell.getLength() == 0) {
            //  // this is buy
            //   price = Integer.valueOf(((Element) buy.item(0)).getAttribute("price"));
            // }
            //  System.out.println("[Price]: " + price);

            // if (buy.getLength() == 0) {
            //  // this is sell
            //   transaction = Integer.valueOf(((Element) sell.item(0)).getAttribute("amount"));

            // } else if (sell.getLength() == 0) {
            //  // this is buy
            //   transaction = Integer.valueOf(((Element) buy.item(0)).getAttribute("amount"));
            // }
            NodeList amount = root.getElementsByTagName("complete");
            transaction = Double.parseDouble(((Element) amount.item(0)).getAttribute("amount"));

             System.out.println("[Transaction Price]: " + transaction);

            if (transaction > 500000) {
            //if (transaction > 100000) { //for testing

                fw.write(new String(timestamp + ", " + msg.getSubject() + ", " + broker + ", " + "\n"));
                fw.write(xml + "\n\n");
                fw.close();
            }
         } catch (Exception e) {
                e.printStackTrace();
         }
       }
    });
    d.subscribe(">");
  }
}
