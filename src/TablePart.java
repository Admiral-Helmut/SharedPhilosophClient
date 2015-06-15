import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * VSS
 * Created by Admiral Helmut on 18.05.2015.
 */
public class TablePart {

    private List<Seat> seats;
    private static TablePart tablePart;

    public TablePart(int seatAmounts) {

        tablePart = this;
        seats = new ArrayList<>();
        Fork fork = null;
        for (int i = 0; i < seatAmounts; i++) {
            Seat s = new Seat(fork);
            seats.add(s);
            fork = new Fork(s);
        }


    }

    public SeatProposal getBestProposalForCurrentTable() {
        int tableSize = getSeats().size();
        List<Integer> indices = new ArrayList<>();
        for(int i = 0; i < tableSize; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices);

        int currentSmallestQueueSize = RestoreClient.getAllPhilosopher() + RestoreClient.getAllHungryPhilosopher();
        int currentSmallestQueueSeat = -1;

        for(int seatCounter = 0; seatCounter < tableSize; seatCounter++) {
            int currentQueueSize = getQueueSizeForSeat(indices.get(seatCounter));
            if(currentQueueSize == 0){
                return new SeatProposal(indices.get(seatCounter), currentQueueSize, Main.lookupName, Main.ownIP);
            }
            if(currentQueueSize <= currentSmallestQueueSize) {
                currentSmallestQueueSize = currentQueueSize;
                currentSmallestQueueSeat = indices.get(seatCounter);
            }
        }

        return new SeatProposal(currentSmallestQueueSeat, currentSmallestQueueSize, Main.lookupName, Main.ownIP);
    }

    public static TablePart getTablePart() {
        return tablePart;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public Seat getSeat (int index) {
        return seats.get(index);
    }

    public int getQueueSizeForSeat(int index) {
        return seats.get(index).getQueueSize();
    }

    public void restoreSeat() {
        Fork fork = null;
        Seat seat = new Seat(fork);
        fork = new Fork(seat);
        seats.get(0).setLeftFork(fork);
        fork.setRightSeat(seats.get(0));
        seats.add(0, seat);
    }

    public void restoreSeats(int amount) {
        Fork fork = null;
        List<Seat> newSeats = new ArrayList<>(amount);
        for(int i = 0; i < amount; i++){
            Seat seat = new Seat(fork);
            fork = new Fork(seat);
            newSeats.add(seat);
        }
        Seat firstSeat = seats.get(0);
        firstSeat.setLeftFork(fork);
        fork.setRightSeat(firstSeat);
        seats.addAll(0, newSeats);
        Philosopher philosopher = firstSeat.getPhilosopher();
        if(philosopher != null){
            philosopher.setGotForkRemote(true);
            synchronized (philosopher.getMonitor()){
                philosopher.getMonitor().notify();
            }
        }
    }
}
