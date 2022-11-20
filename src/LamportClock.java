import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

public class LamportClock {

    public int timestamp;

    LamportClock() {
        this.timestamp = 0;
    }

    public int getTimestamp() {
        return timestamp;
    }

    // update local clock by comparing node's timestamp and neighbour's timestamp
    public void receiveUpdate(int receiveTimestamp) {
        this.timestamp = Math.max(this.timestamp, receiveTimestamp) + 1;
    }

    // send timestamp update to all peers using RMI call
    public void sendTimestampUpdate(int buyerID) throws MalformedURLException {

        for(Map.Entry<Integer, String> entry : PeerCommunication.peerIdURLMap.entrySet()) {
            if(entry.getKey() == buyerID)
                continue;

            URL url = new URL(entry.getValue());
            try{
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendTimeStampUpdate(this.timestamp, PeerCommunication.rolesMap.get(entry.getKey()));
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

}
