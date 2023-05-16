/**
 * Take the NATS URL on the command-line.
 */
import java.util.Date;
import java.sql.Timestamp;
import io.nats.client.*;
public class StockPublisher {

    private static Connection nc = null;

    public static void main(String... args) throws Exception {
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }
        nc = Nats.connect("nats://localhost:4222");
        System.console().writer().println("connected to nats");
        System.console().writer().println("Starting stock publisher....");

        // StockMarket sm1 = new StockMarket(StockPublisher::publishDebugOutput, "AMZN", "MSFT", "GOOG");
        // new Thread(sm1).start();
        // StockMarket sm2 = new StockMarket(StockPublisher::publishDebugOutput, "ACTV", "BLIZ", "ROVIO");
        // new Thread(sm2).start();
        // StockMarket sm3 = new StockMarket(StockPublisher::publishDebugOutput, "GE", "GMC", "FORD");
        // new Thread(sm3).start();

        StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "AMZN", "MSFT", "GOOG", "NFLX", "SPOT", "META", "AAPL");
        new Thread(sm1).start();
        StockMarket sm2 = new StockMarket(StockPublisher::publishMessage, "ATVI", "ROVIO", "NVDA", "RBLX", "SONY", "NTDOY", "TCEHY");
        new Thread(sm2).start();
        StockMarket sm3 = new StockMarket(StockPublisher::publishMessage, "GE", "GM", "F", "TSLA", "FUJHY", "BMWYY", "MBGYY");
        new Thread(sm3).start();
    }

    // public synchronized static void publishDebugOutput(String symbol, int adjustment, int price) {
    //     System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
    // }

    // When you have the NATS code here to publish a message, put "publishMessage" in
    // the above where "publishDebugOutput" currently is
    public synchronized static void publishMessage(String symbol, int adjustment, int price) {
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        String xmlString = 
            "<message sent=\""+ts+"\">" +
                "<stock>" +
                    "<name>"+symbol+"</name>" +
                    "<adjustment>"+(adjustment)+"</adjustment>" +  
                    "<adjustedPrice>"+(price)+"</adjustedPrice>" +
                "</stock>" +
            "</message>";
        nc.publish("stock."+symbol, xmlString.getBytes());
    } 
}