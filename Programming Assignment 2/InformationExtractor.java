import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class InformationExtractor {
    private final static int headerlength = 20;
    private int messageLength;
    private byte[] data;
    private byte[] message;
    private byte[] sourcePort = new byte[2];
    private byte[] destinationPort = new byte[2];
    private byte[] sequenceNumber = new byte[4];
    private byte[] ackNumber = new byte[4];
    private byte[] flag = new byte[2];
    private int checksum;
    private int checksum_check;
    
    public InformationExtractor(DatagramPacket packet) {
        receiveMessage(packet);
    }
    
    public void receiveMessage(DatagramPacket packet) {
        data = packet.getData();
        messageLength = data.length - 20;
        message = new byte[messageLength];
        message = Arrays.copyOfRange(data, 20, data.length);
        sourcePort = Arrays.copyOfRange(data, 0, 2);
        destinationPort = Arrays.copyOfRange(data, 2, 4);
        sequenceNumber = Arrays.copyOfRange(data, 4, 8);
        ackNumber = Arrays.copyOfRange(data, 8, 12);
        flag = Arrays.copyOfRange(data, 12, 14);
        checksum = (int)ByteBuffer.wrap(Arrays.copyOfRange(data, 16, 18)).getShort();
        checksum_check = new CalculateChecksum(data).checksum();
    }
    
    public int getSequenceNumber() {
        int seq = ByteBuffer.wrap(sequenceNumber).getInt();
        return seq;
    }
    
    public String getMessage() {
        String str = new String(message);
        return str;
    }
    
    public int getAckNumber() {
        int ack = ByteBuffer.wrap(ackNumber).getInt();
        return ack;
    }
    
    public int getSourceNum() {
        int source = (int)ByteBuffer.wrap(sourcePort).getShort();
        if (source < 0){
            source = 65536 + source;
        }
        return source;
    }
    
    public int getDestinationNum() {
        int destination = (int)ByteBuffer.wrap(destinationPort).getShort();
        if (destination < 0){
            destination = 65536 + destination;
        }
        return destination;
    }
    
    public boolean checkChecksum() {
        if (checksum == checksum_check) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public int getflag() {
        int flagnum = (int)ByteBuffer.wrap(flag).getShort();
        return flagnum;
    }
    
}
