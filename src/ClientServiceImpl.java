import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * VSS
 * Created by admiralhelmut on 01.05.15.
 */
public class ClientServiceImpl extends UnicastRemoteObject implements ClientRemote {


    private static HashMap<String, ClientRemote> neighbourList;
    MasterRemote master;
    String masterName;
    private List<Philosopher> philosophers;

    TablePart tablePart = null;
    private static Object leftForkMonitor = new Object();
    private static boolean leftForkAvailable = true;
    protected ClientServiceImpl() throws RemoteException {

        neighbourList = new HashMap<>();
    }

    @Override
    public boolean checkClient() throws RemoteException {
        return true;
    }

    @Override
    public boolean setNeighbour(String ip, String lookupName) throws RemoteException {

        ClientRemote neighbourRemote = null;
        try {
            neighbourRemote = (ClientRemote) Naming.lookup("rmi://" + ip + "/"+lookupName);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        if(neighbourRemote!=null){
            neighbourList.put(lookupName, neighbourRemote);
        }

        printMaster();
        printNeighbours();
        return true;
    }

    @Override
    public void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher, int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime, int runTimeInSeconds, String leftneighbourIP, String leftneighbourLookupName, String rightneighbourIP, String rightneighbourLookupName, boolean debugging) throws RemoteException {

        ClientRemote leftClient = null;
        ClientRemote rightClient = null;
        try {
            leftClient = (ClientRemote)Naming.lookup("rmi://"+leftneighbourIP+"/"+leftneighbourLookupName);
            rightClient = (ClientRemote)Naming.lookup("rmi://"+rightneighbourIP+"/"+rightneighbourLookupName);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        philosophers = new ArrayList<>(allPhilosopher+allHungryPhilosopher);

        new RestoreClient(allSeats, allPhilosopher, allHungryPhilosopher, eatTime, meditationTime, sleepTime, runTimeInSeconds, leftneighbourIP, leftneighbourLookupName, rightneighbourIP, rightneighbourLookupName, leftClient, rightClient, debugging);
        tablePart = new TablePart(seats);

        for(int i =0; i < RestoreClient.getAllPhilosopher(); i++){
            boolean b = (i+1)>=philosopherOffset&&(i+1)<philosopherOffset+philosopher;

            Philosopher p = new Philosopher((i+1),false, b);
            philosophers.add(p);
            p.start();

        }

        for(int i =0; i < RestoreClient.getAllHungryPhilosopher(); i++){
            boolean b = (i+1)>=hungryPhilosopherOffset&&(i+1)<hungryPhilosopherOffset+hungryPhilosopher;

            Philosopher p = new Philosopher((i+1+RestoreClient.getAllPhilosopher()),true, b);
            philosophers.add(p);
            p.start();
        }


    }

    public SeatProposal searchSeat(String startingClientName) throws RemoteException {
        SeatProposal currentBestSeatProposal = null;
        if (! startingClientName.equals(RestoreClient.getLeftneighbourLookupName())) {
            currentBestSeatProposal = RestoreClient.getLeftClient().searchSeat(startingClientName);
        }
        SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();

        if(currentBestSeatProposal.compareTo(ownSeatProposal) > 0) {
            return currentBestSeatProposal;
        }
        return ownSeatProposal;
    }

    @Override
    public void updatePhilosopher(int philosopherId, int newEatCount) throws RemoteException {
        philosophers.get(philosopherId).setMealsEaten(newEatCount);
    }

    @Override
    public boolean takeForkIfAvailable() throws RemoteException {
        return tablePart.getSeat(tablePart.getSeats().size()-1).getRightFork().takeForkIfAvailable();
    }

    @Override
    public void notifyReleaseFirstFork() throws RemoteException {
        leftForkAvailable = true;
        leftForkMonitor.notifyAll();
    }

    @Override
    public void notifyReleaseLastFork() throws RemoteException {
        Fork fork = tablePart.getSeats().get(tablePart.getSeats().size() - 1).getRightFork();
        fork.setAvailable(true);
        fork.getMonitor().notifyAll();
    }

    @Override
    public void awakePhilosopherAddToQueue(int philosopherId, int seatNumber) throws RemoteException {
        Philosopher philosopher = philosophers.get(philosopherId);
        philosopher.setSeat(tablePart.getSeat(seatNumber));
        philosopher.setActive(true);
        philosopher.setStatus(Status.EATING);
        philosopher.getMonitor().notifyAll();
    }

    public void setMaster(MasterRemote master, String masterName){
        this.master = master;
        this.masterName = masterName;
        printMaster();
        printNeighbours();

    }
    private void printNeighbours(){
        System.out.println("");
        System.out.println("Nachbar Clients:");
        System.out.println("");
        int nachbarCounter = 1;
        for (Map.Entry<String, ClientRemote> e:neighbourList.entrySet()){
            System.out.println("-- "+nachbarCounter+" "+e.getKey());
        }
    }

    private void printMaster(){
        System.out.println("");
        System.out.println("################");
        System.out.println("Master: "+masterName);
    }

    public static void updatePhilosopherForAllClientsCall(int PhilosopherId, int newEatCount){
        for(Map.Entry<String, ClientRemote> entry : neighbourList.entrySet()) {
            try {
                entry.getValue().updatePhilosopher(PhilosopherId, newEatCount);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean takeForkIfAvailableCall() {
        try {
            return RestoreClient.getLeftClient().takeForkIfAvailable();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void notifyReleaseLeftForkCall() {
        leftForkAvailable = true;
        try {
            RestoreClient.getLeftClient().notifyReleaseLastFork();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void notifyReleaseRightForkCall() {
        try {
            RestoreClient.getRightClient().notifyReleaseFirstFork();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static Object getLeftForkMonitor() {
        return leftForkMonitor;
    }

    public static void awakePhilosopherAddToQueueCall(int philosopherId, int seatNumber, String name) {
        try {
            neighbourList.get(name).awakePhilosopherAddToQueue(philosopherId, seatNumber);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

