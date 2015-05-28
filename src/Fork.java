import java.io.Serializable;

/**
 * Created by Admiral Helmut on 20.05.2015.
 */
public class Fork implements Serializable {


    private Seat rightSeat;
    private Seat leftSeat;

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
}
