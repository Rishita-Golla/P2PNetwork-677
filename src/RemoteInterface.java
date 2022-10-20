import java.net.MalformedURLException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteInterface extends Remote {
    public void checkOrBroadcastMessage(Message m, String productName, int ID) throws RemoteException, MalformedURLException; //replace with M? //In implementation it should call processMessageForward
    public boolean sellItem(String requestedItem) throws RemoteException;
    public void replyBackwards(Message m) throws RemoteException;
}
