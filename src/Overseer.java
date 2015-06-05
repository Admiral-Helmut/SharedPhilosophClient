import java.util.List;

/**
 * VSS
 * Created by David on 05.06.2015.
 */
public class Overseer extends Thread {
    private long endTime;
    private List<Philosopher> philosophers;

    public void run() {
        endTime = RestoreClient.getEndTime();
        new PhilosoperUpdater().start();

        while(System.currentTimeMillis() < endTime) {
            //TODO: Overseer stuff
        }
    }


    private class PhilosoperUpdater extends Thread {
        public void run() {
            while(System.currentTimeMillis() < endTime) {
                try {
                    wait(RestoreClient.getMeditationTime()*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ClientServiceImpl.updatePhilosophersForNeighbor(philosophers);
            }
        }
    }
}
