package Service;

import DataLayer.DataInterface;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationService implements Runnable {

    private final ZMQ.Socket notifSock;

    private final DataInterface data;
    private final Map<String, Long> records = new HashMap<>();
    private int minimalRefreshRate = 500;// Default 500 milliseconds
    private int occupationDist = 0;
    private Map<String, Long> typeCount = new HashMap<>();

    /**
     * POSSIBLE SUBSCRIPTIONS:
     * - Dispositivos Online: T1, T2, T3
     * - Record de dispositivos online: record (todos os tipos), recordt1, recordt2, recordt3
     * - aumento
     * - decrescimo
     */


    public NotificationService(ZContext ctx, DataInterface data, String notifAddr) {

        this.notifSock = ctx.createSocket(SocketType.PUB);
        this.notifSock.bind("tcp://" + notifAddr);
        this.data = data;

    }

    public void setMinimalRefreshRate(int minimalRefreshRate) {
        this.minimalRefreshRate = minimalRefreshRate;
    }

    public void checkOnline(Map<String, Long> newTypeCount) {

        Set<String> lost = new HashSet<>(typeCount.keySet());
        lost.removeAll(newTypeCount.keySet());

        for (String type : lost) {
            notifSock.sendMore(type.getBytes(StandardCharsets.UTF_8));
            notifSock.send("Nao ha dispositivos online do tipo " + type);
        }
    }


    public void record(Map<String, Long> newTypeCount) {

        Set<String> newRecords = newTypeCount.entrySet()
                .stream()
                .filter(x -> x.getValue() >= this.typeCount.getOrDefault(x.getKey(), 0L))   // If it's climbing
                .filter(x -> x.getValue() > this.records.getOrDefault(x.getKey(), 0L))      // If it's bigger then the last record
                .peek(x -> records.put(x.getKey(), x.getValue()))                                      // Update entry to the new record
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!newRecords.isEmpty()) {
            for (String type : newRecords) {
                notifSock.sendMore("record" + type);
                notifSock.send("Foi atingido um novo record para o tipo: " + type + " com o valor: " + records.get(type));
            }

            notifSock.sendMore("record");
            notifSock.send("Foi atingido um novo record de users online de qualquer tipo"); // É necessário indicar o valor ??
        }
    }

    private String incDecMSG(String change, int i, int newRatio) {
        return "Houve um " + change + " de dispositivos online, " +
                "tendo atravessado os " + i + "0 %. Percentagem atual de " +
                "utilizadores na zona: " + newRatio + " %.";
    }

    public void increase_decrease() {

        int newDistribution = this.data.getLocalOnlineDistribution(); // in [0, 100] %

        int newRatio = newDistribution / 10; // in [0 , 10]

        int oldRatio = occupationDist / 10; // in [0, 10]

        // increase
        if (newRatio > oldRatio) {

            for (int i = oldRatio + 1; i <= newRatio; i++) {
                notifSock.sendMore("aumento" + (i * 10));
                notifSock.send(incDecMSG("aumento", i, newDistribution));
            }

            // decrease
        } else if (newRatio < oldRatio) {

            for (int i = newRatio + 1; i <= oldRatio; i++) {
                notifSock.sendMore("decrescimo" + (i * 10));
                notifSock.send(incDecMSG("decrescimo", i, newDistribution));
            }
        }

        occupationDist = newDistribution;
    }

    public void handleNewRecords() {
        Map<String, Long> newTypeCount = data.getLocalTypeCount();

        this.checkOnline(newTypeCount);
        this.record(newTypeCount);

        this.typeCount = newTypeCount;
    }


    @Override
    public void run() {
        System.out.println("Started NotificationService");
        try {
            while (!Thread.currentThread().isInterrupted() && data.awaitDeviceChange()) {

                handleNewRecords();
                increase_decrease();

                if (minimalRefreshRate > 0) {
                    try {
                        Thread.sleep(minimalRefreshRate);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        } catch (ZMQException e) {
            if (e.getErrorCode() != 4) {
                System.err.println("Critical Error in NotificationService involving ZMQ");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Critical Error in NotificationService");
            e.printStackTrace();
        } finally {
            notifSock.close();
            System.out.println("Stopped NotificationService");

        }
    }
}

