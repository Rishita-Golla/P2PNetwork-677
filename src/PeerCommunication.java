import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PeerCommunication {

    public static HashMap<Integer, String> peerIdURLMap = new HashMap<>();
    public static HashMap<Integer, List<Integer>> neighborPeerIDs = new HashMap<>();

    public PeerCommunication() {

    }

    public PeerCommunication(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        this.peerIdURLMap = peerIdURLMap;
        this.neighborPeerIDs = neighborPeerIDs;
    }

    public void processMessageForward(Message m) {};
    public void processReply(Message m) throws InterruptedException {};

    public void startCommunication() {

    }

    // A seller checks if he has the item, else broadcasts
    // A buyer directly broadcats the message
    public static void checkOrBroadcastMessage(Message m, String productName, int ID) { //can send map<ID, neigh>
        if (m.getRequestedItem().equals(productName)) {
            replyBackwards(m);
            return;
        }

        if(m.getHopCount() >= Constants.MAX_HOP) {
            return;
        }

        m.addLastInPath(ID);
        m.setHopCount(m.getHopCount()+1);

        for (int peerID : neighborPeerIDs.get(ID)) {
           try{
               URL url = new URL(peerIdURLMap.get(ID));
               Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
               RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("remoteInterface");
               remoteInterface.checkOrBroadcastMessage(m,productName, peerID); //change location
           } catch (MalformedURLException | RemoteException | NotBoundException e) {
               e.printStackTrace();
           }
            //send(m, peerID);
        }
    }

    public static void replyBackwards(Message m) {
        //set "Reply" in message and let node handle
        int prevID = m.removeLastNodeInPath();
        try {
            URL url = new URL(peerIdURLMap.get(prevID));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("remoteInterface");
            remoteInterface.replyBackwards(m); // implement at interface's place
            //create a new class for implementing Remote Interface

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}