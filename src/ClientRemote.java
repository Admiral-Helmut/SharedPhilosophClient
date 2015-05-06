import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Admiral Helmut on 01.05.2015.
 */
public interface ClientRemote extends Remote {

    public boolean checkClient() throws RemoteException;
    public boolean setNeighbour(String ip) throws RemoteException;
}
