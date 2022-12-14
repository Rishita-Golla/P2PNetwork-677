import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Buyer extends PeerCommunication{

    public String buyerItem;
    protected int buyerID;
    private static Semaphore semaphore = new Semaphore(1);
    List<String> timedOutReplies = new ArrayList<>();
    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    Date date = new Date(System.currentTimeMillis());

    public Buyer(int buyerID, String buyerItem) {
        super();
        this.buyerID = buyerID;
        this.buyerItem = buyerItem;
    }

    public Buyer(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    // buyer starts a transaction with seller and attempts to buy requested item
    public boolean buyItemDirectlyFromSeller(int sellerId) {
        System.out.println(formatter.format(date)+" Trying to buy item directly from Seller ID: " + sellerId);
        try {
            URL url = new URL(peerIdURLMap.get(sellerId));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            return remoteInterface.sellItem(this.buyerItem, "buyer");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // pick a new item in cyclic order
    public void pickNewBuyerItem() {
        int size = Constants.POSSIBLE_ITEMS.size();
        int index = Constants.POSSIBLE_ITEMS.indexOf(buyerItem);
        buyerItem = Constants.POSSIBLE_ITEMS.get((index+1)%size);
        System.out.println(formatter.format(date)+" Picked up "+ buyerItem+ " as new buyer item for ID: "+buyerID);
    }

    public void processMessageForward(Message m) throws MalformedURLException {
        checkOrBroadcastMessage(m, "", buyerID, "buyer");
    }

    // process reply at buyers end. If message reaches initial buyer then
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
        checkOrBroadcastMessage(m, "", buyerID, "buyer");

        for(int i = 0; i < 5; i++) {
            Thread.sleep(Constants.MAX_TIMEOUT/5);
            if(timedOutReplies.contains(lookupId))
                break;
        }

        if(!timedOutReplies.contains(lookupId)) {
            discardReply(lookupId);
        }
    }

    public void discardReply(String lookupId) {
        try {
            semaphore.acquire();
            System.out.println(formatter.format(date)+ " Timed out request for "+ buyerID + " and lookUp "+ lookupId);
            timedOutReplies.add(lookupId);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        semaphore.release();
    }

}
