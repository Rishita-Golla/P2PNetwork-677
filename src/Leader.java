import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Leader {

    PriorityQueue<Message> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(Message::getTimestamp));
    public HashMap<Integer, HashMap<String, Integer>> sellerItemCountMap = new HashMap<>(); //initialize it (PeerComm) or keep it with trader
    int processedRequests;

    Leader() {
        this.processedRequests = 0;
        readDataFromFile();
        new ProcessThread().start();
    }

    private void readDataFromFile() {
        // open file
        // read data about buyer requests and seller items
        // initialize queue with requests
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

    public void processBuyMessage(Message m) {
        priorityQueue.add(m);
    }
}
