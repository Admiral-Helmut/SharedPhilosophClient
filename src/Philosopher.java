/**
 * Created by Admiral Helmut on 20.05.2015.
 */
public class Philosopher extends Thread {

    private boolean hungry;
    private int id;
    private boolean active;
    private Object monitor = new Object();
    private Status status;

    public Philosopher(int id, boolean hungry, boolean active){

        this.id = id;
        this.hungry = hungry;
        this.active = active;
        this.status = Status.MEDITATING;
    }

    public void run(){

        while(System.currentTimeMillis()<=RestoreClient.getEndTime()){
            if(!active) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            while(active){

                switch(status){

                    case MEDITATING:
                        break;
                    case EATING:
                        break;
                    case SLEEPING:
                        break;

                }
            }

        }

        System.out.println("Programm beendet!");


    }

}
