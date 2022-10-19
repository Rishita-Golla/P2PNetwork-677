import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Buyer extends PeerCommunication{

    public String buyerItem;
    protected int buyerID;

    public Buyer(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        super(peerIdURLMap, neighborPeerIDs);
    }

    public  boolean buyItemDirectlyFromSeller(int sellerId) {
        //get Seller address -> Seller
        try {
            URL url = new URL(peerIdURLMap.get(sellerId));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("remoteInterface");
            remoteInterface.sellItem(this.buyerItem); // implement at interface's place
            //create a new class for implementing Remote Interface

        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        return Seller.sellItem(buyerItem);
    }

    public void pickNewBuyerItem() {
        Random rand = new Random();
        int size = Constants.POSSIBLE_ITEMS.size();
        buyerItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(size));
    }

    public void processMessageForward(Message m) {
        checkOrBroadcastMessage(m, "", buyerID, neighborPeerIDs.get(buyerID));
    }

    public void processReply(Message m) {
        if(m.getPath().isEmpty()) { // reached initial buyer node
            buyItemDirectlyFromSeller(m.getSellerID());
        } else { // an intermediate node
            replyBackwards(m);
        }
    }


}
