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

    public static HashMap<Integer, HashMap<String, Integer>> sellerItemCountMap = new HashMap<>(); //initialize it (PeerComm) or keep it with trader

    static Date date = new Date(System.currentTimeMillis());
    static Leader leader;

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
    /**
     * LAB 2 CODE
     */

    public static void sendLeaderIDBackwards(ElectionMessage message, int leaderID) {
        // System.out.println("Path of message before removal"+ message.getPath());
        if(message.peerIDs.size() != 0){
            int nodeID = message.removeLastNodeInPath();
            Leader.leaderID = leaderID;
            try {
                URL url = new URL(peerIdURLMap.get(nodeID));
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendLeaderIDBackwards(message, leaderID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendLeaderElectionMsg(ElectionMessage message, int nodeID) throws MalformedURLException, InterruptedException {

        int maxNodeID = 0; //next node ID to send call to

        //if msg already contains this node we should stop
        if (message.peerIDs.contains(nodeID)) {
            Leader.leaderID = Collections.max(message.peerIDs);
            System.out.println(formatter.format(date)+" I am at the initial node. Election should stop. Elected node is: " + Leader.leaderID);
            leader = new Leader(Leader.leaderID);
            sendLeaderIDBackwards(message, Leader.leaderID);
        }else {
            if(nodeID == Leader.leaderID) {
                //pick random item
                //set seller ID to process
                //update seller map

                List<Integer> neighbours =  neighborPeerIDs.get(nodeID);
                for(int node: neighbours) {
                    if(!message.getPath().contains(node)) {
                        maxNodeID = node;
                    }
                }
            } else{
                message.peerIDs.add(nodeID);
                if(nodeID == peerIdURLMap.size()){
                    maxNodeID = 1;
                }else if(nodeID == 1){
                    maxNodeID = Collections.min(neighborPeerIDs.get(nodeID));
                }else{
                    maxNodeID = Collections.max(neighborPeerIDs.get(nodeID));
                }
            }
            System.out.println(formatter.format(date)+" At node ID: "+nodeID +" sending election request forward to: " + maxNodeID);
            URL url = new URL(peerIdURLMap.get(maxNodeID));
            try {
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendLeaderElectionMsg(message, maxNodeID);
            }
            catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static String checkLeaderStatus() {
        System.out.println(formatter.format(date)+" Check status leaderID: "+Leader.leaderID);
        if(Leader.processedRequestsCount <= 2){
            return "OK";
        }else {
            leader.writeDataToFile();
            return "DOWN";
        }
    }

    protected static void sendBuyMessage(Message m) throws MalformedURLException {
        //String checkString = m.getBuyerID()+"-"+m.
        if(leader != null && Leader.priorityQueue != null) {
            Leader.priorityQueue.add(m);
            System.out.println(formatter.format(date)+" Added buyer's message to trader");
        }
    }

    public static void addRequestToQueue(Message m) {
        if(Leader.priorityQueue != null) {
            Leader.priorityQueue.add(m);
            System.out.println(formatter.format(date)+" Adding request of buyer ID:" + m.getBuyerID() + " to the queue");
            //System.out.println("size of queue:" + Leader.priorityQueue.size());
        }
    }

    public static void sendTransactionAck(int buyerID, int sellerID, int income) throws MalformedURLException {
       //System.out.println("Inside sendTransactionAck");
        List<Integer> peerIDs = List.of(buyerID, sellerID);
        for (int peerID : peerIDs) {
            URL url = new URL(PeerCommunication.peerIdURLMap.get(peerID));
            String role = PeerCommunication.rolesMap.get(peerID);
            if(role.equals("buyer"))
                income = 0;
            try {
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendTransactionAck(role, true, income); // for buyer send 0
            } catch (RemoteException | NotBoundException e) {
                // e.printStackTrace();
            }
        }
    }
}