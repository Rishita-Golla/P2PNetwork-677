import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Client {
    public static Logger logger;
    public static Buyer buyer;

    private Client() {}

    public static void main (String[] args) throws Exception {
        int id = Integer.parseInt(args[0]);
        String configFilePath = args[2];
        String pathToCommonFile = args[1];
        String[] products = args[3].split(",");
//        logger = Logger.getLogger("ClientLog");
//        FileHandler fh;
//        try {
//            // This block configure the logger with handler and formatter
//            fh = new FileHandler(String.format("Node_%d_client.log", id), true);
//            MyLogFormatter formatter = new MyLogFormatter(id);
//            fh.setFormatter(formatter);
//            logger.addHandler(fh);
//            // the following statement is used to log any messages
//        } catch (SecurityException | IOException exception) {
//            exception.printStackTrace();
//            System.out.println("Failed to setup logging.");
//        }
        buyer = new Buyer(id, products[0]);
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

        try (InputStream input = new FileInputStream(configFilePath)) {
            prop = new Properties();
            // load a properties file
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                ArrayList<Integer> list = new ArrayList();
                Integer key = Integer.parseInt((String) entry.getKey());
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
        ServerThread serverThread = new ServerThread(id);
        serverThread.start();
        while(true){
            try {
                System.out.println("Sleeping for two seconds......");
                Thread.sleep(2000);
            }
            catch(InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }

            // Passing productName as empty string because a buyer doesn't sell any product.
            // Lookup is used by the server nodes too where they pass the product name they sell to this.
            try{
               buyer.startLookUp();
            } catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }
    }
}