import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Seller extends PeerCommunication{

    public static String sellerItem;
    public static int currItemCount = Constants.MAX_ITEM_COUNT;
    public static int sellerID;
    private static Semaphore semaphore = new Semaphore(1);
    static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    static Date date = new Date(System.currentTimeMillis());
    static LamportClock lamportClock = null;
    private static int income;

    public Seller(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
        income = 0;
        lamportClock = new LamportClock();
    }

    public static void setSellerItem(String itemName) {
        sellerItem = itemName;
    }
    // Sell item to a buyer after receiving response from buyer
    public static boolean sellItem(String requestedItem, String role) {
        System.out.println(formatter.format(date)+" Selling item "+requestedItem);
        if(!requestedItem.equals(sellerItem)) {
            System.out.println(formatter.format(date)+" Items don't match in sellItem, returning ");
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
            System.out.println(formatter.format(date)+"Sold requested item - " + requestedItem + "!!");
            if (currItemCount == 0)
                stockItems();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();
        return successfulSell;
    }

    public static void processReply(Message m) {
        replyBackwards(m);
    }

    /**
     * LAB 2 CODE
     */
    public static void receiveTimeStampUpdate(int timestamp) {
        lamportClock.receiveUpdate(timestamp);
    }

    public static void receiveTransactionAck(int income) {
        System.out.println("Sold item" + Seller.sellerItem);
        Seller.income += income;
        System.out.println("Seller income for sellerID"+ sellerID + "is"+ Seller.income);
    }

    public static void stockItems() {
        int index = Constants.POSSIBLE_ITEMS.indexOf(sellerItem);
        sellerItem = Constants.POSSIBLE_ITEMS.get((index+1)%3);
        System.out.println(formatter.format(date)+"Restocked items. Now selling item:"+sellerItem);
        currItemCount = Constants.MAX_ITEM_COUNT;
    }
}
