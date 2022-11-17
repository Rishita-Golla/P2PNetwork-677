import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
}
