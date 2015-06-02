/**
 * VSS
 * Created by Admiral Helmut on 20.05.2015.
 */
public class RestoreClient {

    private static int allSeats;
    private static int allPhilosopher;
    private static int allHungryPhilosopher;
    private static int eatTime;
    private static int meditationTime;
    private static int sleepTime;
    private static long endTime;

    private static String leftneighbourIP;
    private static String leftneighbourLookupName;
    private static String rightneighbourIP;
    private static String rightneighbourLookupName;

    private static ClientRemote leftClient;
    private static ClientRemote rightClient;

    private static boolean debugging;




    public RestoreClient(int allSeats, int allPhilosopher, int allHungryPhilosopher, int eatTime, int meditationTime, int sleepTime, int runTimeInSeconds, String leftneighbourIP, String leftneighbourLookupName, String rightneighbourIP, String rightneighbourLookupName, ClientRemote leftClient, ClientRemote rightClient, boolean debugging){
        RestoreClient.allSeats = allSeats;
        RestoreClient.allPhilosopher = allPhilosopher;
        RestoreClient.allHungryPhilosopher = allHungryPhilosopher;
        RestoreClient.eatTime = eatTime;
        RestoreClient.meditationTime = meditationTime;
        RestoreClient.sleepTime = sleepTime;
        RestoreClient.endTime = 1000 * runTimeInSeconds+System.currentTimeMillis();

        RestoreClient.leftneighbourIP = leftneighbourIP;
        RestoreClient.leftneighbourLookupName = leftneighbourLookupName;
        RestoreClient.rightneighbourIP = rightneighbourIP;
        RestoreClient.rightneighbourLookupName = rightneighbourLookupName;

        RestoreClient.leftClient = leftClient;
        RestoreClient.rightClient = rightClient;
        RestoreClient.debugging = debugging;

    }

    public static int getAllSeats() {
        return allSeats;
    }

    public static int getAllPhilosopher() {
        return allPhilosopher;
    }

    public static int getAllHungryPhilosopher() {
        return allHungryPhilosopher;
    }

    public static int getEatTime() {
        return eatTime;
    }

    public static int getMeditationTime() {
        return meditationTime;
    }

    public static int getSleepTime() {
        return sleepTime;
    }

    public static long getEndTime() {
        return endTime;
    }

    public static String getLeftneighbourIP() {
        return leftneighbourIP;
    }

    public static String getLeftneighbourLookupName() {
        return leftneighbourLookupName;
    }

    public static String getRightneighbourIP() {
        return rightneighbourIP;
    }

    public static String getRightneighbourLookupName() {
        return rightneighbourLookupName;
    }

    public static boolean isDebugging() {
        return debugging;
    }

    public static ClientRemote getLeftClient() {
        return leftClient;
    }

    public static ClientRemote getRightClient() {
        return rightClient;
    }
}
