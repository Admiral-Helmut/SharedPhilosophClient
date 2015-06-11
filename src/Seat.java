import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * VSS
 * Created by Admiral Helmut on 01.04.2015.
 */
public class Seat {

    private Fork leftFork;
    private Fork rightFork;
    private Philosopher philosopher;
    private BlockingQueue<Philosopher> waitingPhilosophers = new ArrayBlockingQueue(RestoreClient.getAllHungryPhilosopher()+RestoreClient.getAllPhilosopher());
    private Object monitor = new Object();

    public Seat(Fork leftFork) {
        this.leftFork = leftFork;
        if (leftFork != null) {
            this.leftFork.setRightSeat(this);
        }
    }

    public Seat getSeatWithSmallesQueue(Philosopher philosopher) {
        int leftQueueSize = getQueueSizeOfLeftNeighbour();
        int rightQueueSize = getQueueSizeOfRightNeighbour();
        int ownQueueSize = getQueueSize();
        if ((leftQueueSize < 0 || ownQueueSize <= leftQueueSize) && (rightQueueSize < 0 || ownQueueSize <= rightQueueSize)) {
            synchronized (monitor) {
                waitingPhilosophers.add(philosopher);
                if (isAvailable()) {
                    this.philosopher = philosopher;
                    return this;
                } else {
                    return null;
                }
            }
        } else {
            if (RestoreClient.isDebugging()) {
                System.out.println("Changed seat, was: " + leftQueueSize + "-" + ownQueueSize + "-" + rightQueueSize);
            }
            if (leftQueueSize >= 0 && (rightQueueSize < 0 || leftQueueSize <= rightQueueSize)) {
                return getLeftNeighbour().getSeatWithSmallesQueue(philosopher);
            } else {
                return getRightNeighbour().getSeatWithSmallesQueue(philosopher);
            }
        }
    }


    public void setRightFork(Fork fork) {
        rightFork = fork;
    }

    public Queue<Philosopher> getWaitingPhilosophers() {
        return waitingPhilosophers;
    }

    public int getQueueSize() {
        return waitingPhilosophers.size();
    }

    public boolean isAvailable() {
        return philosopher == null;
    }

    public Seat getLeftNeighbour() {
        if (leftFork != null) {
            return leftFork.getLeftSeat();
        } else {
            return null;
        }
    }

    public Seat getRightNeighbour() {
        return rightFork.getRightSeat();
    }

    public int getQueueSizeOfLeftNeighbour() {
        if (leftFork == null) {
            return -1;
        } else {
            return getLeftNeighbour().getQueueSize();
        }
    }

    public int getQueueSizeOfRightNeighbour() {
        if (rightFork.getRightSeat() == null) {
            return -1;
        } else {
            return getRightNeighbour().getQueueSize();
        }
    }

    public Fork getLeftFork() {
        return leftFork;
    }

    public Fork getRightFork() {
        return rightFork;
    }

    public void removePhilosopher() {
        synchronized (monitor){
            releaseLeftFork();
            getRightFork().releaseFork();
            if(waitingPhilosophers.size() > 0){
                waitingPhilosophers.remove();
                philosopher = waitingPhilosophers.peek();
                if (philosopher != null) {
                    philosopher.setSeat(this);
                    synchronized (philosopher.getMonitor()) {
                        philosopher.getMonitor().notifyAll();
                    }
                }
            }
        }
    }

    public void releaseLeftFork() {
        if(leftFork != null) {
            leftFork.releaseFork();
        }
        else{
            if(RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)) {
                TablePart.getTablePart().getSeat(TablePart.getTablePart().getSeats().size()-1).getRightFork().releaseFork();
            }
            else {
                ClientServiceImpl.notifyReleaseLeftForkCall();
            }
        }
    }


    public boolean takeLeftForkIfAvailable() {
        if (leftFork != null) {
            return leftFork.takeForkIfAvailable(true);
        } else {
            if(RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)) {
                return TablePart.getTablePart().getSeat(TablePart.getTablePart().getSeats().size()-1).getRightFork().takeForkIfAvailable(true);
            }
            else{
                return ClientServiceImpl.takeForkIfAvailableCall();
            }
        }
    }

    public void setLeftFork(Fork leftFork) {
        this.leftFork = leftFork;
    }

    public void setWaitingPhilosophers(BlockingQueue<Philosopher> waitingPhilosophers) {
        this.waitingPhilosophers = waitingPhilosophers;
    }

    public Object getMonitor() {
        return monitor;
    }

    public void setPhilosopher(Philosopher philosopher) {
        this.philosopher = philosopher;
    }


}