import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// class for creating an election message
public class ElectionMessage implements Serializable {

    public List<Integer> peerIDs = new ArrayList<>();

    public List<Integer> getPath() {
        return peerIDs;
    }

    public void setPath(List<Integer> path) {
        this.peerIDs = path;
    }

    public void addLastInPath(int ID) {
        this.peerIDs.add(ID);
    }

    public String sellerItem;

    public int removeLastNodeInPath() { //check return type
        int lastNode = peerIDs.get(peerIDs.size()-1);
        peerIDs.remove(peerIDs.get(peerIDs.size()-1));
        return lastNode;
    }

}
