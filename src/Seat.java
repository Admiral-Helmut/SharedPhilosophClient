import java.util.LinkedList;
import java.util.Queue;

/**
 * VSS
 * Created by Admiral Helmut on 01.04.2015.
 */
public class Seat {

    private int seatNumber;
    private Fork rightFork;
    private Fork leftFork;
    private Philosopher philosopher;
    private Queue<Philosopher> waitingPhilosophers = new LinkedList<>();

    public Seat(int seatNumber) {
        if(Main.debugging) {
            System.out.println("Created seat " + seatNumber);
        }
        this.seatNumber = seatNumber;

    }

    public void setRightFork(Fork rightFork) {
        this.rightFork = rightFork;
    }

    public void setLeftFork(Fork leftFork) {
        this.leftFork = leftFork;
    }

    public Fork getRightFork() {
        return rightFork;
    }

    public Fork getLeftFork() {
        return leftFork;
    }

    public void removePhilosopher(){
        getLeftFork().releaseFork();
        getRightFork().releaseFork();
        waitingPhilosophers.remove();
        philosopher = waitingPhilosophers.peek();
        if(philosopher != null) {
            philosopher.setSeat(this);
            synchronized (philosopher) {
                philosopher.notify();
            }
        }
    }

    public synchronized Seat getSeatWithSmallesQueue(Philosopher philosopher) {
        int leftQueueSize = getLeftNeighbour().getQueueSize();
        int rightQueueSize = getRightNeighbour().getQueueSize();
        int ownQueueSize = getQueueSize();

        if(ownQueueSize <= leftQueueSize && ownQueueSize <= rightQueueSize) {
            waitingPhilosophers.add(philosopher);
            if (isAvailable()) {
                this.philosopher = philosopher;
                return this;
            }
            else {
                return null;
            }
        }
        else {
            if(Main.debugging) {
                System.out.println("Changed seat, was: " + leftQueueSize + "-" + ownQueueSize + "-" + rightQueueSize);
            }
            if (leftQueueSize <= rightQueueSize) {
                return  getLeftNeighbour().getSeatWithSmallesQueue(philosopher);
            }
            else {
                return getRightNeighbour().getSeatWithSmallesQueue(philosopher);
            }
        }
    }

    private boolean isAvailable() {
        return philosopher == null;
    }

    public int getQueueSize(){
        return waitingPhilosophers.size();
    }

    private Seat getLeftNeighbour() {
        return leftFork.getLeftSeat();
    }

    private Seat getRightNeighbour() {
        return rightFork.getRightSeat();
    }
}
