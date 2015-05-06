import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by Admiral Helmut on 01.05.2015.
 */
public class ClientServiceImpl extends UnicastRemoteObject implements ClientRemote {


    public ClientServiceImpl() throws RemoteException {
    }

    @Override
    public boolean checkClient() throws RemoteException {

        return true;
    }
}
