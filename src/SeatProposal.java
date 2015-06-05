import java.io.Serializable;

/**
 * VSS
 * Created by David on 01.06.2015.
 */
public class SeatProposal implements Comparable<SeatProposal>, Serializable{
    private int seatNumber;
    private int waitingPhilosophersCount;
    private String name;
    private String ip;

    public SeatProposal(int seatNumber, int waitingPhilosophersCount, String name, String ip) {
        this.seatNumber = seatNumber;
        this.waitingPhilosophersCount = waitingPhilosophersCount;
        this.name = name;
        this.ip = ip;
    }



    public int compareTo(SeatProposal other) {
        if(waitingPhilosophersCount > other.getWaitingPhilosophersCount()) {
            return 1;
        }
        if(waitingPhilosophersCount < other.getWaitingPhilosophersCount()) {
            return -1;
        }
        return 0;
    }

    public int getWaitingPhilosophersCount() {
        return waitingPhilosophersCount;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }
}
