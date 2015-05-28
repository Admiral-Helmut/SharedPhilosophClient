import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by admiralhelmut on 01.05.15.
 */
public interface ClientRemote extends Remote {

    public boolean checkClient() throws RemoteException;
    public boolean setNeighbour(String ip, String lookupName) throws RemoteException;
    public void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher,
                           int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime,
                           int runTimeInSeconds, String leftneighbourIP,
                           String leftneighbourLookupName , String rightneighbourIP,
                           String rightneighbourLookupName, boolean debugging) throws RemoteException;

}
