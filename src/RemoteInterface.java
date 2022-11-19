import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    void checkOrBroadcastMessage(Message m, int peerID, String role) throws RemoteException, MalformedURLException;
    boolean sellItem(String requestedItem, String role) throws RemoteException;
    void replyBackwards(Message m, String role) throws RemoteException;
    void sendTimeStampUpdate(int timestamp, String role) throws RemoteException;
    void sendLeaderElectionMsg(ElectionMessage message, int nodeID) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;
    void sendTransactionAck(String role, boolean ack, int income) throws RemoteException;

    void sendLeaderIDBackwards(ElectionMessage message, int leaderID) throws RemoteException;
    void addRequestToQueue(Message message) throws RemoteException;
}
