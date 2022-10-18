import java.util.Collections;
import java.util.List;

public class PeerCommunication {

    public void checkOrBroadcastMessage(Message m, String productName, int ID, List<Integer> neighborPeerID) { //can send map<ID, neigh>
        if (m.getRequestedItem().equals(productName)) {
            replyBackwards(m);
            return;
        }

        if(m.getHopCount() >= Constants.MAX_HOP) {
            return;
        }

        m.addLastInPath(ID);
        m.setHopCount(m.getHopCount()+1);

        for (int peerID : neighborPeerID) {
            //get dest IP from registry
            send(m, peerID);
        }
    }

    public void replyBackwards(Message m) {
        //set "Reply" in message and let node handle
        int prevID = m.removeLastNodeInPath();
        send(m, prevID);
    }

    public void send(Message m, int ID) {
        // get URL/address from registry
        // send
    }
}