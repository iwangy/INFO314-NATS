/**
 * Take the NATS URL on the command-line.
 */
import io.nats.client.*;
public class StockPublisher {
  public static void main(String... args) throws Exception {
      String natsURL = "nats://127.0.0.1:4222";
      if (args.length > 0) {
          natsURL = args[0];
      }
      Connection nc = Nats.connect("nats://localhost:4222");

      System.console().writer().println("Starting stock publisher....");

      StockMarket sm1 = new StockMarket(StockPublisher::publishDebugOutput, "AMZN", "MSFT", "GOOG");
      new Thread(sm1).start();
      StockMarket sm2 = new StockMarket(StockPublisher::publishDebugOutput, "ACTV", "BLIZ", "ROVIO");
      new Thread(sm2).start();
      StockMarket sm3 = new StockMarket(StockPublisher::publishDebugOutput, "GE", "GMC", "FORD");
      new Thread(sm3).start();
    }

    public synchronized static void publishDebugOutput(String symbol, int adjustment, int price) {
        System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
    }
    // When you have the NATS code here to publish a message, put "publishMessage" in
    // the above where "publishDebugOutput" currently is
    public synchronized static void publishMessage(String symbol, int adjustment, int price) {
    } 
}