//public class RemoteInterfaceImpl implements RemoteInterface{
//
//    int nodeID;
//    String productName;
//
//    public static Buyer buyer;
//
//    public RemoteInterfaceImpl(){
//
//    }
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
//        Seller.sellItem(requestedItem); //Server.java
//    }
//
//    @Override
//    public void replyBackwards(Message m) {
//        buyer.processReply(m);
//    }
//}
