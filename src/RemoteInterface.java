import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    public void checkOrBroadcastMessage(Message m, int peerID, String role) throws RemoteException, MalformedURLException;
    public boolean sellItem(String requestedItem) throws RemoteException;
    public void replyBackwards(Message m, String role) throws RemoteException;
}
