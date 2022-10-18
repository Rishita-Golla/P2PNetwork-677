import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Stack;

public interface RemoteFunctions extends Remote {
    public void testFunction() throws RemoteException;
}
