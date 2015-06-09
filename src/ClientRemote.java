import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VSS
 * Created by admiralhelmut on 01.05.15.
 */
public interface ClientRemote extends Remote {

    boolean checkClient() throws RemoteException;
    boolean setNeighbour(String ip, String lookupName) throws RemoteException;
    void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher,
                           int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime,
                           int runTimeInSeconds, String leftneighbourIP,
                           String leftneighbourLookupName , String rightneighbourIP,
                           String rightneighbourLookupName, boolean debugging, long startTime) throws RemoteException;

    SeatProposal searchSeat(String startingClientName, int ident) throws RemoteException;

    void updatePhilosophers(HashMap<Integer, Integer> philsophersUpdate) throws RemoteException;

    boolean takeForkIfAvailable() throws RemoteException;

    void awakePhilosopherAddToQueue(int philosopherId, int seatNumber, int mealsEaten) throws RemoteException;

    void lastForkWait() throws RemoteException;

    void releaseLastFork() throws RemoteException;

    void updateAverage(String lookupName, List<Integer> averages) throws RemoteException;

    void restoreSetRightNeigbour(String lookupNameLostClient, String newLookupName, String newIp) throws RemoteException;

    String[] restoreGetLookupNameAndIp(String leftneighbourLookupName) throws RemoteException;

    void restoreInformAll(String lookupNameLostClient) throws RemoteException;

    void restoreFinishedInformAll(String lookupName) throws RemoteException;

    Map<String,Integer> getSeatsForRestoring(String leftneighbourLookupName) throws RemoteException;

    void restoreAddSeat() throws RemoteException;

    boolean[] restoreGetPhilosophersCount(String lookupName) throws RemoteException;
}
