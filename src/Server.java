import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;


class ServerThread implements Runnable {
    String url;
    int ID;
    String role;
    Thread t;
    public ServerThread(int ID){
        this.url = PeerCommunication.peerIdURLMap.get(ID);
        this.ID = ID;
    }
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    Date date = new Date(System.currentTimeMillis());

    @Override
    public void run() {
        System.out.printf(formatter.format(date)+" Node %d running as a Lookup server on url %s..\n", this.ID, url);
        int port;
        try {
            port = new URL(this.url).getPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(" Failed to start the Server");
        }
        try {
            RemoteInterfaceImpl obj = new RemoteInterfaceImpl();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);
            System.setProperty("java.rmi.server.hostname", new URL(this.url).getHost());
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("RemoteInterface", stub);
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
    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
    Date date = new Date(System.currentTimeMillis());

    public RemoteInterfaceImpl(){

    }

    public RemoteInterfaceImpl(int nodeID, String productName) {
        this.nodeID = nodeID;
        this.productName = productName;
    }

    public void checkOrBroadcastMessage(Message m, int peerID, String role) throws MalformedURLException {
        System.out.println(formatter.format(date)+" Reached new node for communication "+ peerID + " item"+ Seller.sellerItem);
        if(role.equals("buyer"))
            PeerCommunication.checkOrBroadcastMessage(m, "", peerID, "buyer");
        else
            PeerCommunication.checkOrBroadcastMessage(m, Seller.sellerItem, peerID, "seller");
    }

    public boolean sellItem(String requestedItem) {
       // System.out.println("In RemoteImpl sellItem");
        return Seller.sellItem(requestedItem); //Server.java
    }

    public void replyBackwards(Message m, String role) {
      //  System.out.println("In RemoteInterImpl replyBackwards");
        if(role.equals("buyer"))
            Client.buyer.processReply(m);
        else
            Seller.processReply(m);
    }
}

public class Server {
    public static Logger logger;
    public static Integer ID;
    public static void main(String[] args) throws Exception{
        String pathToConfigFile;
        String pathToCommonFile;
        String[] productsToSell;
        String[] productsToRestock;
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());

       // System.out.println(formatter.format(date));

        try {
            ID = Integer.parseInt(args[0]);
            pathToCommonFile = args[1];
            pathToConfigFile = args[2];
            productsToSell = args[3].split(",");
            if(args.length>=5)
                productsToRestock = args[4].split(",");
            else
                productsToRestock = productsToSell;

        }catch (Exception e){ //change
            System.err.println("Incorrect arguments. Usage java -c destination Server {id} {pathToConfig} {products to sell separated by ,} {maxCount} {[optional] products to restock separated by ,}");
            throw e;
        }

        Properties prop;
        try (InputStream input = new FileInputStream(pathToCommonFile)) {
            prop = new Properties();
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                String value = (String) entry.getValue();
                String[] peersAndRoles = value.split(",");
                PeerCommunication.peerIdURLMap.put(Integer.parseInt((String) entry.getKey()),peersAndRoles[0]);
                PeerCommunication.rolesMap.put(Integer.parseInt((String) entry.getKey()),peersAndRoles[1]);
            }
        }

        HashMap<Integer,String> sellerItems = new HashMap<>();
        try (InputStream input = new FileInputStream(pathToConfigFile)) {
            prop = new Properties();
            // load a properties file
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                ArrayList<Integer> list = new ArrayList();
                int key = Integer.parseInt((String) entry.getKey());
                //set.add(key);
                String value = (String) entry.getValue();
                String[] URLandNeighbors = value.split(",");
                //sellerItems.put(key,URLandNeighbors[1]);
                for (int i = 1; i < URLandNeighbors.length; i++)
                    list.add(Integer.parseInt(URLandNeighbors[i]));
                PeerCommunication.neighborPeerIDs.put(key,list);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        //System.out.println(sellerItems);
        //Seller.sellerItem = productsToSell[0];
        Seller.setSellerItem(productsToSell[0]);
        ServerThread serverThread = new ServerThread(ID);
        serverThread.start();
//        System.out.println(formatter.format(date)+" PeerURLMap "+PeerCommunication.peerIdURLMap);
//        System.out.println("**************");
//        System.out.println(formatter.format(date)+ " PeerNeighborsMap "+PeerCommunication.neighborPeerIDs);
        System.out.println(formatter.format(date)+" I am node:+"+ID+"selling item:"+Seller.sellerItem);


//        for (Map.Entry<Integer,String> entry : sellerItems.entrySet()) {
//
//            System.out.println(PeerCommunication.peerIdURLMap);
//
//            System.out.println(PeerCommunication.neighborPeerIDs);
//            System.err.println("ID:"+entry.getValue());
//        }


    }
}
