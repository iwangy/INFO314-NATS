import io.nats.client.*;
public class StockBrokerClient {
  public static void main(String... args) throws Exception {

    String stockBrokerName = args[0];

    // StockBroker jason = new StockBroker("Jason");

    Connection nc = Nats.connect("nats://localhost:4222");
    nc.publish("broker." + stockBrokerName, "asdfasdfasdfasdfasdfasdf".getBytes());
    System.out.println("jason connected");



    
  }
}
