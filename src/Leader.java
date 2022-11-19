import java.net.MalformedURLException;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

public class Leader extends PeerCommunication {

    public static int leaderID;
    public static PriorityQueue<Message> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Message::getTimestamp));
    static int processedRequestsCount;

    public Leader(int leaderID) {
        Leader.leaderID = leaderID;
        processedRequestsCount = 0;
        readDataFromFile();
        new ProcessThread().start();
    }

    public int getLeaderID() {
        return leaderID;
    }

    public void setLeaderID(int leaderID) {
        Leader.leaderID = leaderID;
    }

    public void readDataFromFile() {
       // System.out.println("In readDataFromFile");
        BufferedReader br = null;
        try {
            String outputPath = "src/sellerInfo.txt";
            File file = new File(outputPath);
            // create BufferedReader object from the File
            br = new BufferedReader(new FileReader(file));
            String line = null;
            int sellerID = -1;
            HashMap<String,Integer> map = new HashMap<>();
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
            System.out.println("Sellers registered with leader successfully:  "+PeerCommunication.sellerItemCountMap);
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
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    private  void checkQueueMessages() throws MalformedURLException {
       // CompletableFuture.runAsync(PeerCommunication::processQueue);
        if(Leader.priorityQueue.size() > 1) {
            System.out.println("Current queue size is: "+ Leader.priorityQueue.size() + " processing queue messages.");
            Message m = Leader.priorityQueue.poll();
            if(processQueueMessage(m)) {
                processedRequestsCount++;
            }
        }
    }

    private  boolean processQueueMessage(Message m) throws MalformedURLException {

        System.out.println("Started processing request of buyerID: " +m.getBuyerID());
        if(m.getBuyerID() == Leader.leaderID)
            return false;

        String requestedItem = m.getRequestedItem();
        boolean foundSeller = false;
        int sellerID = -1;
        int totalIncome = 0;

        for(Map.Entry<Integer, HashMap<String, Integer>> entry : PeerCommunication.sellerItemCountMap.entrySet()) {
            if(entry.getKey() == Leader.leaderID)
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
            PeerCommunication.sendTransactionAck(m.getBuyerID(), sellerID, (int) (totalIncome - 0.2*totalIncome));
        }
        return foundSeller;
    }

    private int sellItemToBuyer(int sellerID, Message m) {
        HashMap<String, Integer> map = PeerCommunication.sellerItemCountMap.get(sellerID);
        int count = map.get(m.getRequestedItem());
        map.put(m.getRequestedItem(), --count);
        if(count == 0){
            PeerCommunication.sellerItemCountMap.remove(sellerID);
        }else{
            PeerCommunication.sellerItemCountMap.put(sellerID,map);
        }
        System.out.println("Sold requested item " + m.getRequestedItem() + " to buyer: " + m.getBuyerID() + " from sellerID: "+ sellerID);
        int income = Constants.SELLER_PURCHASE_PRICES.get(m.getRequestedItem());
        return income;
    }
}
