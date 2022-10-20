import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Buyer extends PeerCommunication{

    public String buyerItem;
    protected int buyerID;
    private static Semaphore semaphore = new Semaphore(1);
    List<String> timedOutReplies = new ArrayList<>();

    public Buyer(int buyerID, String buyerItem) {
        super();
        this.buyerID = buyerID;
        this.buyerItem = buyerItem;
    }

    public Buyer(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    public boolean buyItemDirectlyFromSeller(int sellerId) {
        System.out.println("Trying to buy item directly from Seller ID: " + sellerId);
        try {
            URL url = new URL(peerIdURLMap.get(sellerId));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            return remoteInterface.sellItem(this.buyerItem);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void pickNewBuyerItem() {
        int size = Constants.POSSIBLE_ITEMS.size();
        int index = Constants.POSSIBLE_ITEMS.indexOf(buyerItem);
        buyerItem = Constants.POSSIBLE_ITEMS.get((index+1)%size);
        System.out.println("Picked up "+ buyerItem+ " as new buyer item for ID: "+buyerID);
    }

    public void processMessageForward(Message m) throws MalformedURLException {
        checkOrBroadcastMessage(m, "", buyerID, "buyer");
    }

    public void processReply(Message m) {
        try {
            semaphore.acquire();
            if(timedOutReplies.contains(m.getLookUpId())) {
                System.out.println("Ignoring timed out reply for: "+m.getLookUpId());
            }
            if (m.getPath().isEmpty()) { // reached initial buyer node
                System.out.println("Reached initial buyer in reply backward path");
                if (buyItemDirectlyFromSeller(m.getSellerID())) {
                    //add to processed LookUps
                    System.out.println("Bought item " + m.getRequestedItem() + " from Seller " + "with ID " + m.getSellerID());
                    timedOutReplies.add(m.getLookUpId());
                    System.out.println("Added " + m.getLookUpId() + " to already processed lookUp Ids");
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

    public void startLookUp() throws Exception {
        String lookupId = UUID.randomUUID().toString();
        Message m = new Message();
        m.setLookUpId(lookupId);
        m.setRequestedItem(buyerItem);
        m.setPath(new ArrayList<>());

        System.out.println("Started a new lookUp with lookUp Id: " + lookupId + ", requested item: "+ m.getRequestedItem());

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
            System.out.println("Did not get a reply for the lookup request of " + buyerID +
                        "Timing out and ignoring future replies for this request");
            timedOutReplies.add(lookupId);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        semaphore.release();
    }

}
