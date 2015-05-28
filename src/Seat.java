import java.util.LinkedList;
import java.util.Queue;

/**
 * VSS
 * Created by Admiral Helmut on 01.04.2015.
 */
public class Seat {

    private Fork leftFork;
    private Fork rightFork;


    public Seat(Fork leftFork){
        this.leftFork = leftFork;
        if(leftFork!=null){
            this.leftFork.setRightSeat(this);
        }


    }

    public void setRightFork(Fork fork) {
        rightFork = fork;
    }
}
