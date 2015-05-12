import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by admiralhelmut on 01.05.15.
 */
public interface MasterRemote extends Remote {

    public boolean register(String ip) throws RemoteException;
}
