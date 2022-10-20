import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class NodeLogger extends Formatter {

    Integer peerID;

    public NodeLogger(Integer peerID) {
        this.peerID = peerID;
    }

    @Override
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        sb.append("Logging for NodeID: "+peerID + " ");
        sb.append(format(record) + "\n");
        //sb.append("\n");
        return sb.toString();
    }
}
