import java.util.ArrayList;
import java.util.List;

public class Message {

    private String requestedItem;
    private String transactionId;
    private int hopCount;
    private List<Integer> path;
    private int SellerID; //set seller ID

    public int getSellerID() {
        return SellerID;
    }

    public void setSellerID(int sellerID) {
        SellerID = sellerID;
    }

    public void Message() {
        this.hopCount = 0;
        path = new ArrayList<Integer>();
    }

    public String getRequestedItem() {
        return requestedItem;
    }

    public void setRequestedItem(String requestedItem) {
        this.requestedItem = requestedItem;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public List<Integer> getPath() {
        return path;
    }

    public void setPath(List<Integer> path) {
        this.path = path;
    }

    public void addLastInPath(int ID) {
        this.path.add(ID);
    }

    public int removeLastNodeInPath() { //check return type
        int lastIndex = path.size()-1;
        path.remove(path.get(lastIndex));

        return lastIndex;
    }
}
