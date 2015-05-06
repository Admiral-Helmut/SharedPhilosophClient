import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Admiral Helmut on 01.05.2015.
 */
public interface MasterRemote extends Remote {


    public boolean register(String ip) throws RemoteException;



}



