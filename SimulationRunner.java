import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimulationRunner {
    public static final String file = "steady_state.txt";

    public static void saveSnapshot(PetriNet net, double transientTime) {
        saveSnapshot(net, transientTime, file);
    }

    public static void saveSnapshot(PetriNet net, double transientTime, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, StandardCharsets.UTF_8))) {
            writer.println("Transient_time=" + transientTime);
            for (Place p : net.getListP()) {
                writer.println("PLACE:" + p.getName() + "=" + p.getMark());
            }
            for (Transition t : net.getListT()) {
                writer.println("TRANSITION:" + t.getName() + "=" + t.getBuffer());
            }
            System.out.println("Steady-state snapshot saved to file: " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to save snapshot: " + e.getMessage());
        }
    }

    public static NetworkMetrics runFromSnapshot(NetworkConfig config, double observationDuration) {
        return runFromSnapshot(config, observationDuration, file);
    }

    public static NetworkMetrics runFromSnapshot(NetworkConfig config, double observationDuration, String fileName) {
        NetworkConfig cfg = config;
        PetriNet net = PhoneNetworkModel.createNet(cfg);
        applySnapshotToNet(net, cfg, fileName);

        PetriSim sim = new PetriSim(net);
        sim.setSimulationTime(observationDuration);
        sim.go();

        return new NetworkMetrics(sim, cfg.getFreeL1Count(), cfg.getFreeL2Count());
    }

    private static void applySnapshotToNet(PetriNet net, NetworkConfig config, String fileName) {
        Map<String, Integer> placeMarks = new LinkedHashMap<>();
        Map<String, Integer> transBuffers = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("PLACE:")) {
                    String payload = line.substring(6);
                    String[] parts = payload.split("=");
                    if (parts.length == 2) {
                        placeMarks.put(parts[0], Integer.parseInt(parts[1]));
                    }
                } else if (line.startsWith("TRANSITION:")) {
                    String payload = line.substring(11);
                    String[] parts = payload.split("=");
                    if (parts.length == 2) {
                        transBuffers.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load snapshot, using cold start: " + e.getMessage());
            return;
        }

        int retryQueue = placeMarks.getOrDefault("P_QueueL1", 0) + transBuffers.getOrDefault("T_Retry", 0);
        int connectQueue = placeMarks.getOrDefault("P_ConnectQueue", 0);
        int dialing = placeMarks.getOrDefault("P_WaitDial", 0) + transBuffers.getOrDefault("T_Dial", 0);
        int talking = placeMarks.getOrDefault("P_WaitTalk", 0) + transBuffers.getOrDefault("T_Talk", 0);

        int l1 = config.getFreeL1Count();
        int l2 = config.getFreeL2Count();

        dialing = Math.min(dialing, Math.min(l1, l2));
        int remainingL1 = l1 - dialing;

        connectQueue = Math.min(connectQueue, remainingL1);
        remainingL1 -= connectQueue;

        talking = Math.min(talking, remainingL1);
        int freeL1 = remainingL1 - talking;
        int freeL2 = l2 - dialing;

        for (Place p : net.getListP()) {
            String name = p.getName();
            if (name.equals("P_NewUsersReady")) {
                p.setMark(1);
            } else if (name.equals("P_FreeL1")) {
                p.setMark(freeL1);
            } else if (name.equals("P_FreeL2")) {
                p.setMark(freeL2);
            } else if (name.equals("P_QueueL1")) {
                p.setMark(retryQueue);
            } else if (name.equals("P_ConnectQueue")) {
                p.setMark(connectQueue);
            } else if (name.equals("P_WaitDial")) {
                p.setMark(dialing);
            } else if (name.equals("P_WaitTalk")) {
                p.setMark(talking);
            } else {
                p.setMark(0);
            }
        }
    }

    public static PetriNet prepareNetFromSnapshot(NetworkConfig config, String snapshotFile) {
        NetworkConfig cfg = (config != null) ? config : new NetworkConfig();
        PetriNet net = PhoneNetworkModel.createNet(cfg);
        applySnapshotToNet(net, cfg, snapshotFile);
        return net;
    }
}