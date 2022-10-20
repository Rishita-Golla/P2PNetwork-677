import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Seller extends PeerCommunication{


    static volatile String sellerItem;
    static int currItemCount;
    int sellerID;
    private static Semaphore semaphore = new Semaphore(1);

    public Seller(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    // Sell item to a buyer after receiving response from buyer
    public static boolean sellItem(String requestedItem) {
        System.out.println("Selling item "+requestedItem);
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
        currItemCount = Constants.MAX_ITEM_COUNT;
    }

    public void processMessageForward(Message m) throws MalformedURLException {
        checkOrBroadcastMessage(m, sellerItem, sellerID);
    }

    public void processReply(Message m) {
        replyBackwards(m);
    }
}
