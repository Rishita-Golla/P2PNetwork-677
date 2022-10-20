import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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

    public void processMessageForward(Message m) throws MalformedURLException {};
    public void processReply(Message m) throws InterruptedException {};

    public void startCommunication() {

    }

    // A seller checks if he has the item, else broadcasts
    // A buyer directly broadcasts the message
    public static void checkOrBroadcastMessage(Message m, String productName, int ID) throws MalformedURLException { //can send map<ID, neigh>
        if (m.getRequestedItem().equals(productName)) {
            System.out.println("Seller "+ ID + " has item " + m.getRequestedItem()+" .Starting backward reply ");
            m.setSellerID(ID);
            replyBackwards(m);
            return;
        }

        if(m.getHopCount() >= Constants.MAX_HOP) {
            System.out.println("Reached maximum hop count at ID: "+ID+", returning");
            return;
        }

        m.addLastInPath(ID);
        m.setHopCount(m.getHopCount()+1);

        for (int peerID : neighborPeerIDs.get(ID)) {
            URL url = new URL(peerIdURLMap.get(peerID));
           try{
               Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
               RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
               remoteInterface.checkOrBroadcastMessage(m,productName, peerID); //change location
           } catch (RemoteException | NotBoundException e) {
               e.printStackTrace();
           }
            //send(m, peerID);
        }
    }

    public static void replyBackwards(Message m) {
        //set "Reply" in message and let node handle
        int prevID = m.removeLastNodeInPath();
        System.out.println("Replying backwards to ID: "+ prevID);
        try {
            URL url = new URL(peerIdURLMap.get(prevID));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteInterface.replyBackwards(m); // implement at interface's place
            //create a new class for implementing Remote Interface

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}