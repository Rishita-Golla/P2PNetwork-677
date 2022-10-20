import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Buyer extends PeerCommunication{

    public String buyerItem;
    protected int buyerID;

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
            return remoteInterface.sellItem(this.buyerItem); // implement at interface's place
            //create a new class for implementing Remote Interface

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void pickNewBuyerItem() {
        Random rand = new Random();
        int size = Constants.POSSIBLE_ITEMS.size();
        buyerItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(size));

        System.out.println("Picked up "+ buyerItem+ " as new buyer item for ID: "+buyerID);
    }

    public void processMessageForward(Message m) throws MalformedURLException {
        checkOrBroadcastMessage(m, "", buyerID);
    }

    public void processReply(Message m) {
        if(m.getPath().isEmpty()) { // reached initial buyer node
            System.out.println("Reached initial buyer in reply backward path");
            if(buyItemDirectlyFromSeller(m.getSellerID())) {
                System.out.println("Bought item " + m.getRequestedItem() + " from Seller " + "with ID "+m.getSellerID());
                pickNewBuyerItem();
            }
        } else { // an intermediate node
            replyBackwards(m);
        }
    }

    public void startLookUp() throws MalformedURLException {
        String lookupId = UUID.randomUUID().toString();
        Message m = new Message();
        m.setLookUpId(lookupId);
        m.setRequestedItem(buyerItem);
        m.setPath(new ArrayList<>());

        System.out.println("Started a new lookUp with lookUp Id: " + lookupId + ", requested item: "+ m.getRequestedItem());

        checkOrBroadcastMessage(m, "", buyerID);

    }


}
