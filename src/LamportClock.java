import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LamportClock {

    public int timestamp;

    LamportClock() {
        this.timestamp = 0;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public void receiveUpdate(int receiveTimestamp) {
        this.timestamp = Math.max(this.timestamp, receiveTimestamp) + 1;
    }

    public void sendTimestampUpdate(int buyerID) throws MalformedURLException {

        for(int peerID : PeerCommunication.neighborPeerIDs.get(1)) {// change maps
            if(peerID == buyerID)
                continue;

            URL url = new URL("peerID"); //change
            try{
                Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort());
                RemoteInterface remoteInterface = (RemoteInterface) registry.lookup("RemoteInterface");
                remoteInterface.sendTimeStampUpdate(this.timestamp, PeerCommunication.rolesMap.get(peerID)); // use rolesMap
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }

}
