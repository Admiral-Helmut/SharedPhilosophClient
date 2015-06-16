import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;

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
    private final static boolean debug = true;
    TablePart tablePart = null;
    private static Object monitor = new Object();
    private static long lastUpdate = 0;
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
        for(Philosopher phil : philosophers) {
            phil.start();
        }
        overseer.start();
    }

    public void searchSeat(String lookupName, int callingPhilosopherID, int allPhilosophersAmount) throws RemoteException {
        SeatProposal seatProposal = TablePart.getTablePart().getBestProposalForCurrentTable();
        if(neighbourList.get(lookupName) != null){
            neighbourList.get(lookupName).notifySetProposal(seatProposal, callingPhilosopherID, allPhilosophersAmount);
        }
    }

    @Override
    public void updatePhilosophers(HashMap<Integer, Integer> philsophersUpdate, int allPhilosopherAmount) throws RemoteException {
        if (getLastUpdate() + 200 < System.currentTimeMillis()) {
            for(Map.Entry<Integer, Integer> philosopher : philsophersUpdate.entrySet()){
                if(allPhilosopherAmount == philosophers.size())
                    philosophers.get(philosopher.getKey() - 1).setMealsEaten(philosopher.getValue());
            }
        }
    }

    @Override
    public void takeForkIfAvailable() throws RemoteException {
        tablePart.getSeat(tablePart.getSeats().size()-1).getRightFork().takeForkIfAvailable(false);
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

    @Override
    public void notifyForkAvailable(boolean a) throws RemoteException {
        Philosopher philosopher = tablePart.getSeats().get(0).getPhilosopher();
        if(philosopher != null){
            philosopher.setGotForkRemote(true);
            synchronized (philosopher.getMonitor()){
                philosopher.getMonitor().notify();
            }
        }
    }

    @Override
    public void notifySetProposal(SeatProposal seatProposal, int philosopherID, int allPhilosophersAmount) throws RemoteException {
        if (getLastUpdate() + 500 < System.currentTimeMillis() && allPhilosophersAmount == philosophers.size()) {
            Philosopher philosopher = philosophers.get(philosopherID - 1);
            philosopher.setPushedSeatProposal(seatProposal);
            synchronized (philosopher.getSeatProposalMonitor()){
                philosopher.getSeatProposalMonitor().notify();
            };
        }
    }

    @Override
    public void addSeats(int diff, int newAmount) throws RemoteException {
        TablePart.getTablePart().restoreSeats(diff);
        RestoreClient.setAllSeats(newAmount);
    }

    @Override
    public void removeSeats(int diff, int newAmount) throws RemoteException {
        TablePart.getTablePart().removeSeats(diff);
        RestoreClient.setAllSeats(newAmount);
    }

    @Override
    public void addPhilosopher(boolean hungry, boolean active) throws RemoteException {
        synchronized (monitor){
            lastUpdate = System.currentTimeMillis();
        }
        Philosopher philosopher = new Philosopher(philosophers.size()+1, hungry, active);
        philosophers.add(philosopher);
        philosopher.start();
    }

    @Override
    public void removePhilosopher(int ident) throws RemoteException {
        synchronized (monitor) {
            lastUpdate = System.currentTimeMillis();
            Philosopher philosopher = philosophers.get(ident);
            philosopher.setExit(true);
            philosopher.setActive(false);
            for (int i = ident + 1; i < philosophers.size(); i++) {
                philosopher.setIdent(i);
            }
            philosophers.remove(ident);
            RestoreClient.setAllPhilosopher(RestoreClient.getAllPhilosopher() - 1);
            RestoreClient.setAllHungryPhilosopher(RestoreClient.getAllHungryPhilosopher() - 1);
        }
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
        int oldSize = philosophers.size();
        for(Philosopher philosopher : philosophers) {
            if(philosopher.isActive()) {
                philosophersUpdate.put(philosopher.getIdent(), philosopher.getMealsEaten());
            }
        }
        try {
            synchronized (getMonitor()) {
                if (getLastUpdate() + 200 < System.currentTimeMillis() && oldSize == philosophers.size()) {
                    rightNeighbor.updatePhilosophers(philosophersUpdate, philosophers.size());
                }
            }
        } catch (RemoteException e) {
            //System.out.println(RestoreClient.getRightneighbourLookupName() + ":" + Main.lookupName);
            if(debug)
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RestoreClient.getLeftClient().takeForkIfAvailable();
                } catch (RemoteException e) {
                    //e.printStackTrace();
                }
            }
        }).start();

        return true;
    }

    public static boolean awakePhilosopherAddToQueueCall(int philosopherId, int seatNumber, String name, int mealsEaten) {
        while (getLastUpdate() + 200 > System.currentTimeMillis()){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


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
            if(debug)
                System.out.println("awakePhilosopherAddToQueueCall");
            RestoreClient.startRestoring();
        }
        return false;
    }

    public static void notifyReleaseLeftForkCall() {
        if(restoringActive){
            return;
        }
        try {
            RestoreClient.getLeftClient().releaseLastFork();
        } catch (RemoteException e) {
            if(debug)
                System.out.println("notifyReleaseLeftForkCall");
            RestoreClient.startRestoring();

        }

    }

    public static void updateAverageCall() {
        synchronized (getMonitor()) {
            if (getLastUpdate() + 200 > System.currentTimeMillis()) {
                return;
            }
        }
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
        try {
            List<Integer> averages = new ArrayList<Integer>();
            RestoreClient.getLeftClient().updateAverage(Main.lookupName, averages);
        } catch (RemoteException e) {
            //System.out.println(RestoreClient.getLeftneighbourLookupName() + ":" + Main.lookupName);
            if(debug)
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

    public static SeatProposal getBestExternalProposal(Philosopher callingPhilosopher) {
        SeatProposal bestSeatProposal = null;
        if(getLastUpdate() + 200 > System.currentTimeMillis()) {
            return null;
        }
        try{
            for(Map.Entry<String, ClientRemote> entry : neighbourList.entrySet()){
                if(!entry.getKey().equals(Main.lookupName)){
                    SeatProposal currentSeatProposal = null;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                entry.getValue().searchSeat(Main.lookupName, callingPhilosopher.getIdent(), philosophers.size());
                            } catch (RemoteException e) {
                                //e.printStackTrace();
                            }
                        }
                    }).start();
                    long startTime = System.currentTimeMillis();
                    try {
                        synchronized (callingPhilosopher.getSeatProposalMonitor()){
                            callingPhilosopher.getSeatProposalMonitor().wait(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(callingPhilosopher.isExit()){
                        return null;
                    }

                    synchronized (getMonitor()) {
                        if (getLastUpdate() + 1200 > System.currentTimeMillis()) {
                            return null;
                        }
                    }

                    if(System.currentTimeMillis() - startTime > 900){
                        if(debug)
                            System.out.println("Restoring started due to timeout when waiting for seat");
                        RestoreClient.startRestoring();
                    }
                    currentSeatProposal = callingPhilosopher.getPushedSeatProposal();
                    if(currentSeatProposal != null && currentSeatProposal.getWaitingPhilosophersCount() == 0){
                        return currentSeatProposal;
                    }
                    if(currentSeatProposal != null){
                        if(bestSeatProposal == null || currentSeatProposal.isBetterThen(bestSeatProposal)){
                            bestSeatProposal = currentSeatProposal;
                        }
                    }
                }
            }
        }catch (ConcurrentModificationException e){

        }
        return bestSeatProposal;
    }

    public static long getLastUpdate() {
        return lastUpdate;
    }

    public static Object getMonitor() {
        return monitor;
    }
}

