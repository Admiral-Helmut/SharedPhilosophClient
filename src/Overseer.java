import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VSS
 * Created by David on 05.06.2015.
 */
public class Overseer extends Thread {
    private long endTime;
    private List<Philosopher> philosophers;

    public Overseer(CopyOnWriteArrayList<Philosopher> philosophers) {
        this.philosophers = philosophers;
    }

    public void run() {
        endTime = RestoreClient.getEndTime();
        new PhilosoperUpdater().start();
        new Punisher().start();

        while(System.currentTimeMillis() < endTime) {
            //TODO: Overseer stuff
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


                List<Integer> averageList = ClientServiceImpl.updateAverageCall(Main.lookupName);

                long ownSum = 0;
                int ownCount = 0;
                for (Philosopher philosopher : philosophers) {
                    if(philosopher.isActive()) {
                        ownSum += philosopher.getMealsEaten();
                        ownCount ++;
                    }
                }
                if(ownCount > 0) {
                    averageList.add((int)(ownSum / ownCount));
                }

                long sum = 0;
                long count = 0;
                for(int value : averageList) {
                    sum += value;
                    count ++;
                }

                int average = ((int)(sum / count));

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

}
