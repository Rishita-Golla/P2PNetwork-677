import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Seller extends PeerCommunication{

    static String sellerItem;
    static int currItemCount;
    static int maxItemCount;
    int sellerID;
    List<Integer> neighborPeerID;

   // public static final List<String> possibleItems = Arrays.asList("FISH","SALT","BOAR");

    public static boolean sellItem(String requestedItem) {

        if(requestedItem.equals(sellerItem))
            return false; //check, seller item might change

        //add synchronization
        if(currItemCount >= 1) {
            currItemCount--;
        }

        if(currItemCount == 0)
            stockItems();

        return true; //check if thread acquired lock and return
    }

    public static void stockItems() {
        Random rand = new Random();
        sellerItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(Constants.POSSIBLE_ITEMS.size()));
        currItemCount = maxItemCount;
    }

    public void processMessageForward(Message m) {
        checkOrBroadcastMessage(m, sellerItem, sellerID, neighborPeerID);
    }
}
