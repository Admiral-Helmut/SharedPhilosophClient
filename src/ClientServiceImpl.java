import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * VSS
 * Created by admiralhelmut on 01.05.15.
 */
public class ClientServiceImpl extends UnicastRemoteObject implements ClientRemote {


    private static HashMap<String, ClientRemote> neighbourList;
    MasterRemote master;
    String masterName;
    private ConcurrentHashMap<Integer,Philosopher> philosophers;

    TablePart tablePart = null;
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
    public void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher, int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime, int runTimeInSeconds, String leftneighbourIP, String leftneighbourLookupName, String rightneighbourIP, String rightneighbourLookupName, boolean debugging, long startTime) throws RemoteException {

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
        philosophers = new ConcurrentHashMap<>();

        new RestoreClient(allSeats, allPhilosopher, allHungryPhilosopher, eatTime, meditationTime, sleepTime, runTimeInSeconds, leftneighbourIP, leftneighbourLookupName, rightneighbourIP, rightneighbourLookupName, leftClient, rightClient, debugging);
        tablePart = new TablePart(seats);
        System.out.println((RestoreClient.getLeftClient() == null)+":" + (RestoreClient.getRightClient() == null));
        for(int i =0; i < RestoreClient.getAllPhilosopher(); i++){
            boolean b = (i+1)>=philosopherOffset&&(i+1)<philosopherOffset+philosopher;

            Philosopher p = new Philosopher((i+1),false, b, leftneighbourIP, leftneighbourLookupName);
            philosophers.put(i, p);
        }

        for(int i =0; i < RestoreClient.getAllHungryPhilosopher(); i++){
            boolean b = (i+1)>=hungryPhilosopherOffset&&(i+1)<hungryPhilosopherOffset+hungryPhilosopher;

            Philosopher p = new Philosopher((i+1+RestoreClient.getAllPhilosopher()),true, b, leftneighbourIP, leftneighbourLookupName);
            philosophers.put(i + RestoreClient.getAllPhilosopher(), p);
        }
        Overseer overseer = new Overseer(philosophers);

        while (System.currentTimeMillis() < startTime ) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for(Philosopher phil : philosophers.values()) {
            phil.start();
        }
        overseer.start();
    }

    public SeatProposal searchSeat(String startingClientName) throws RemoteException {
        SeatProposal currentBestSeatProposal = null;
        if (! startingClientName.equals(RestoreClient.getLeftneighbourLookupName())) {
            currentBestSeatProposal = RestoreClient.getLeftClient().searchSeat(startingClientName);
        }
        SeatProposal ownSeatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();

        if(currentBestSeatProposal != null && currentBestSeatProposal.isBetterThen(ownSeatProposal)) {
            return currentBestSeatProposal;
        }
        return ownSeatProposal;
    }

    @Override
    public void updatePhilosophers(HashMap<Integer, Integer> philsophersUpdate) throws RemoteException {
        for(Map.Entry<Integer, Integer> philosopher : philsophersUpdate.entrySet()){
            philosophers.get(philosopher.getKey()-1).setMealsEaten(philosopher.getValue());
        }
    }

    @Override
    public boolean takeForkIfAvailable() throws RemoteException {
        return tablePart.getSeat(tablePart.getSeats().size()-1).getRightFork().takeForkIfAvailable();
    }

    @Override
    public void awakePhilosopherAddToQueue(int philosopherId, int seatNumber, int mealsEaten) throws RemoteException {
        //System.out.println(philosophers.size() + ":::" + philosopherId);
        if(RestoreClient.isDebugging()) {
            System.out.println("Philosopher " + philosopherId + " activated with eat count " + mealsEaten + ".");
        }
        Philosopher philosopher = philosophers.get(philosopherId-1);
        Seat seat = tablePart.getSeat(seatNumber);
        philosopher.setMealsEaten(mealsEaten);
        philosopher.setActive(true);
        philosopher.setStatus(Status.EATING);
        philosopher.setNewSeat(seat);

        synchronized (philosopher.getMonitor()){
            philosopher.getMonitor().notifyAll();
        }
    }

    @Override
    public void lastForkWait() throws RemoteException {
        Fork fork = tablePart.getSeat(tablePart.getSeats().size()-1).getRightFork();
        synchronized (fork.getMonitor()) {
            if(fork.isAvailable()) {
                return;
            }
            try {
                fork.getMonitor().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void releaseLastFork() throws RemoteException {
        Fork fork = tablePart.getSeat(tablePart.getSeats().size()-1).getRightFork();
        synchronized (fork.getMonitor()) {
            fork.setAvailable(true);
            fork.getMonitor().notifyAll();
        }
    }

    @Override
    public List<Integer> updateAverage(String lookupName) throws RemoteException {
        List<Integer> averages = new ArrayList<>();

        long sum = 0;
        int count = 0;
        for (Philosopher philosopher : philosophers.values()) {
            if(philosopher.isActive()) {
                sum += philosopher.getMealsEaten();
                count ++;
            }
        }
        if(count > 0) {
            averages.add((int)(sum / count));
        }

        if(!lookupName.equals(RestoreClient.getRightneighbourLookupName())){
            averages.addAll(RestoreClient.getRightClient().updateAverage(lookupName));
        }
        return averages;
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

    public static void updatePhilosophersForNeighborCall(ConcurrentHashMap<Integer,Philosopher> philosophers){
        ClientRemote rightNeighbor = RestoreClient.getRightClient();
        HashMap<Integer, Integer> philosophersUpdate = new HashMap<>();
        for(Philosopher philosopher : philosophers.values()) {
            if(philosopher.isActive()) {
                philosophersUpdate.put(philosopher.getIdent(), philosopher.getMealsEaten());
            }
        }
        try {
            rightNeighbor.updatePhilosophers(philosophersUpdate);
        } catch (RemoteException e) {
            e.printStackTrace();
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

    public static void awakePhilosopherAddToQueueCall(int philosopherId, int seatNumber, String name, int mealsEaten) {
        try {
            neighbourList.get(name).awakePhilosopherAddToQueue(philosopherId, seatNumber, mealsEaten);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void leftForkWaitCall() {
        try {
            RestoreClient.getLeftClient().lastForkWait();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void notifyReleaseLeftForkCall() {
        try {
            RestoreClient.getLeftClient().releaseLastFork();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public static List<Integer> updateAverageCall(String lookupName) {
        try {
            return RestoreClient.getRightClient().updateAverage(lookupName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }
}

