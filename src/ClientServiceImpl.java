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
    private static List<Philosopher> philosophers;
    private static boolean restoringActive = false;
    private static Overseer overseer;

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
            ClientRemote ownClient = (ClientRemote)Naming.lookup("rmi://"+Main.ownIP+"/"+Main.lookupName);
            neighbourList.put(Main.lookupName, ownClient);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        philosophers = new ArrayList<>(allPhilosopher+allHungryPhilosopher);
        new RestoreClient(allSeats, allPhilosopher, allHungryPhilosopher, eatTime, meditationTime, sleepTime, runTimeInSeconds, leftneighbourIP, leftneighbourLookupName, rightneighbourIP, rightneighbourLookupName, leftClient, rightClient, debugging);
        tablePart = new TablePart(seats);
        System.out.println((RestoreClient.getLeftClient() == null)+":" + (RestoreClient.getRightClient() == null));
        for(int i =0; i < RestoreClient.getAllPhilosopher(); i++){
            boolean b = (i+1)>=philosopherOffset&&(i+1)<philosopherOffset+philosopher;

            Philosopher p = new Philosopher((i+1),false, b);
            philosophers.add(p);
        }

        for(int i =0; i < RestoreClient.getAllHungryPhilosopher(); i++){
            boolean b = (i+1)>=hungryPhilosopherOffset&&(i+1)<hungryPhilosopherOffset+hungryPhilosopher;

            Philosopher p = new Philosopher((i+1+RestoreClient.getAllPhilosopher()),true, b);
            philosophers.add(p);
        }
        overseer = new Overseer(philosophers);

        while (System.currentTimeMillis() < startTime ) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(philosophers.size() + "asd");
        for(Philosopher phil : philosophers) {
            phil.start();
        }
        overseer.start();
    }

    public SeatProposal searchSeat(String startingClientName, int ident) throws RemoteException {
        SeatProposal currentBestSeatProposal = null;
        if (! startingClientName.equals(RestoreClient.getLeftneighbourLookupName())) {
            Philosopher philosopher = philosophers.get(ident-1);
            currentBestSeatProposal = RestoreClient.getLeftClient().searchSeat(startingClientName, ident);
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
            philosophers.get(philosopher.getKey() - 1).setMealsEaten(philosopher.getValue());
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
    public void updateAverage(String lookupName, List<Integer> averages) throws RemoteException {

        long sum = 0;
        int count = 0;
        for (Philosopher philosopher : philosophers) {
            if(philosopher.isActive()) {
                sum += philosopher.getMealsEaten();
                count ++;
            }
        }
        if(count > 0) {
            averages.add((int)(sum / count));
        }

        if(lookupName.equals(Main.lookupName)){
            int finalSum = 0;
            int finalCount = 0;
            for(int value : averages) {
                finalSum += value;
                finalCount ++;
            }

            int average = ((int)(finalSum / finalCount));
            Overseer.setAverage(average);
        }
        else{
            RestoreClient.getLeftClient().updateAverage(lookupName, averages);
        }
    }

    public void restoreSetRightNeigbour(String lookupNameLostClient, String newLookupName, String newIp) throws RemoteException {
        if(lookupNameLostClient.equals(RestoreClient.getRightneighbourLookupName())) {
            RestoreClient.setRightneighbourLookupName(newLookupName);
            RestoreClient.setRightneighbourIP(newIp);
            try {
                ClientRemote newClient = (ClientRemote)Naming.lookup("rmi://"+newIp+"/"+newLookupName);
                RestoreClient.setRightClient(newClient);
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else{
            RestoreClient.getRightClient().restoreSetRightNeigbour(lookupNameLostClient, newLookupName, newIp);
        }
    }

    @Override
    public String[] restoreGetLookupNameAndIp(String leftneighbourLookupName) throws RemoteException {
        if(leftneighbourLookupName.equals(RestoreClient.getRightneighbourLookupName())){
            return new String[]{RestoreClient.getRightneighbourLookupName(), RestoreClient.getRightneighbourIP()};
        }
        else{
            return RestoreClient.getRightClient().restoreGetLookupNameAndIp(leftneighbourLookupName);
        }
    }

    @Override
    public void restoreInformAll(String lookupNameLostClient) throws RemoteException {
        if(!lookupNameLostClient.equals(RestoreClient.getRightneighbourLookupName())){
            RestoreClient.getRightClient().restoreInformAll(lookupNameLostClient);
            RestoreClient.copyPhilosophersAndRemove();
            restoringActive = true;
        }
    }

    @Override
    public void restoreFinishedInformAll(String lookupName) throws RemoteException {
        if(!lookupName.equals(RestoreClient.getRightneighbourLookupName())){
            RestoreClient.getRightClient().restoreFinishedInformAll(lookupName);
            restoringActive = false;
            overseer.reStartPunisher();
            overseer.reStartUpdater();
            RestoreClient.awakePhilosophers();
        }
    }

    @Override
    public Map<String, Integer> getSeatsForRestoring(String leftneighbourLookupName) throws RemoteException {
        Map<String, Integer> seats = new HashMap<String, Integer>();
        if(!leftneighbourLookupName.equals(RestoreClient.getRightneighbourLookupName())){
            seats.putAll(getSeatsForRestoring(leftneighbourLookupName));
        }
        seats.put(Main.lookupName, tablePart.getSeats().size());
        return seats;
    }

    @Override
    public void restoreAddSeat() throws RemoteException {
        tablePart.restoreSeat();
    }

    @Override
    public boolean[] restoreGetPhilosophersCount(String lookupName) throws RemoteException {
        boolean[] philosophersForRestoring;
        if(!lookupName.equals(RestoreClient.getRightneighbourLookupName())) {
            philosophersForRestoring = RestoreClient.getRightClient().restoreGetPhilosophersCount(lookupName);
        }
        else{
            philosophersForRestoring = new boolean[RestoreClient.getAllPhilosopher()+RestoreClient.getAllHungryPhilosopher()];
        }
        for(Philosopher philosopher : philosophers) {
            if(philosopher.isActive()){
                philosophersForRestoring[philosopher.getIdent()-1] =  true;
            }
        }
        return philosophersForRestoring;
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

    public static void updatePhilosophersForNeighborCall(List<Philosopher> philosophers){
        if(restoringActive || RestoreClient.getRightneighbourLookupName().equals(Main.lookupName)){
            return;
        }
        ClientRemote rightNeighbor = RestoreClient.getRightClient();
        HashMap<Integer, Integer> philosophersUpdate = new HashMap<>();
        for(Philosopher philosopher : philosophers) {
            if(philosopher.isActive()) {
                philosophersUpdate.put(philosopher.getIdent(), philosopher.getMealsEaten());
            }
        }
        try {
            rightNeighbor.updatePhilosophers(philosophersUpdate);
        } catch (RemoteException e) {
            System.out.println(RestoreClient.getRightneighbourLookupName() + ":" + Main.lookupName);
            System.out.println("updatePhilosophersForNeighborCall");
            if(!RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)) {
                RestoreClient.startRestoring();
            }
        }
    }

    public static boolean takeForkIfAvailableCall() {
        if(restoringActive){
            return false;
        }
        try {
            return RestoreClient.getLeftClient().takeForkIfAvailable();
        } catch (RemoteException e) {
            RestoreClient.startRestoring();
            System.out.println("takeForkIfAvailableCall");
        }
        return false;
    }

    public static boolean awakePhilosopherAddToQueueCall(int philosopherId, int seatNumber, String name, int mealsEaten) {
        if(restoringActive){
            return false;
        }
        try {
            if(neighbourList.containsKey(name)){
                neighbourList.get(name).awakePhilosopherAddToQueue(philosopherId, seatNumber, mealsEaten);
                return true;
            }
            else{
                return false;
            }
        } catch (RemoteException e) {
            RestoreClient.startRestoring();
            System.out.println("awakePhilosopherAddToQueueCall");
        }
        return false;
    }

    public static void leftForkWaitCall() {
        if(restoringActive || RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)){
            return;
        }
        try {
            RestoreClient.getLeftClient().lastForkWait();
        } catch (RemoteException e) {
            RestoreClient.startRestoring();
            System.out.println("leftForkWaitCall");

        }
    }

    public static void notifyReleaseLeftForkCall() {
        if(restoringActive){
            return;
        }
        try {
            RestoreClient.getLeftClient().releaseLastFork();
        } catch (RemoteException e) {
            RestoreClient.startRestoring();
            System.out.println("notifyReleaseLeftForkCall");

        }

    }

    public static void updateAverageCall() {
        if(restoringActive || RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)){
            long sum = 0;
            int count = 0;
            for (Philosopher philosopher : philosophers) {
                if(philosopher.isActive()) {
                    sum += philosopher.getMealsEaten();
                    count ++;
                }
            }

            int average = ((int)(sum / count));
            Overseer.setAverage(average);
            return;
        }
        //System.out.println("ASDASD");
        try {
            List<Integer> averages = new ArrayList<Integer>();
            RestoreClient.getLeftClient().updateAverage(Main.lookupName, averages);
        } catch (RemoteException e) {
            System.out.println(RestoreClient.getLeftneighbourLookupName() + ":" + Main.lookupName);
            System.out.println("updateAverageCall");
            if(!RestoreClient.getLeftneighbourLookupName().equals(Main.lookupName)){
                RestoreClient.startRestoring();
            }
        }
    }

    public static HashMap<String, ClientRemote> getNeighbourList() {
        return neighbourList;
    }

    public static boolean isRestoringActive() {
        return restoringActive;
    }

    public static void setRestoringActive(boolean restoringActive) {
        ClientServiceImpl.restoringActive = restoringActive;
    }

    public static void restoreAddSeatCall(String fewestSeats) {
        try {
            neighbourList.get(fewestSeats).restoreAddSeat();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static List<Philosopher> getPhilosophers() {
        return philosophers;
    }

    public static void setPhilosophers(List<Philosopher> philosophers) {
        ClientServiceImpl.philosophers = philosophers;
    }

    public static Overseer getOverseer() {
        return overseer;
    }
}

