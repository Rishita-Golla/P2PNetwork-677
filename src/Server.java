import org.w3c.dom.Node;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import javafx.util.Pair;

class ServerThread implements Runnable {
    String url;
    int ID;
    Thread t;

    String role;
    public ServerThread(int ID, String role){
        this.url = Nodes.nodeURLs.get(ID);
        this.ID = ID;
        this.role = role;
    }

    @Override
    public void run() {
        int port;
        try {
            port = new URL(this.url).getPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start the Server");
        }
        try {
            ServerRMI obj = new ServerRMI();
            SellerNode stub = (SellerNode) UnicastRemoteObject.exportObject(obj, 0);
            System.setProperty("java.rmi.server.hostname", new URL(this.url).getHost());
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("SellerNode", stub);
            System.out.printf("Node %d running as a Lookup server on url %s..\n", this.ID, url);

            if(role == "Seller"){
                String prod = hm.get(ID)
                Seller.setProducts(prod);
                //Seller.setProductsToRestock(productsToRestock);
            }else{

            }
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

class ServerRMI implements SellerNode {
    public ServerRMI() {
    }

    public void floodLookUps(String itemName, int maxHopCount, String lookupId, Stack<Integer> path) {
        System.out.printf("Looking up product %s\n", itemName);
        Lookup lookup = new Lookup(Server.ID, Seller.productName);
        try {
            lookup.floodLookUps(itemName, maxHopCount, lookupId, path);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ;
    }

    public boolean sellProduct(String itemName) {
        Server.logger.info(String.format("Selling product %s\n", itemName));
        return Seller.sellProduct(itemName);
    }

    @Override
    public void sendReplyBackToBuyer(Stack<Integer> pathToTraverse, ReplyMessage messageToSend) {
        Reply reply = new Reply(pathToTraverse, messageToSend);
        reply.sendReplyBackToBuyer();
    }
}

public class Server {
    public static Logger logger;
    public static Integer ID;
    public static HashMap<Integer,Pair[] >> productsMap
    public static void main(String[] args) throws Exception{
        String pathToConfigFile;
        String[] productsToSell;
        String[] productsToRestock;
        Set<Integer> hashSet = new HashSet<>();
        try {
            ID = Integer.parseInt(args[0]);
            pathToConfigFile = args[1];
//            productsToSell = args[2].split(",");
//            Seller.maxProductCount = Integer.parseInt(args[3]);
//            if(args.length>=5)
//                productsToRestock = args[4].split(",");
//            else
//                productsToRestock = productsToSell;

        }catch (Exception e){
            System.err.println("Incorrect arguments. Usage java -c destination Server {id} {pathToConfig} {products to sell separated by ,} {maxCount} {[optional] products to restock separated by ,}");
            throw e;
        }

        logger = Logger.getLogger("ServerLog");
        FileHandler fh;
//        try {
//            // This block configure the logger with handler and formatter
//            fh = new FileHandler(String.format("Node_%d_server.log", ID), true);
//            logger.addHandler(fh);
//            MyLogFormatter formatter = new MyLogFormatter(ID);
//            fh.setFormatter(formatter);
//            // the following statement is used to log any messages
//        } catch (SecurityException | IOException exception) {
//            exception.printStackTrace();
//        }
//        Seller.setProducts(productsToSell);
//        Seller.setProductsToRestock(productsToRestock);
        Properties prop;
        // Read urls of all the nodes in my peer to peer network.
        /*
         * Config file to have below structure
         * NodeID=<URLofNode>,<Comma separated list of neighbors>
         * 1=http://127.0.0.1:5000,2,3
         * 2=http://127.0.0.1:5001,3,1
         * 3=http://127.0.0.1:5002,1,3
         */
        try (InputStream input = new FileInputStream(pathToConfigFile)) {
            prop = new Properties();
            // load a properties file
            prop.load(input);

            //create a global hashtable and a list of neighbours
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                ArrayList neighbours = new ArrayList<>();
                int key = Integer.parseInt((String) entry.getKey());
                String value = (String) entry.getValue();
                String[] values = value.split(",");
                if(values[1].equals("S")){

                }else if(values[1].equals("B"))
                    hashSet.add(key);
                Nodes.nodeURLs.put(key, values[0]);
                for (int i = 2; i < values.length; i++)
                    neighbours.add(Integer.parseInt(values[i]));
                Nodes.adjacencyList.put(key,neighbours);

            }
            System.out.println(Nodes.nodeURLs);
            System.out.println("***********************************");
            System.out.println(Nodes.adjacencyList);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        for (Map.Entry<Integer, String> entry : Nodes.nodeURLs.entrySet()) {
            try {
                // This block configure the logger with handler and formatter
                fh = new FileHandler(String.format("Node_%d_server.log", entry.getKey()), true);
                logger.addHandler(fh);
                MyLogFormatter formatter = new MyLogFormatter(ID);
                fh.setFormatter(formatter);
                // the following statement is used to log any messages
            } catch (SecurityException | IOException exception) {
                exception.printStackTrace();
            }
            if(hashSet.contains(entry.getKey())){
                continue;
            }else{
                ServerThread serverThread = new ServerThread(entry.getKey(), "Seller");
                serverThread.start();
                TimeUnit.SECONDS.sleep(1);
            }
        }

        System.err.printf("Hi. I am node %d running as a seller. I got %d number of product %s to sell." +
                " Hit me up!\n", ID, Seller.maxProductCount, Seller.productName);

    }
}