import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by Admiral Helmut on 01.05.2015.
 */
public class Main {

    public static final String masterIP = "localhost";



    public static void main(String[] args){


        //Bereitstellung von ClientServices
        try {
            ClientServiceImpl clientService = new ClientServiceImpl();
            Naming.rebind("ClientRemote", clientService);

            System.out.println("# Client Remote Service gestartet");

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        //Verbindung zu den Servicen des Masters
        //Registrierung beim Master
        try {

            MasterRemote masterRemote = (MasterRemote)Naming.lookup("rmi://"+masterIP+"/MasterRemote");
            masterRemote.register("localhost");

        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
