import Common.Pair;
import DataLayer.AggregatorData;
import DataLayer.DataInterface;
import DataLayer.Device;
import DataLayer.Snapshot;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataLayerSimulation {


    static Consumer<PrintStream> lineBreak = x -> x.println("-".repeat(32));
    static Consumer<PrintStream> lineBreak2 = x -> x.println("+".repeat(32));

    static Lock l = new ReentrantLock();

    private final Random r = new Random();
    public String[] type;
    public Device[] devices;
    public String[] events;
    private final List<Consumer<DataInterface>> genActions = List.of(
            dt -> {
                Device d = rndElem(devices);
                dt.addOnlineDevice(d.name(), d.type());
            },
            dt -> {
                Device d = rndElem(devices);
                dt.removeOnlineDevice(d.name(), d.type());
            },
            DataInterface::addActiveUser,
            DataInterface::removeActiveUser,
            dt -> dt.addEvent(events[r.nextInt(0, events.length)])
    );

    public static void main(String[] argv) {

        DataLayerSimulation sim = new DataLayerSimulation();

        sim.genRandomData(20, 500, 100);
        ExecutorService executor = Executors.newFixedThreadPool(8); // My computer only has 8 PUs :')
        System.out.println("Submitting Simulations to worker");
        IntStream.range(0, 500).forEach(x -> executor.execute(() -> sim.fullEpidemicMergeSimulation(x, 20, 500, 70)));
        System.out.println("Finished submitting all simulations");
        executor.shutdown();
    }

    private void randomStep(DataInterface dt) {
        genActions.get(r.nextInt(0, genActions.size())).accept(dt);
    }

    public <E> E rndElem(E[] array) {
        return array[r.nextInt(0, array.length)];
    }

    public void genRandomData(int typeCount, int deviceCount, int eventCount) {
        type = IntStream.range(0, typeCount).mapToObj(x -> "Type" + x).toArray(String[]::new);
        devices = IntStream.range(0, deviceCount).mapToObj(x -> Device.of("USR" + x, rndElem(type))).toArray(Device[]::new);
        events = IntStream.range(0, eventCount).mapToObj(x -> "Event" + x).toArray(String[]::new);
    }

    private void fullEpidemicMergeSimulation(int id, int nodeCount, int epochs, int stepChance) {
        Map<String, AggregatorData> aggs = IntStream.range(0, nodeCount).mapToObj(x -> "AG" + x).collect(Collectors.toMap(x -> x, AggregatorData::new));

        int logn = (int) Math.log(nodeCount);

        List<String> nodes = new ArrayList<>(aggs.keySet());
        List<Pair<Snapshot, Snapshot>> SERIALIZATION_FAILURES = new ArrayList<>();
        Random rl = new Random();
        // The simulation
        for (int i = 0; i < epochs; i++) {

            Collections.shuffle(nodes);

            for (String node : nodes) {
                if (rl.nextInt(0, 101) <= stepChance)
                    randomStep(aggs.get(node));
            }

            for (String node : nodes) {
                Snapshot snp = aggs.get(node).getSnapshot();
                Snapshot retrieved = Snapshot.deserialize(snp.serialize());
                if (!snp.equals(retrieved))
                    SERIALIZATION_FAILURES.add(Pair.of(snp, retrieved));

                List<String> l = new ArrayList<>(aggs.keySet());
                Collections.shuffle(l);

                l.subList(0, logn).forEach(x -> aggs.get(x).applySnapshot(snp));
            }
        }

        //Final state mash
        aggs.forEach((k, v) -> aggs.forEach((k1, v1) -> v.applySnapshot(v1.getSnapshot())));

        l.lock();
        lineBreak2.accept(System.out);
        System.out.println("Report from simulation: " + id + " (reference: " + nodes.get(0) + ")");

        if (!SERIALIZATION_FAILURES.isEmpty()) {
            SERIALIZATION_FAILURES.forEach(x -> {
                lineBreak.accept(System.out);
                System.out.println("SERIALIZATION FAILURE\n" + x.getFirst() + "\n" + x.getSecond());
                lineBreak.accept(System.out);
            });
        }
        AggregatorData chosenOne = aggs.get(nodes.remove(0));

        // System.out.println(chosenOne);

        nodes.stream().sorted().map(aggs::get).forEach(n ->
        {
            if (!chosenOne.equalStates(n)) {
                lineBreak.accept(System.out);
                System.out.println("DIVERGENCE!");
                System.out.println(chosenOne + "\n" + n);
                lineBreak.accept(System.out);
            }
        });
        System.out.println("End");
        System.out.flush();
        l.unlock();
    }


}


