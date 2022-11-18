import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class Leader {

    public static int leaderID;
    public static PriorityQueue<Message> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Message::getTimestamp));
    public static int processedRequests;
    static int processedRequestsCount;
    public HashSet<String> requestsInQueue = new HashSet<>();

    Leader(int leaderID) throws InterruptedException {
        this.leaderID = leaderID;
        this.processedRequestsCount = 0;
        readDataFromFile();
        Thread.sleep(2000);
        new ProcessThread().start();
    }

    public int getLeaderID() {
        return this.leaderID;
    }

    public void setLeaderID(int leaderID) {
        this.leaderID = leaderID;
    }

    public void readDataFromFile() {
        BufferedReader br = null;
        try {
            String outputPath = "src/sellerInfo.txt";
            File file = new File(outputPath);
            // create BufferedReader object from the File
            br = new BufferedReader(new FileReader(file));
            String line = null;
            int sellerID = -1;
            HashMap<String,Integer> map = new HashMap();
            while ((line = br.readLine()) != null) {
                // split the line by :
                if(line.equals("*")) {
                    HashMap mapCopy = (HashMap) map.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                    PeerCommunication.sellerItemCountMap.put(sellerID, mapCopy);
                    //System.out.println("SellerItemCountMap"+sellerItemCountMap);
                    map.clear();
                }else{
                    String[] parts = line.split(":");
                    // first part is name, second is number
                    sellerID = Integer.parseInt(parts[0].trim());
                    String[] sellerInfo = parts[1].trim().split(",");
                    String item = sellerInfo[0];
                    int itemCount = Integer.parseInt(sellerInfo[1].trim());
                    if(sellerID == getLeaderID()) {
                        continue;
                    }else{
                        map.put(item, itemCount);
                    }
                }
            }
            br.close();
            System.out.println("SellerItemCountMap"+PeerCommunication.sellerItemCountMap);
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
                bf.write("*");
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

    private void checkQueueMessages() throws MalformedURLException {
        if(priorityQueue.size() > 1) {
            Message m = priorityQueue.poll();
            if(processQueueMessage(m)) {
                Buyer.processBuy(); // display bought item and add lookUp Id to processed LookUps
                processedRequests++;
            } else {
                System.out.println("Didn't process leader's previous buy requests");
            }
//        } else {
//            System.out.println("No messages to process in queue");
        }
    }

    private boolean processQueueMessage(Message m) throws MalformedURLException {

        if(m.getBuyerID() == this.leaderID)
            return false;

        String requestedItem = m.getRequestedItem();
        boolean foundSeller = false;
        int sellerID = -1;
        int totalIncome = 0;

        for(Map.Entry<Integer, HashMap<String, Integer>> entry : PeerCommunication.sellerItemCountMap.entrySet()) {
            if(entry.getKey() == PeerCommunication.leaderID)
                continue;
            for(Map.Entry<String, Integer> itemAndCountMap : entry.getValue().entrySet())
                if(itemAndCountMap.getKey().equals(requestedItem) && itemAndCountMap.getValue() > 0) {
                    foundSeller = true;
                    sellerID = entry.getKey();
                    break;
                }
            }

        if(foundSeller) {
            totalIncome = sellItemToBuyer(sellerID, m);
            sendTransactionAck(m.getBuyerID(), sellerID, (int) (totalIncome - 0.2*totalIncome));
        }
        return foundSeller;
    }

    private int sellItemToBuyer(int sellerID, Message m) throws MalformedURLException {
        System.out.println("Selling requested item" + m.getRequestedItem() + " to buyer: " + m.getBuyerID() + "from sellerID"+ sellerID);
        HashMap<String, Integer> map = PeerCommunication.sellerItemCountMap.get(sellerID);
        int count = map.get(m.getRequestedItem());
        map.put(m.getRequestedItem(), --count);
        String sellerItem = map.keySet().iterator().next();
        return Constants.SELLER_PURCHASE_PRICES.get(sellerItem);
    }

    public void sendTransactionAck(int buyerID, int sellerID, int income) throws MalformedURLException {
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
                e.printStackTrace();
            }
        }
    }
}
