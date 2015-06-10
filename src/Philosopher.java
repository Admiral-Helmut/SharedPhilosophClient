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
    private boolean exit = false;

    public Philosopher(int ident, boolean hungry, boolean active){

        this.ident = ident;
        this.hungry = hungry;
        this.active = active;
        this.status = Status.MEDITATING;
    }

    public Philosopher(int ident, boolean hungry, int mealsEaten, boolean active) {
        this.ident = ident;
        this.hungry = hungry;
        this.active = active;
        this.status = Status.MEDITATING;
        this.mealsEaten = mealsEaten;
    }

    public void run(){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = RestoreClient.getEndTime();
        while(System.currentTimeMillis()<=endTime && !exit){
            if(!active) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while(active && System.currentTimeMillis()<=endTime && !exit){
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

        if(!exit)
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
                if(!exit)
                    startEating();
                return true;
            }
            else{
                if(!ClientServiceImpl.awakePhilosopherAddToQueueCall(ident, seatProposal.getSeatNumber(), seatProposal.getName(), mealsEaten)){
                    SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();
                    takeSeatWhenAvailable(TablePart.getTablePart().getSeat(ownSeatProposal.getSeatNumber()));
                    if(!exit)
                        startEating();
                    return true;
                }
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
            while (seat != null && !seat.takeLeftForkIfAvailable() && !exit) {
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
            if(exit)
                return;
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
            SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();

            SeatProposal currentBestSeatProposal;
            if(ClientServiceImpl.isRestoringActive()){
                currentBestSeatProposal = ownSeatProposal;
            }
            else {
                currentBestSeatProposal = RestoreClient.getLeftClient().searchSeat(Main.lookupName, ident);
            }

            if(currentBestSeatProposal.isBetterThen(ownSeatProposal)) {
                if(RestoreClient.isDebugging()) {
                    System.out.println("Philosopher " + ident + " found seat on other table, it was better: "+currentBestSeatProposal.getWaitingPhilosophersCount()+"-"+ownSeatProposal.getWaitingPhilosophersCount());
                }

                return currentBestSeatProposal;
            }

            return ownSeatProposal;

        } catch (RemoteException e) {
            RestoreClient.startRestoring();
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

    public Status getStatus() {
        return status;
    }

    public boolean isHungry() {
        return hungry;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }
}
