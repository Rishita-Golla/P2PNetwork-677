import java.rmi.Remote;
import java.util.List;

public interface RemoteInterface extends Remote {
    public void startCommunication(String productName, List<Integer> path);
    public boolean sellItem(String requestedItem);
}
