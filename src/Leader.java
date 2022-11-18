import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.io.*;

public class Leader {

    int LeaderID;

    public int getLeaderID() {
        return LeaderID;
    }

    public void setLeaderID(int leaderID) {
        LeaderID = leaderID;
    }
    PriorityQueue<Message> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Message::getTimestamp));
    public HashMap<Integer, HashMap<String, Integer>> sellerItemCountMap = new HashMap<>(); //initialize it (PeerComm) or keep it with trader
    int processedRequests;

    Leader() {
        this.processedRequests = 0;
        readDataFromFile();
        new ProcessThread().start();
    }

    public void readDataFromFile() {
        BufferedReader br = null;
        try {
            String outputPath = "sellerInfo.txt";
            File file = new File(outputPath);

            // create BufferedReader object from the File
            br = new BufferedReader(new FileReader(file));

            String line = null;
            while ((line = br.readLine()) != null) {

                // split the line by :
                String[] parts = line.split(":");
                // first part is name, second is number
                int sellerID = Integer.parseInt(parts[0].trim());
                String[] sellerInfo = parts[1].trim().split(",");
                String item = sellerInfo[0];
                int itemCount = Integer.parseInt(sellerInfo[1].trim());
                if(sellerID == getLeaderID()) {
                    continue;
                }else{
                    HashMap<String,Integer> map = new HashMap();
                    map.put(item, itemCount);
                    PeerCommunication.sellerItemCountMap.put(sellerID, map);
                }
            }
            br.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeDataToFile() {
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
            }
            bf.flush();
            bf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public class ProcessThread extends Thread{
        public void run() {
            while(true){
                try {
                    checkQueueMessages();
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        }
    }

    private void checkQueueMessages() {
        if(priorityQueue.size() > 1) {
            Message m = priorityQueue.poll();
            if(processQueueMessage(m)) {
                // don't process trader's buy request
                Buyer.processBuy(); // display bought item and add lookUp Id to processed LookUps
                this.processedRequests++;
            }
        } else {
            System.out.println("No messages to process in queue");
        }
    }

    private boolean processQueueMessage(Message m) {
        String requestedItem = m.getRequestedItem();
        boolean foundSeller = false;
        int sellerID = -1;

        for(Map.Entry<Integer, HashMap<String, Integer>> entry : sellerItemCountMap.entrySet()) {
            if(entry.getKey() == PeerCommunication.leaderID)
                continue;
            for(Map.Entry<String, Integer> itemAndCountMap : entry.getValue().entrySet())
                if(itemAndCountMap.getKey().equals(requestedItem) && itemAndCountMap.getValue() > 0) {
                    foundSeller = true;
                    sellerID = entry.getKey();
                    break;
                }
            }

        if(foundSeller)
            sellItemToBuyer(sellerID, m);
        return foundSeller;
    }

    private void sellItemToBuyer(int sellerID, Message m) {
        System.out.println("Selling requested item" + m.getRequestedItem() + " to buyer: " + m.getBuyerID() + "from sellerID"+ sellerID);
        // update trader sellerItemCountMap



    }

    public void sendTransactionAck(int buyerID, int sellerID) throws MalformedURLException {
        List<Integer> peerIDs = List.of(buyerID, sellerID);
        int income = 0; // get this from map
        for (int peerID : peerIDs) {
            URL url = new URL(PeerCommunication.peerIdURLMap.get(peerID));
            String role = PeerCommunication.rolesMap.get(peerID);
            try {
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendTransactionAck(role, true, income); // for buyer send 0
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
}
