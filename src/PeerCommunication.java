import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.*;

public class PeerCommunication {

    public static HashMap<Integer, String> peerIdURLMap = new HashMap<>();
    public static HashMap<Integer, List<Integer>> neighborPeerIDs = new HashMap<>();
    public static HashMap<Integer, String> rolesMap = new HashMap<Integer, String>();
    public static List<String> processedLookUps = new ArrayList<>();
    static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    static Date date = new Date(System.currentTimeMillis());

    public PeerCommunication() {

    }

    public PeerCommunication(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        this.peerIdURLMap = peerIdURLMap;
        this.neighborPeerIDs = neighborPeerIDs;
    }

   // public void processMessageForward(Message m) throws MalformedURLException {};
    //public void processReply(Message m) throws InterruptedException {};

    public void startCommunication() {

    }

    // A seller checks if he has the item, else broadcasts
    // A buyer directly broadcasts the message
    public static void checkOrBroadcastMessage(Message m, String productName, int ID, String role) throws MalformedURLException { //can send map<ID, neigh>
        System.out.println(formatter.format(date)+PeerCommunication.peerIdURLMap);
        //System.out.println("************** in client");
        System.out.println(formatter.format(date)+PeerCommunication.neighborPeerIDs);

        System.out.println(formatter.format(date)+" Message props: "+m.getHopCount()+m.getPath());

        if(processedLookUps.contains(m.getLookUpId())){
            System.out.println(formatter.format(date)+" Processed lookUpId "+m.getLookUpId()+", returning");
            return;
        }
        processedLookUps.add(m.getLookUpId());

        if (m.getRequestedItem().equals(productName)) {
            System.out.println(formatter.format(date)+" Seller "+ ID + " has item " + m.getRequestedItem()+" .Starting backward reply ");
            m.setSellerID(ID);
            replyBackwards(m);
            return;
        }

        if(m.getHopCount() >= Constants.MAX_HOP) {
            System.out.println(formatter.format(date)+" Reached maximum hop count at ID: "+ID+", returning");
            return;
        }

        m.addLastInPath(ID);
        m.setHopCount(m.getHopCount()+1);
        List<Integer> currPath = m.getPath();

        for (int peerID : neighborPeerIDs.get(ID)) {
            URL url = new URL(peerIdURLMap.get(peerID));
            role = rolesMap.get(peerID);
            if(currPath.contains(peerID))
                continue;
           try{
               Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
               RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
               remoteInterface.checkOrBroadcastMessage(m, peerID, role); //change location
           } catch (RemoteException | NotBoundException e) {
               e.printStackTrace();
           }
        }
    }

    public static void replyBackwards(Message m) {
        //set "Reply" in message and let node handle
        int prevID = m.removeLastNodeInPath();
        System.out.println(formatter.format(date)+" Replying backwards to ID: "+ prevID);
        String role = rolesMap.get(prevID);
        try {
            URL url = new URL(peerIdURLMap.get(prevID));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteInterface.replyBackwards(m, role); // implement at interface's place

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}