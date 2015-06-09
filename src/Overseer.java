import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VSS
 * Created by David on 05.06.2015.
 */
public class Overseer extends Thread {
    private long endTime;
    private List<Philosopher> philosophers;
    private static int average;

    public Overseer(List<Philosopher> philosophers) {
        this.philosophers = philosophers;
    }

    public void run() {
        try {
            sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = RestoreClient.getEndTime();
        new PhilosoperUpdater().start();
        new Punisher().start();
        new RMIistdoofThread().start();
        while(System.currentTimeMillis() < endTime) {
            //TODO: Overseer stuff
        }
    }

    private class RMIistdoofThread extends Thread {
        public void run() {
            while(System.currentTimeMillis() < endTime) {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(Philosopher philosopher : philosophers) {
                    System.out.println(philosopher.getIdent() + ":" + philosopher.getMealsEaten() + ":" + philosopher.getStatus() + ":" + philosopher.isActive() + ":" + philosopher.isPunished());
                }
                for(Seat seat : TablePart.getTablePart().getSeats()){
                    System.out.print(seat.getQueueSize() + "-");
                }
                System.out.println("\n");
            }
        }
    }



    private class PhilosoperUpdater extends Thread {
        public void run() {
            while(System.currentTimeMillis() < endTime) {
                try {
                    sleep(RestoreClient.getMeditationTime()*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ClientServiceImpl.updatePhilosophersForNeighborCall(philosophers);
            }
        }
    }

    private class Punisher extends Thread {
        public void run() {
            while(System.currentTimeMillis() < endTime) {
                try {
                    sleep(RestoreClient.getMeditationTime()*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(RestoreClient.isDebugging()) {
                    System.out.println("Punisher starts punishing");
                }
                System.out.println("qwertz");

                ClientServiceImpl.updateAverageCall();

                for (Philosopher philosopher : philosophers) {
                    if(philosopher.isActive()) {
                        if(!philosopher.isPunished() && philosopher.getMealsEaten() > average + 20) {
                            if(RestoreClient.isDebugging()) {
                                System.out.println("Philosopher " + philosopher.getIdent() + " got punished because he eat "
                                + philosopher.getMealsEaten() + " which is more then average of " + average + "!");
                            }
                            philosopher.setPunished(true);
                        }
                    }
                }

            }
        }
    }

    public static int getAverage() {
        return average;
    }

    public static void setAverage(int average) {
        Overseer.average = average;
    }
}
