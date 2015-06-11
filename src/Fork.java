import java.io.Serializable;
import java.rmi.RemoteException;

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
        available = true;
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

    public boolean takeForkIfAvailable (boolean localCall) {

        synchronized (monitor) {
            if(!localCall) {
                boolean a = false;
                if (available) {
                    available = false;
                    a = true;
                }
                try {
                    RestoreClient.getRightClient().notifyForkAvailable(a);
                } catch (RemoteException e) {
                    System.out.println("takeForkIfAvailable");
                    RestoreClient.startRestoring();
                }
            }
            if (available) {
                available = false;
                return true;
            }
            return false;
        }
    }

    public void releaseFork() {
        available = true;
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public Object getMonitor() {
        return monitor;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}
