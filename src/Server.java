import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;


class ServerThread implements Runnable {
    String url;
    int ID;

    String role;
    Thread t;
    public ServerThread(int ID, String role){
        this.url = PeerCommunication.peerIdURLMap.get(ID);
        //this.url = Nodes.nodes.get(ID);
        this.ID = ID;
        this.role = role;
    }

    @Override
    public void run() {
        System.out.printf("Node %d running as a Lookup server on url %s..\n", this.ID, url);
        int port;
        try {
            port = new URL(this.url).getPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start the Server");
        }
        try {
            RemoteInterfaceImpl obj = new RemoteInterfaceImpl();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);
            System.setProperty("java.rmi.server.hostname", new URL(this.url).getHost());
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("SellerNode", stub);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            throw new RuntimeException("Failed to start the server");
        }

    }
    public void start () {
        if (t == null) {
            t = new Thread (this, "Server");
            t.start ();
        }
    }
}

class RemoteInterfaceImpl implements RemoteInterface{

    int nodeID;
    String productName;

    public static Buyer buyer;

    public RemoteInterfaceImpl(){

    }

    public RemoteInterfaceImpl(int nodeID, String productName) {
        this.nodeID = nodeID;
        this.productName = productName;
    }

    @Override
    public void checkOrBroadcastMessage(Message m, String productName, int ID) {
        return;
    }

    @Override
    public boolean sellItem(String requestedItem) {
        return Seller.sellItem(requestedItem); //Server.java
    }

    @Override
    public void replyBackwards(Message m) {
        buyer.processReply(m);
    }
}


//class RemoteInterfaceImpl implements RemoteInterface {
//
//
//    public void floodLookUps(String itemName, int maxHopCount, String lookupId, Stack<Integer> path) {
//        System.out.printf("Looking up product %s\n", itemName);
//        Lookup lookup = new Lookup(Server.ID, Seller.productName);
//        try {
//            lookup.floodLookUps(itemName, maxHopCount, lookupId, path);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return ;
//    }
//
//    public boolean sellProduct(String itemName) {
//        Server.logger.info(String.format("Selling product %s\n", itemName));
//        return Seller.sellItem(itemName);
//    }
//
//    @Override
//    public void sendReplyBackToBuyer(Stack<Integer> pathToTraverse, ReplyMessage messageToSend) {
//        Reply reply = new Reply(pathToTraverse, messageToSend);
//        reply.sendReplyBackToBuyer();
//    }
//}

//public class RemoteInterfaceImpl implements RemoteInterface{
//
//    int nodeID;
//    String productName;
//
//    public RemoteInterfaceImpl(int nodeID, String productName) {
//        this.nodeID = nodeID;
//        this.productName = productName;
//    }
//
//    @Override
//    public void checkOrBroadcastMessage(Message m, String productName, int ID) {
//
//    }
//
//    @Override
//    public boolean sellItem(String requestedItem) {
//        return false;
//    }
//}
public class Server {
    public static Logger logger;
    public static Integer ID;
    public static void main(String[] args) throws Exception{
        String pathToConfigFile;
        String pathToCommonFile;
        String[] productsToSell;
        String[] productsToRestock;
        try {
            ID = Integer.parseInt(args[0]);
            pathToCommonFile = args[1];
            pathToConfigFile = args[2];
            productsToSell = args[3].split(",");
            //Seller.maxProductCount = Integer.parseInt(args[4]);
            if(args.length>=5)
                productsToRestock = args[4].split(",");
            else
                productsToRestock = productsToSell;

        }catch (Exception e){
            System.err.println("Incorrect arguments. Usage java -c destination Server {id} {pathToConfig} {products to sell separated by ,} {maxCount} {[optional] products to restock separated by ,}");
            throw e;
        }

        Properties prop;
        // Read urls of all the nodes in my peer to peer network.
        /*
         * Config file to have below structure
         * NodeID=<URLofNode>,<Comma separated list of neighbors>
         * 1=http://127.0.0.1:5000,2,3
         * 2=http://127.0.0.1:5001,3,1
         * 3=http://127.0.0.1:5002,1,3
         */
        try (InputStream input = new FileInputStream(pathToCommonFile)) {
            prop = new Properties();
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                PeerCommunication.peerIdURLMap.put(Integer.parseInt((String) entry.getKey()),(String) entry.getValue());
            }
        }
        HashSet<Integer> set = new HashSet();
        try (InputStream input = new FileInputStream(pathToConfigFile)) {
            prop = new Properties();
            // load a properties file
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                ArrayList<Integer> list = new ArrayList();
                int key = Integer.parseInt((String) entry.getKey());
                set.add(key);
                String value = (String) entry.getValue();
                String[] URLandNeighbors = value.split(",");
                for (int i = 1; i < URLandNeighbors.length; i++)
                    list.add(Integer.parseInt(URLandNeighbors[i]));
                PeerCommunication.neighborPeerIDs.put(key,list);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        Seller.sellerItem = productsToSell[0];

        for (Integer val : set) {
            ServerThread serverThread = new ServerThread(val, "Seller");

            serverThread.start();
            System.out.println(PeerCommunication.peerIdURLMap);
            System.out.println("***********************************");
            System.out.println(PeerCommunication.neighborPeerIDs);
        }
//        System.err.printf("Hi. I am node %d running as a seller. I got %d number of product %s to sell." +
//                " Hit me up!\n", ID, Seller.maxProductCount, Seller.productName);

    }
}
