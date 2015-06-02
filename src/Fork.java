import java.io.Serializable;

/**
 * VSS
 * Created by Admiral Helmut on 20.05.2015.
 */
public class Fork implements Serializable {


    private Seat rightSeat;
    private Seat leftSeat;
    private boolean available;
    private final Object monitor = new Object();

    public Fork(Seat seat){
        this.leftSeat = seat;
        this.leftSeat.setRightFork(this);
    }

    public void setRightSeat(Seat seat) {
        this.rightSeat = seat;
    }

    public void setLeftSeat(Seat seat) {
        this.leftSeat = seat;
    }

    public Seat getRightSeat() {
        return rightSeat;
    }

    public Seat getLeftSeat() {
        return leftSeat;
    }

    public boolean takeForkIfAvailable () {
        synchronized (monitor) {
            if (available) {
                available = false;
                return true;
            }
            return false;
        }
    }

    public void releaseFork() {
        available = true;
        if(rightSeat != null) {
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
        else {
            ClientServiceImpl.notifyReleaseRightForkCall();
        }
    }

    public Object getMonitor() {
        return monitor;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
