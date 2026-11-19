import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class PhoneNetworkModel {
    public static PetriNet createNet(NetworkConfig config) {
        NetworkConfig cfg = (config != null) ? config : new NetworkConfig();

        Place.initNext();
        Transition.initNext();
        Arc.initNext();

        ArrayList<Place> places = new ArrayList<>();
        ArrayList<Transition> transitions = new ArrayList<>();
        ArrayList<ArcIn> inArcs = new ArrayList<>();
        ArrayList<ArcOut> outArcs = new ArrayList<>();

        Place pNewUsersReady = new Place("P_NewUsersReady", cfg.getInitialUsersReady());
        Place pTotalArrived = new Place("P_TotalArrived", 0);
        Place pQueueL1 = new Place("P_QueueL1", cfg.getInitialQueueL1());
        Place pRetryCount = new Place("P_RetryCount", 0);
        Place pFreeL1 = new Place("P_FreeL1", cfg.getFreeL1Count());
        Place pConnectQueue = new Place("P_ConnectQueue", 0);
        Place pFreeL2 = new Place("P_FreeL2", cfg.getFreeL2Count());
        Place pWaitDial = new Place("P_WaitDial", 0);
        Place pWaitTalk = new Place("P_WaitTalk", 0);
        Place pServed = new Place("P_Served", 0);

        places.add(pNewUsersReady);
        places.add(pTotalArrived);
        places.add(pQueueL1);
        places.add(pRetryCount);
        places.add(pFreeL1);
        places.add(pConnectQueue);
        places.add(pFreeL2);
        places.add(pWaitDial);
        places.add(pWaitTalk);
        places.add(pServed);

        Transition tArrival = new Transition("T_Arrival", "exp", cfg.getArrivalMean(), 0.0, cfg.getArrivalMin(), cfg.getArrivalMax(), 0);
        Transition tRetry = new Transition("T_Retry", "norm", cfg.getRetryMean(), cfg.getRetryDev(), cfg.getRetryMin(), cfg.getRetryMax(), cfg.getPriorityRetry());
        Transition tTakeL1 = new Transition("T_TakeL1", 0.0, cfg.getPriorityTakeL1());
        Transition tTakeL2 = new Transition("T_TakeL2", 0.0, 0);
        Transition tDial = new Transition("T_Dial", "exp", cfg.getDialMean(), 0.0, cfg.getDialMin(), cfg.getDialMax(), 0);
        Transition tTalk = new Transition("T_Talk", "exp", cfg.getTalkMean(), 0.0, cfg.getTalkMin(), cfg.getTalkMax(), 0);

        transitions.add(tArrival);
        transitions.add(tRetry);
        transitions.add(tTakeL1);
        transitions.add(tTakeL2);
        transitions.add(tDial);
        transitions.add(tTalk);

        inArcs.add(new ArcIn(pNewUsersReady, tArrival));
        inArcs.add(new ArcIn(pQueueL1, tRetry));
        inArcs.add(new ArcIn(pQueueL1, tTakeL1));
        inArcs.add(new ArcIn(pFreeL1, tTakeL1));
        inArcs.add(new ArcIn(pConnectQueue, tTakeL2));
        inArcs.add(new ArcIn(pFreeL2, tTakeL2));
        inArcs.add(new ArcIn(pWaitDial, tDial));
        inArcs.add(new ArcIn(pWaitTalk, tTalk));

        outArcs.add(new ArcOut(tArrival, pNewUsersReady));
        outArcs.add(new ArcOut(tArrival, pQueueL1));
        outArcs.add(new ArcOut(tArrival, pTotalArrived));
        outArcs.add(new ArcOut(tRetry, pQueueL1));
        outArcs.add(new ArcOut(tRetry, pRetryCount));
        outArcs.add(new ArcOut(tTakeL1, pConnectQueue));
        outArcs.add(new ArcOut(tTakeL2, pWaitDial));
        outArcs.add(new ArcOut(tDial, pWaitTalk));
        outArcs.add(new ArcOut(tDial, pFreeL2));
        outArcs.add(new ArcOut(tTalk, pServed));
        outArcs.add(new ArcOut(tTalk, pFreeL1));

        return new PetriNet("TelephoneNetworkModel", places, transitions, inArcs, outArcs);
    }

    public static void doVerification() {
        System.out.printf("%-3s | %-7s | %-7s | %-8s | %-8s | %-9s | %-7s | %-7s | %-8s\n", "No", "Served", "Retries", "T_dial", "T_talk", "T_total", "U_L1", "U_L2", "P_retry");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        double simTime = 10000.0;
        NetworkConfig[] configs = {
            new NetworkConfig(),
            new NetworkConfig().setLines(2, 1),
            new NetworkConfig().setLines(5, 1),
            new NetworkConfig().setLines(10, 2),
            new NetworkConfig().setLines(15, 3),
            new NetworkConfig().setArrival(5.0, 0.5, 20.0),
            new NetworkConfig().setArrival(0.35, 0.05, 2.0),
            new NetworkConfig().setTalk(2.0, 0.5, 10.0),
            new NetworkConfig().setTalk(20.0, 2.0, 60.0),
            new NetworkConfig().setRetry(2.0, 0.5, 0.5, 6.0),
            new NetworkConfig().setDial(1.5, 0.1, 5.0),
            new NetworkConfig()
                    .setArrival(1.0, 0.0, Double.MAX_VALUE)
                    .setRetry(15.0, 2.0, 0.0, Double.MAX_VALUE)
                    .setDial(0.2, 0.0, Double.MAX_VALUE)
                    .setTalk(10.0, 0.0, Double.MAX_VALUE)
        };

        for (int i = 0; i < configs.length; i++) {
            NetworkConfig cfg = configs[i];
            PetriNet net = PhoneNetworkModel.createNet(cfg);

            PetriSim sim = new PetriSim(net);
            sim.setSimulationTime(simTime);
            sim.go();

            NetworkMetrics metrics = new NetworkMetrics(sim, cfg.getFreeL1Count(), cfg.getFreeL2Count());

            System.out.printf(java.util.Locale.US, "%-3d | %-7d | %-7d | %-8.4f | %-8.4f | %-9.4f | %-7.4f | %-7.4f | %-8.4f\n",
                    (i + 1),
                    metrics.getServed(),
                    metrics.getRetries(),
                    metrics.getDialDuration(),
                    metrics.getTalkDuration(),
                    metrics.getTotalDuration(),
                    metrics.getLoadL1(),
                    metrics.getLoadL2(),
                    metrics.getRetryProbability());
        }

        System.out.println("=========================================================================================================");
    }

    public static void generateEventsProtocol(String fileName, double maxLogTime) {
        NetworkConfig config = new NetworkConfig().setRetry(15.0, 2.0, 5.0, 25.0);
        PetriNet net = createNet(config);

        PetriSim sim = new PetriSim(net);
        sim.setSimulationTime(maxLogTime);
        sim.setTimeCurr(0.0);
        sim.input();

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, StandardCharsets.UTF_8))) {
            writer.println("Events protocol");
            writer.println("Mark in Net " + net.getName() + ":");
            writer.println(formatMarking(net));

            while (sim.getCurrentTime() < maxLogTime && !sim.isStop()) {
                sim.eventMin();
                Transition currentEvent = sim.getEventMin();
                double nextTime = sim.getTimeMin();

                if (currentEvent == null || nextTime == Double.MAX_VALUE || nextTime > maxLogTime) {
                    break;
                }

                sim.setTimeCurr(nextTime);

                writer.println();
                writer.println("Time progress: time = " + sim.getCurrentTime());
                writer.println("NextEvent: time = " + sim.getCurrentTime() + "   Event '" + currentEvent.getName() + "'");

                sim.output();
                writer.println("Markers leave transitions:");
                writer.println("Mark in Net " + net.getName() + ":");
                writer.println(formatMarking(net));

                sim.input();
                writer.println("Markers enter transitions:");
                writer.println("Mark in Net " + net.getName() + ":");
                writer.println(formatMarking(net));
            }
        } catch (IOException e) {
            System.err.println("Failed to write events protocol: " + e.getMessage());
        }
    }

    private static String formatMarking(PetriNet net) {
        StringBuilder sb = new StringBuilder();
        for (Place p : net.getListP()) {
            sb.append(p.getMark()).append("  ");
        }
        return sb.toString().trim();
    }
}