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
    private Debug debug;
    private boolean gotForkRemote;

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
                        debug = Debug.STATE1;
                        if(newSeat != null) {
                            debug = Debug.STATE2;
                            takeSeatWhenAvailable(newSeat);
                            startEating();
                            newSeat = null;
                        }
                        else{
                            debug = Debug.STATE3;
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
            if(tryToGetLocalSeat()){
                return true;
            }else{
                SeatProposal seatProposal = searchSeat();
                if(seatProposal.getName().equals(Main.lookupName)) {
                    if(!exit){
                        takeSeatWhenAvailable(TablePart.getTablePart().getSeat(seatProposal.getSeatNumber()));
                        startEating();
                    }
                    return true;
                }
                else{
                    if(!ClientServiceImpl.awakePhilosopherAddToQueueCall(ident, seatProposal.getSeatNumber(), seatProposal.getName(), mealsEaten)){
                        if(!exit){
                            SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();
                            takeSeatWhenAvailable(TablePart.getTablePart().getSeat(ownSeatProposal.getSeatNumber()));
                            startEating();
                        }
                        return true;
                    }
                    return false;
                }
            }
        }
    }

    private boolean tryToGetLocalSeat() {
        for (Seat seat : TablePart.getTablePart().getSeats()){
            if(seat.getQueueSize() == 0) {
                takeSeatWhenAvailable(seat);
                startEating();
                return true;
            }
        }
        return false;
    }

    private void startEating(){
        debug = Debug.STATE7;

        boolean readyToEat = false;
        while (!readyToEat) {
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " tries to get left fork.");
            }
            boolean gotFork = false;
            gotForkRemote = false;
            while (seat != null && !exit && !gotFork) {
                if(seat.getLeftFork() != null || RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)){
                    gotFork = seat.takeLeftForkIfAvailable();
                    if(!gotFork){
                        try {
                            debug = Debug.STATE8;
                            if(seat.getLeftFork() == null) {
                                System.out.println("Ungültig");
                                ClientServiceImpl.leftForkWaitCall();
                            }
                            else {
                                debug = Debug.STATE9;
                                synchronized (seat.getLeftFork().getMonitor()){
                                    debug = Debug.STATE10;
                                    seat.getLeftFork().getMonitor().wait();
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    seat.takeLeftForkIfAvailable();
                    long time = System.currentTimeMillis();
                    try {
                        synchronized (monitor){
                            monitor.wait(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(System.currentTimeMillis() - time > 400){
                        System.out.println("Restore due to timeout");
                        RestoreClient.startRestoring();
                    }
                    else{
                        if(gotForkRemote){
                            gotFork = true;
                        }
                    }
                }
            }
            debug = Debug.STATE11;

            if(exit)
                return;
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " got left fork and tries to get right fork.");
            }
            if (!exit && !seat.takeRightForkIfAvailable()) {
                debug = Debug.STATE12;
                seat.releaseLeftFork();
                debug = Debug.STATE13;

                // Fix problem of all philosopher starting to eat at same time
                if (Math.random() > 0.9) {
                    try {
                        debug = Debug.STATE14;
                        Thread.sleep(10);
                        debug = Debug.STATE15;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                debug = Debug.STATE16;
                if(RestoreClient.isDebugging()) {
                    System.out.println("Right Fork was not available, Philosopher " + ident + " released left fork.");
                }
                debug = Debug.STATE17;
            } else {
                readyToEat = true;
            }
            debug = Debug.STATE18;
        }
        debug = Debug.STATE19;

        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + ident + " starts to eat.");
        }
        try {
            sleep(RestoreClient.getEatTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        debug = Debug.STATE20;
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
        SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();

        if(ownSeatProposal.getWaitingPhilosophersCount() == 0){
            return ownSeatProposal;
        }

        SeatProposal currentBestSeatProposal;
        if(ClientServiceImpl.isRestoringActive()){
            currentBestSeatProposal = ownSeatProposal;
        }
        else {
            currentBestSeatProposal = ClientServiceImpl.getBestExternalProposal();
        }

        if(currentBestSeatProposal.isBetterThen(ownSeatProposal)) {
            if(RestoreClient.isDebugging()) {
                System.out.println("Philosopher " + ident + " found seat on other table, it was better: "+currentBestSeatProposal.getWaitingPhilosophersCount()+"-"+ownSeatProposal.getWaitingPhilosophersCount());
            }

            return currentBestSeatProposal;
        }

        return ownSeatProposal;
    }

    private void takeSeatWhenAvailable(Seat seat) {
        this.seat = seat.getSeatWithSmallesQueue(this);
        debug = Debug.STATE4;

        if(this.seat == null) {
            try {
                debug = Debug.STATE5;

                synchronized (monitor) {
                    debug = Debug.STATE6;
                    monitor.wait();
                }
                debug = Debug.STATE7;
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

    public Debug getDebug() {
        return debug;
    }

    public void setGotForkRemote(boolean gotFork) {
        this.gotForkRemote = gotFork;
    }
}
