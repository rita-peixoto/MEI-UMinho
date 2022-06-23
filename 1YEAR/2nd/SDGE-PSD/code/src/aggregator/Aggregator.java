import DataLayer.AggregatorData;
import Service.AGService;
import Service.CollectorSink;
import Service.NetworkManager;
import Service.NotificationService;
import org.zeromq.ZContext;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.IntStream;

public class Aggregator {

    public static void main(String[] args) {
        try (ZContext ctx = new ZContext()) {
            // For convenience purposes, we store the address of the aggregator in a file so that other lauches can automaticly search
            // the file for possible endpoints to the network. This is totally optional and be ignored and discarded if one so chooses.
            // The system doesn't rely on this in any shape or form
            FileIPSet IPSet = new FileIPSet(args.length > 1 ? args[1] : "IPSet.txt");
            if (args.length > 0)
                IPSet.registerTo(args[0]);
            else
                IPSet.registerRandom();

            String netAddr = IPSet.getAddress();
            String serviceAddr = "*:" + (IPSet.getPort() + 1);
            String collAddr = "*:" + (IPSet.getPort() + 2);
            String notifAddr = "*:" + (IPSet.getPort() + 3);

            System.out.println("Service bound to *:" + (IPSet.getPort() + 1));

            AggregatorData business = new AggregatorData(netAddr);

            NetworkManager net = new NetworkManager(ctx, business, netAddr);
            CollectorSink colls = new CollectorSink(ctx, business, collAddr);
            NotificationService notif = new NotificationService(ctx, business, notifAddr);
            AGService server = new AGService(ctx, business, serviceAddr);

            if (args.length > 2) {
                IntStream.range(2, args.length).boxed().forEach(i -> {
                    try {
                        net.makeContact(args[i]);
                    } catch (Exception ignored) {
                    }
                });
            }

            Thread serverThread = new Thread(server, "Server");
            Thread sinkThread = new Thread(colls, "Sink");
            Thread notifThread = new Thread(notif, "Notifications");
            Thread netThread = new Thread(() -> net.accept(IPSet), "Network");
            Thread cleaner = new Thread(
                    () -> {
                        netThread.interrupt(); // The network Thread must be interrupted in order for it to do cleanups
                        serverThread.interrupt();
                        sinkThread.interrupt();
                        notifThread.interrupt();

                        IPSet.unregister();
                        try {
                            netThread.join();
                            serverThread.join();
                            sinkThread.join();
                            notifThread.join();
                        } catch (InterruptedException ignored) {
                        }
                    });

            Runtime.getRuntime().addShutdownHook(cleaner);

            serverThread.start();
            sinkThread.start();
            notifThread.start();
            netThread.start();

            try {
                if (!awaitStopInput()) {
                    // If it can't receive a stop (aka STDIN is/was close) halt and wait indefinitely for the workers
                    netThread.join();
                    serverThread.join();
                    sinkThread.join();
                    notifThread.join();
                }
            } finally {
                cleaner.start();
                cleaner.join();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean awaitStopInput() {
        try (Scanner in = new Scanner(System.in)) {
            String s = "start";

            while (!Objects.equals(s, "stop")) {
                s = in.nextLine();
            }
            return true;
        } catch (IllegalStateException | NoSuchElementException ignored) {
            return false;
        }
    }

}
