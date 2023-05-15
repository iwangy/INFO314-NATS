import java.io.IOException;
import io.nats.client.*;

public class Consumer {
  public static void main(String... args) throws IOException, InterruptedException {
    Connection nc = Nats.connect("nats://localhost:4222");
    Dispatcher d = nc.createDispatcher((msg) -> {
      System.out.println(new String(msg.getData()));
    });

    d.subscribe("INFO314");
  }
}
