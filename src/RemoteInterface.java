import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    public void checkOrBroadcastMessage(Message m, int peerID, String role) throws RemoteException, MalformedURLException;
    public boolean sellItem(String requestedItem, String role) throws RemoteException;
    public void replyBackwards(Message m, String role) throws RemoteException;
    public void sendTimeStampUpdate(int timestamp, String role) throws RemoteException;
    public void sendLeaderElectionMsg(ElectionMessage message, int nodeID) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;
    public void sendTransactionAck(String role, boolean ack, int income) throws RemoteException;

    void sendLeaderIDBackwards(ElectionMessage message, int leaderID) throws RemoteException;
    public boolean addRequestToQueue(Message message) throws RemoteException, MalformedURLException;
}
