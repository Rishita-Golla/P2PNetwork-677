import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Seller extends PeerCommunication{

    static String sellerItem;
    static int currItemCount;
    static int maxItemCount; //change to constant
    int sellerID;
    private static Semaphore semaphore = new Semaphore(1);

    public Seller(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    // Sell item to a buyer after receiving response from buyer
    public static boolean sellItem(String requestedItem) {

        if(!requestedItem.equals(sellerItem))
            return false;

        // synchronization
        boolean successfulSell = false;
        try {
            semaphore.acquire();
            if (currItemCount >= 1) {
                currItemCount--;
                successfulSell = true;
            }
            if (currItemCount == 0)
                stockItems();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return successfulSell;
    }

    public static void stockItems() {
        Random rand = new Random();
        sellerItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(Constants.POSSIBLE_ITEMS.size()));
        currItemCount = maxItemCount;
    }

    public void processMessageForward(Message m) {
        checkOrBroadcastMessage(m, sellerItem, sellerID, neighborPeerIDs.get(sellerID));
    }

    public void processReply(Message m) {
        replyBackwards(m);
    }
}
