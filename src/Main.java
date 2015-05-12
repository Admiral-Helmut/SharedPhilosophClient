import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Created by admiralhelmut on 01.05.15.
 */
public class Main {


    public static String masterIP = "127.0.0.1";
    public static String ownIP = "127.0.0.1";


    public static void main(String[] args){


        System.out.println("Start");
        if(args.length>1){
            ownIP = args[0];
            masterIP = args[1];

        }


        try{

            LocateRegistry.createRegistry(1099);
            System.out.println("Start der Registry erfolgreich!");

        }catch(Exception e){
            System.out.println("Start der Registry fehlgeschlagen!");
        }



        ClientServiceImpl clientService = null;
        try {
            clientService = new ClientServiceImpl();
            Naming.rebind("//"+ownIP+"/ClientRemote", clientService);
            System.out.println("# Client Remote Service gestartet");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        //Verbindung zu den Servicen des Masters
        //Registrierung beim Master

        boolean connectionToServer = false;
        MasterRemote masterRemote = null;
        try {
            masterRemote = (MasterRemote)Naming.lookup("rmi://"+masterIP+"/MasterRemote");
            connectionToServer = masterRemote.register(ownIP);

        } catch (NotBoundException e) {
            e.printStackTrace();
            connectionToServer = false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            connectionToServer = false;
        } catch (RemoteException e) {
            e.printStackTrace();
            connectionToServer = false;
        }

        if(connectionToServer){
            System.out.println("# Verbindung zum Server war erfolgreich!");
            clientService.setMaster(masterRemote, masterIP);
        }else{
            System.out.println("# Verbindung zum Server FEHLGESCHLAGEN!");
        }

    }
}
