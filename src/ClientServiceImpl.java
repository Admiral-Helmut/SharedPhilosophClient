import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by admiralhelmut on 01.05.15.
 */
public class ClientServiceImpl extends UnicastRemoteObject implements ClientRemote {


    private HashMap<String, ClientRemote> neighbourList;
    MasterRemote master;
    String masterName;

    TablePart tablePart = null;


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

        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if(neighbourRemote!=null){
            neighbourList.put(ip, neighbourRemote);
        }

        printMaster();
        printNeighbours();
        return true;
    }

    @Override
    public void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher, int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime, int runTimeInSeconds, String leftneighbourIP, String leftneighbourLookupName, String rightneighbourIP, String rightneighbourLookupName) throws RemoteException {

        tablePart = new TablePart();

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


}

