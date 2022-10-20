import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Seller extends PeerCommunication{

    public static String sellerItem;
    public static int currItemCount = Constants.MAX_ITEM_COUNT;
    public static int sellerID;
    private static Semaphore semaphore = new Semaphore(1);

    public Seller(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    public static void setSellerItem(String itemName) {
        sellerItem = itemName;
    }
    // Sell item to a buyer after receiving response from buyer
    public static boolean sellItem(String requestedItem) {
        System.out.println("Selling item "+requestedItem);
        if(!requestedItem.equals(sellerItem)) {
            System.out.println("Items dont match in sellItem, returning ");
            return false;
        }

        // synchronization
        boolean successfulSell = false;
        try {
            semaphore.acquire();
            if (currItemCount >= 1) {
                currItemCount--;
                successfulSell = true;
            }
            System.out.println("Sold requested item - " + requestedItem + "!!!!:))))");
            if (currItemCount == 0)
                stockItems();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();
        return successfulSell;
    }

    public static void stockItems() {

        int index = Constants.POSSIBLE_ITEMS.indexOf(sellerItem);
        sellerItem = Constants.POSSIBLE_ITEMS.get((index+1)%3);
//        String possibleNextItem = sellerItem;
//        while(sellerItem.equals(possibleNextItem)) {
//            possibleNextItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(size));
//        }
        System.out.println("Restocked items for seller ID:"+sellerID+"with item:"+sellerItem);
        currItemCount = Constants.MAX_ITEM_COUNT;
    }

//    public void processMessageForward(Message m) throws MalformedURLException {
//        checkOrBroadcastMessage(m, sellerItem, sellerID, "seller");
//    }

    public static void processReply(Message m) {
        replyBackwards(m);
    }
}
