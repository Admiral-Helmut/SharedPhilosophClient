import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admiral Helmut on 18.05.2015.
 */
public class TablePart {

    private List<Seat> seats;
    private static TablePart tablePart;

    public TablePart(){

        tablePart = this;
        seats = new ArrayList<Seat>();
    }

    public static TablePart getTablePart(){
        return tablePart;
    }

}
