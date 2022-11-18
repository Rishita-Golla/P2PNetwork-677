import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Client {
    public static Logger logger;
    public static Buyer buyer;
    public static BuyerAndSeller buyerAndSeller;

    private Client() {}

    public static void writeDataToFile() {
        // open file
        // read data about buyer requests and seller items
        // initialize queue with requests
        String outputPath = "sellerInfo.txt";
        File file = new File(outputPath);
        BufferedWriter bf = null;
        try {
            // create new BufferedWriter for the output file
            bf = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<Integer, HashMap<String,Integer>> entry : PeerCommunication.sellerItemCountMap.entrySet()) {
                for(Map.Entry<String,Integer> entry1: entry.getValue().entrySet()){
                    bf.write(entry.getKey() + ":");
                    bf.write(entry1.getKey());
                    bf.write(",");
                    bf.write(entry1.getValue());
                    bf.newLine();
                }
                bf.write("*");
            }
            bf.flush();
            bf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main (String[] args) throws Exception {
        boolean onlyBuyer = true;
        int id = Integer.parseInt(args[0]);
        String configFilePath = args[2];
        String pathToCommonFile = args[1];
        String[] products = args[3].split(",");

        if(args.length > 4)
            onlyBuyer = false;

        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());

        if(onlyBuyer)
            buyer = new Buyer(id, products[0]);
        else
            buyerAndSeller = new BuyerAndSeller(id, products[0], args[4]);

        Properties prop;
        // creating HashMap for peerIdURL, roles, and neighbours
        try (InputStream input = new FileInputStream(pathToCommonFile)) {
            prop = new Properties();
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                String value = (String) entry.getValue();
                String[] peersAndRoles = value.split(",");
                PeerCommunication.peerIdURLMap.put(Integer.parseInt((String) entry.getKey()),peersAndRoles[0]);
                PeerCommunication.rolesMap.put(Integer.parseInt((String) entry.getKey()),peersAndRoles[1]);
                // Add buyerAndSeller as another role
            }
        }

        try (InputStream input = new FileInputStream(configFilePath)) {
            prop = new Properties();
            // load a properties file
            prop.load(input);
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                List<Integer> list = new ArrayList<>();
                Integer key = Integer.parseInt((String) entry.getKey());
                String value = (String) entry.getValue();
                String[] URLandNeighbors = value.split(",");
                for (int i = 0; i < URLandNeighbors.length; i++)
                    list.add(Integer.parseInt(URLandNeighbors[i]));
                PeerCommunication.neighborPeerIDs.put(key,list);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        ServerThread serverThread = new ServerThread(id);
        serverThread.start();

        //writeDataToFile();
        Thread.sleep(3000);
        ElectionMessage message = new ElectionMessage();
        PeerCommunication.sendLeaderElectionMsg(message, id);

        Thread.sleep(3000);

        while(true) {

            try {
                System.out.println(formatter.format(date) + " Thread sleep - 2 seconds");
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            System.out.println("The leader is:"+ Leader.leaderID);
            // starting buyer lookup
            if (onlyBuyer) {
                try {
                    buyer.startLookUpWithTrader();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            } else {
                try{
                    buyerAndSeller.startLookUpWithTrader();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }

    }
}