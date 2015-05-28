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
            neighbourList.put(lookupName, neighbourRemote);
        }

        printMaster();
        printNeighbours();
        return true;
    }

    @Override
    public void initClient(int seats, int allSeats, int philosopher, int allPhilosopher, int hungryPhilosopher, int allHungryPhilosopher, int philosopherOffset, int hungryPhilosopherOffset, int eatTime, int meditationTime, int sleepTime, int runTimeInSeconds, String leftneighbourIP, String leftneighbourLookupName, String rightneighbourIP, String rightneighbourLookupName, boolean debugging) throws RemoteException {

        ClientRemote leftClient = null;
        ClientRemote rightClient = null;
        try {
            leftClient = (ClientRemote)Naming.lookup("rmi://"+leftneighbourIP+"/"+leftneighbourLookupName);
            rightClient = (ClientRemote)Naming.lookup("rmi://"+rightneighbourIP+"/"+rightneighbourLookupName);
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        new RestoreClient(allSeats, allPhilosopher, allHungryPhilosopher, eatTime, meditationTime, sleepTime, runTimeInSeconds, leftneighbourIP, leftneighbourLookupName, rightneighbourIP, rightneighbourLookupName, leftClient, rightClient, debugging);
        tablePart = new TablePart(seats);

        for(int i =0; i < RestoreClient.getAllPhilosopher(); i++){
            boolean b = (i+1)>=philosopherOffset&&(i+1)<philosopherOffset+philosopher;

            Philosopher p = new Philosopher((i+1),false, b);
            p.start();

        }

        for(int i =0; i < RestoreClient.getAllHungryPhilosopher(); i++){
            boolean b = (i+1)>=hungryPhilosopherOffset&&(i+1)<hungryPhilosopherOffset+hungryPhilosopher;

            Philosopher p = new Philosopher((i+1+RestoreClient.getAllPhilosopher()),true, b);
            p.start();
        }


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

