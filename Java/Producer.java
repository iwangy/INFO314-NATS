import io.nats.client.*;

public class Producer {
  public static void main(String... args) throws Exception {
    Connection nc = Nats.connect("nats://localhost:4222");
    nc.publish("INFO314", "hello world".getBytes());
    nc.publish("INFO330", "databses rule".getBytes());
    System.out.println("Done Publishing: Shutting Down");
    System.exit(0);
  }
}
