import com.oracle.deploy.update.Updater;

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
    private Punisher punisher;
    private PhilosoperUpdater updater;

    public Overseer(List<Philosopher> philosophers) {
        this.philosophers = philosophers;
    }

    public void setPhilosophers(List<Philosopher> philosophers) {
        this.philosophers = philosophers;
    }

    public void run() {
        try {
            sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = RestoreClient.getEndTime();
        startPunisher();
        startUpdater();
        //new DisplayOutput().start();
    }

    public void reStartPunisher() {
        punisher.setExit(true);
        startPunisher();
    }

    public void reStartUpdater() {
        updater.setExit(true);
        startUpdater();
    }

    private void startPunisher() {
        punisher = new Punisher();
        punisher.start();
    }

    private void startUpdater() {
        updater = new PhilosoperUpdater();
        updater.start();
    }

    private class DisplayOutput extends Thread {
        public void run() {
            while(System.currentTimeMillis() < endTime) {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(Philosopher philosopher : philosophers) {
                    System.out.println(philosopher.getIdent() + ":" + philosopher.getMealsEaten() + ":" + philosopher.getStatus() + ":" + philosopher.isActive() + ":" + philosopher.isPunished() + ":"+ philosopher.getDebug());
                }
                for(Seat seat : TablePart.getTablePart().getSeats()){
                    System.out.print(seat.getQueueSize() + "-");
                }
                System.out.println("\n");
            }
        }

    }



    private class PhilosoperUpdater extends Thread {
        private boolean exit = false;
        public void run() {
            while(System.currentTimeMillis() < endTime && !exit) {
                try {
                    sleep(RestoreClient.getMeditationTime()*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ClientServiceImpl.updatePhilosophersForNeighborCall(philosophers);
            }
        }

        public void setExit(boolean exit) {
            this.exit = exit;
        }
    }

    private class Punisher extends Thread {
        private boolean exit = false;
        public void run() {
            while(System.currentTimeMillis() < endTime && !exit) {
                try {
                    sleep(RestoreClient.getMeditationTime()*3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(RestoreClient.isDebugging()) {
                    System.out.println("Punisher starts punishing");
                }

                ClientServiceImpl.updateAverageCall();
                if(!exit){
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

        public void setExit(boolean exit) {
            this.exit = exit;
        }
    }

    public static int getAverage() {
        return average;
    }

    public static void setAverage(int average) {
        Overseer.average = average;
    }
}
