import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * VSS
 * Created by Admiral Helmut on 20.05.2015.
 */
public class Philosopher extends Thread {

    private boolean hungry;
    private int ident;
    private boolean active;
    private final Object monitor = new Object();
    private Status status;
    private boolean punished = false;
    private Seat seat;
    private Seat newSeat;
    private int mealsEaten;
    private long endTime;
    private ClientRemote leftClient;
    private ClientRemote rightClient;

    public Philosopher(int ident, boolean hungry, boolean active, String leftNeighbourIP, String leftNeighbourLookup){

        this.ident = ident;
        this.hungry = hungry;
        this.active = active;
        this.status = Status.MEDITATING;
        try {
            leftClient= (ClientRemote) Naming.lookup("rmi://" + leftNeighbourIP + "/" + leftNeighbourLookup);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void run(){
        endTime = RestoreClient.getEndTime();
        while(System.currentTimeMillis()<=endTime){
            if(!active) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while(active){
                switch(status){
                    case MEDITATING:
                        doMeditating();
                        status = Status.EATING;
                        break;
                    case EATING:
                        if(newSeat != null) {
                            takeSeatWhenAvailable(newSeat);
                            startEating();
                            newSeat = null;
                        }
                        else{
                            active = tryToEat();
                        }
                        break;
                    case SLEEPING:
                        doSleeping();
                        status = Status.MEDITATING;
                        break;

                }
            }

        }

        System.out.println("Philosopher finished with " + mealsEaten + " meals Eaten he was " + ((active) ? "activ" : "inaktiv") + ".");


    }

    private boolean tryToEat() {
        if(isPunished()) {
            status = Status.MEDITATING;
            try {
                sleep(RestoreClient.getMeditationTime() * 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setPunished(false);
            return true;
        }else {
            SeatProposal seatProposal = searchSeat();
            if(seatProposal.getName().equals(Main.lookupName)) {
                takeSeatWhenAvailable(TablePart.getTablePart().getSeat(seatProposal.getSeatNumber()));
                startEating();
                return true;
            }
            else{
                ClientServiceImpl.awakePhilosopherAddToQueueCall(ident, seatProposal.getSeatNumber(), seatProposal.getName(), mealsEaten);
                return false;
            }
        }
    }
    private void startEating(){

        boolean readyToEat = false;
        while (!readyToEat) {
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " tries to get left fork.");
            }
            while (!seat.takeLeftForkIfAvailable()) {
                try {
                    if(seat.getLeftFork() == null) {
                        ClientServiceImpl.leftForkWaitCall();
                    }
                    else {
                        synchronized (seat.getLeftFork().getMonitor()){
                            seat.getLeftFork().getMonitor().wait();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " got left fork and tries to get right fork.");
            }
            if (!seat.takeRightForkIfAvailable()) {
                seat.releaseLeftFork();

                // Fix problem of all philosopher starting to eat at same time
                if (Math.random() > 0.9) {
                    try {
                        sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(RestoreClient.isDebugging()) {
                    System.out.println("Right Fork was not available, Philosopher " + ident + " released left fork.");
                }
            } else {
                readyToEat = true;
            }
        }

        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " starts to eat.");
        }
        try {
            sleep(RestoreClient.getEatTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mealsEaten++;
        seat.removePhilosopher();
        seat = null;
        if(mealsEaten % 3 == 0) {
            status = Status.SLEEPING;
        }
        else{
            status = Status.MEDITATING;
        }
        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " finished his " + mealsEaten + ". meal.");
        }
    }

    private SeatProposal searchSeat() {
        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " tries to find seat.");
        }
        try {
            SeatProposal currentBestSeatProposal = leftClient.searchSeat(Main.lookupName);
            SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();

            if(currentBestSeatProposal.isBetterThen(ownSeatProposal)) {
                if(RestoreClient.isDebugging()) {
                    System.out.println("Philosopher " + ident + " found seat on other table, it was better: "+currentBestSeatProposal.getWaitingPhilosophersCount()+"-"+ownSeatProposal.getWaitingPhilosophersCount());
                }
                return currentBestSeatProposal;
            }
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " found seat on own table.");
            }
            return ownSeatProposal;

        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return new SeatProposal(-1, -1, "", "");
    }

    private void takeSeatWhenAvailable(Seat seat) {
        this.seat = seat.getSeatWithSmallesQueue(this);

        if(this.seat == null) {
            try {
                synchronized (monitor) {
                    monitor.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void doMeditating(){
        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " starts meditating.");
        }
        try {
            sleep(hungry ? RestoreClient.getMeditationTime()/2 : RestoreClient.getMeditationTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doSleeping(){
        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " starts sleeping.");
        }
        try {
            sleep(RestoreClient.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public boolean isPunished() {
        return punished;
    }

    public void setPunished(boolean punished) {
        this.punished = punished;
    }

    public void setSeat(Seat seat) { this.seat = seat; }

    public Object getMonitor() {
        return monitor;
    }

    public void setMealsEaten(int mealsEaten) {
        this.mealsEaten = mealsEaten;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return active;
    }

    public int getMealsEaten() {
        return mealsEaten;
    }

    public int getIdent() {
        return ident;
    }

    public void setNewSeat(Seat newSeat) {
        this.newSeat = newSeat;
    }
}
