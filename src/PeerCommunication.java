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
    public static int leaderID = 0;
    public static Leader leader = null;

    public PeerCommunication() {

    }

    public PeerCommunication(HashMap<Integer, String> peerIdURLMap, HashMap<Integer, List<Integer>> neighborPeerIDs) {
        PeerCommunication.peerIdURLMap = peerIdURLMap;
        PeerCommunication.neighborPeerIDs = neighborPeerIDs;
    }

    // A seller checks if he has the item, else broadcasts
    // A buyer directly broadcasts the message
    public static void checkOrBroadcastMessage(Message m, String productName, int ID, String role) throws MalformedURLException { //can send map<ID, neigh>
        System.out.println(formatter.format(date)+PeerCommunication.peerIdURLMap);
        System.out.println(formatter.format(date)+PeerCommunication.neighborPeerIDs);

       // System.out.println(formatter.format(date)+" Message props: "+m.getHopCount()+m.getPath());

        // If a lookUp has already been processed ignore it
        if(processedLookUps.contains(m.getLookUpId())){
            System.out.println(formatter.format(date)+" Processed lookUpId "+m.getLookUpId()+", returning");
            return;
        }
        processedLookUps.add(m.getLookUpId());

        // check if the requested item is same as product name (applicable only for seller)
        if (m.getRequestedItem().equals(productName)) {
            System.out.println(formatter.format(date)+" Seller "+ ID + " has item " + m.getRequestedItem()+" .Starting backward reply ");
            m.setSellerID(ID);
            // start reply backwards in the path
            replyBackwards(m);
            return;
        }

        if(m.getHopCount() >= Constants.MAX_HOP) {
            System.out.println(formatter.format(date)+" Reached maximum hop count at ID: "+ID+", returning");
            return;
        }

        // add node at end of path and increment hop count
        m.addLastInPath(ID);
        m.setHopCount(m.getHopCount()+1);
        List<Integer> currPath = m.getPath();

        // send message to peer nodes
        for (int peerID : neighborPeerIDs.get(ID)) {
            URL url = new URL(peerIdURLMap.get(peerID));
            role = rolesMap.get(peerID);
            if(currPath.contains(peerID))
                continue;
           try{
               Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
               RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
               remoteInterface.checkOrBroadcastMessage(m, peerID, role);
           } catch (RemoteException | NotBoundException e) {
               e.printStackTrace();
           }
        }
    }

    // get last node in the path and send reply backwards
    public static void replyBackwards(Message m) {
        int prevID = m.removeLastNodeInPath();
        System.out.println(formatter.format(date)+" Replying backwards to ID: "+ prevID);
        String role = rolesMap.get(prevID);
        try {
            URL url = new URL(peerIdURLMap.get(prevID));
            Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
            RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
            remoteInterface.replyBackwards(m, role);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendLeaderElectionMsg(ElectionMessage message, int nodeID) throws MalformedURLException {

        int maxNodeID = 0; //next node ID to send call to

        //if msg already contains this node we should stop
        if (message.peerIDs.contains(nodeID)) {
            System.out.println("I am at the initial node. Election should stop.");
            int leaderID = Collections.max(message.peerIDs);
            leader = new Leader();
            System.out.println("Elected node is "+leaderID);
            for(int peerID: message.getPath())
            {
                if(rolesMap.get(peerID).equals("seller")){
                    //sellerInfo.put(peerID,)
                }
            }
            return;
        }else{
            if(nodeID == leaderID) {
                List<Integer> neighbours =  neighborPeerIDs.get(nodeID);
                for(int node: neighbours) {
                    if(!message.getPath().contains(node)) {
                        maxNodeID = node;
                    }
                }
            }else{
                message.peerIDs.add(nodeID);
                System.out.println("At node ID: "+nodeID +" sending election request forward.");
                if(nodeID == peerIdURLMap.size()){
                    maxNodeID = 1;
                }else if(nodeID == 1){
                    maxNodeID = Collections.min(neighborPeerIDs.get(nodeID));
                }else{
                    maxNodeID = Collections.max(neighborPeerIDs.get(nodeID));
                }
            }
            URL url = new URL(peerIdURLMap.get(maxNodeID));
            try {
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendLeaderElectionMsg(message, maxNodeID);
            }
            catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }

            return;
        }
    }

    public static String sendLeaderStatus() {
        if(leader.processedRequests <= 10){
            return "OK";
        }else
            return "DOWN";
    }

}