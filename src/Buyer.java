import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
        try {
            URL url = new URL(peerIdURLMap.get(sellerId));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("remoteInterface");
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
    }

    public void processMessageForward(Message m) {
        checkOrBroadcastMessage(m, "", buyerID);
    }

    public void processReply(Message m) {
        if(m.getPath().isEmpty()) { // reached initial buyer node
            buyItemDirectlyFromSeller(m.getSellerID());
        } else { // an intermediate node
            replyBackwards(m);
        }
    }

    public void startLookUp() {
        String lookupId = UUID.randomUUID().toString();
        Message m = new Message();
        m.setLookUpId(lookupId);
        m.setRequestedItem(buyerItem);

        checkOrBroadcastMessage(m, "", buyerID);

    }


}
