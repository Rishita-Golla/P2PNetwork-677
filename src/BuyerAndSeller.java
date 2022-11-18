import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

public class BuyerAndSeller extends PeerCommunication {
    public String buyerItem;
    public static String sellerItem;
    public static int currItemCount = Constants.MAX_ITEM_COUNT;
    protected int peerID;
    private static Semaphore semaphore = new Semaphore(1);
    List<String> timedOutReplies = new ArrayList<>();
    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    Date date = new Date(System.currentTimeMillis());
    LamportClock lamportClock = null;

    public BuyerAndSeller(int peerID, String buyerItem, String sellerItem) {
        super();
        this.peerID = peerID;
        this.buyerItem = buyerItem;
        BuyerAndSeller.sellerItem = sellerItem;
        lamportClock = new LamportClock();
    }

    public BuyerAndSeller(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

//    public static void setSellerItem(String itemName) {
//        sellerItem = itemName;
//    }

    public boolean buyItemDirectlyFromSeller(int sellerId) {
        System.out.println(formatter.format(date)+" Trying to buy item directly from Seller ID: " + sellerId);
        try {
            URL url = new URL(peerIdURLMap.get(sellerId));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            return remoteInterface.sellItem(this.buyerItem, "buyerAndSeller");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean sellItem(String requestedItem, String role) {
      //  System.out.println(formatter.format(date)+" Selling item "+requestedItem);
        if(!requestedItem.equals(sellerItem)) {
      //      System.out.println(formatter.format(date)+" Items don't match in sellItem, returning ");
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
        //    System.out.println(formatter.format(date)+"Sold requested item - " + requestedItem + "!!");
            if (currItemCount == 0)
                stockItems();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();
        return successfulSell;
    }

    // pick a new item in cyclic order
    public void pickNewBuyerItem() {
        int size = Constants.POSSIBLE_ITEMS.size();
        int index = Constants.POSSIBLE_ITEMS.indexOf(buyerItem);
        buyerItem = Constants.POSSIBLE_ITEMS.get((index+1)%size);
        System.out.println(formatter.format(date)+" Picked up "+ buyerItem+ " as new buyer item for ID: "+peerID);
    }

    public static void stockItems() {
        int index = Constants.POSSIBLE_ITEMS.indexOf(sellerItem);
        sellerItem = Constants.POSSIBLE_ITEMS.get((index+1)%3);
    //    System.out.println(formatter.format(date)+"Restocked items. Now selling item:"+sellerItem);
        currItemCount = Constants.MAX_ITEM_COUNT;
    }

    public void processReply(Message m) {
        try {
            semaphore.acquire();
            if(timedOutReplies.contains(m.getLookUpId())) {
                System.out.println(formatter.format(date)+"Ignoring timed out reply for: "+m.getLookUpId());
            }
            if (m.getPath().isEmpty()) { // reached initial buyer node
                System.out.println(formatter.format(date)+" Reached initial buyer in reply backward path");
                if (buyItemDirectlyFromSeller(m.getSellerID())) {
                    System.out.println(formatter.format(date)+" Bought item " + m.getRequestedItem() + " from Seller " + "with ID " + m.getSellerID());
                    timedOutReplies.add(m.getLookUpId());
                    System.out.println(formatter.format(date)+" Added " + m.getLookUpId() + " to already processed lookUp Ids");
                    pickNewBuyerItem();
                }
            } else { // an intermediate node
                replyBackwards(m);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();
    }

    // Starting lookUp with new lookUpId and message
    public void startLookUp() throws Exception {
        String lookupId = UUID.randomUUID().toString();
        Message m = new Message();
        m.setLookUpId(lookupId);
        m.setRequestedItem(buyerItem);
        m.setPath(new ArrayList<>());

        System.out.println(formatter.format(date)+" Started a new lookUp with lookUp Id: " + lookupId + ", requested item: "+ m.getRequestedItem());

        // As this is a common method, buyer will be passing empty string as he doesn't own any item
        checkOrBroadcastMessage(m, "", peerID, "buyer");

        for(int i = 0; i < 5; i++) {
            Thread.sleep(Constants.MAX_TIMEOUT/5);
            if(timedOutReplies.contains(lookupId))
                break;
        }

        if(!timedOutReplies.contains(lookupId)) {
            discardReply(lookupId);
        }
    }

    public void startLookUpWithTrader() throws Exception {
        Message m = new Message();
        String lookupId = UUID.randomUUID().toString();
        m.setLookUpId(lookupId);
        m.setRequestedItem(buyerItem);
        m.setTimestamp(lamportClock.getTimestamp());
        m.setBuyerID(peerID);

        System.out.println(formatter.format(date)+" Started a new lookUp with lookUp Id: " + lookupId + ", requested item: "+ m.getRequestedItem());

        if(checkStatusOfLeader().equals("OK")) {
            sendTimeStampUpdate(peerID);
            sendBuyMessageToTrader(m);
        }else if (checkStatusOfLeader().equals("DOWN")) {
            //if the request has timed out beyond MAX Value start re-election
            ElectionMessage message = new ElectionMessage();
            PeerCommunication.sendLeaderElectionMsg(message, peerID);
        }
    }

    // update peer's timestamps based on the buyer's timestamp before message is sent to the trader
    public void sendTimeStampUpdate(int buyerID) {
        CompletableFuture.runAsync(() -> {
            try {
                lamportClock.sendTimestampUpdate(buyerID);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendBuyMessageToTrader(Message m) {
        PeerCommunication.sendBuyMessage(m);
    }

    public void discardReply(String lookupId) {
        try {
            semaphore.acquire();
            System.out.println(formatter.format(date)+ " Timed out request for "+ peerID + " and lookUp "+ lookupId);
            timedOutReplies.add(lookupId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();
    }

    public void receiveTimeStampUpdate(int timestamp) {
        lamportClock.receiveUpdate(timestamp);
    }

    public String checkStatusOfLeader() {
        return PeerCommunication.checkLeaderStatus();
    }

    public void receiveTransactionAck() {
        System.out.println("Sold item" + Seller.sellerItem);

    }
}
