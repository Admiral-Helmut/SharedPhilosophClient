import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by admiralhelmut on 01.05.15.
 */
public interface ClientRemote extends Remote {

    public boolean checkClient() throws RemoteException;
    public boolean setNeighbour(String ip, String lookupName) throws RemoteException;
}
