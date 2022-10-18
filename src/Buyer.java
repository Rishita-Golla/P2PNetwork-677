import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Buyer extends PeerCommunication{

    public static String buyerItem;
    protected List<Integer> neighborPeerID;
    protected int buyerID;
  //  public static final List<String> possibleItems = Arrays.asList("FISH","SALT","BOAR");

    public void Buyer(String buyerItem, List<Integer> neighborPeerID) {
        this.buyerItem = buyerItem;
        this.neighborPeerID = neighborPeerID;
    }

    public  boolean buyItemDirectlyFromSeller(int sellerId) {
        //get Seller address -> Seller
        return Seller.sellItem(buyerItem);
    }

    public void pickNewBuyerItem() {
        Random rand = new Random();
        int size = Constants.POSSIBLE_ITEMS.size();
        buyerItem = Constants.POSSIBLE_ITEMS.get(rand.nextInt(size));
    }

    public void processMessageForward(Message m) {
        checkOrBroadcastMessage(m, "", buyerID, neighborPeerID);
    }

    public void processReply(Message m) {
        if(m.getPath().isEmpty()) { // reached initial buyer node
            buyItemDirectlyFromSeller(m.getSellerID());
        } else { // an intermediate node
            replyBackwards(m);
        }
    }


}
