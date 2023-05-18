import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.nats.client.*;

public class StockBrokerClient {
  public static void main(String... args) throws Exception {

    String stockBrokerName = args[0];
    Connection nc = Nats.connect("nats://localhost:4222");
    CompletableFuture<Message> response;
    try {
      System.out.println("We are connecting to " + stockBrokerName);
      response = nc.request("broker." + stockBrokerName, "asdfasdfasdfasdfasdfasdf".getBytes());
      Message m = response.get(1, TimeUnit.SECONDS);
      System.out.println("Data: " + new String(m.getData()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
