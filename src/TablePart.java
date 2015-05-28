import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admiral Helmut on 18.05.2015.
 */
public class TablePart {

    private List<Seat> seats;
    private static TablePart tablePart;

    public TablePart(int seatAmounts){

        tablePart = this;
        seats = new ArrayList<Seat>();
        Fork fork = null;
        for(int i = 0; i<seatAmounts;i++){
            Seat s = new Seat(fork);
            seats.add(s);
            fork = new Fork(s);
        }




    }

    public static TablePart getTablePart(){
        return tablePart;
    }

}
