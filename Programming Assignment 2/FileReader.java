import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class FileReader {
    private final static int mss = 256; // the size of the data of a packet
    private ArrayList<byte[]> data = new ArrayList<byte[]>();
    private String fileName;
    private int lengthInByte;
    private int receiverport;
    private int receiveport;
    //store the segments of byte data that is separated from the file
    
    public FileReader(String fileName, int receiverport, int receiveport) throws IOException {
        this.fileName = fileName;
        this.receiverport = receiverport;
        this.receiveport = receiveport;
        doFileReader();
    }
    
    
    
    public void doFileReader() throws IOException {
        FileInputStream inputStream = null;
        
        // read the file
        File file = new File(fileName);
        lengthInByte = (int) file.length(); //estimate the number of bytes in the file
        try {
            inputStream = new FileInputStream(file);
            
        } catch (FileNotFoundException e) {
            // file not found warning
            System.out.println("File not found!");
        }
        
        // calculate the number of packets needed
        int numberOfPacket;// the number of packets
        if (lengthInByte % mss == 0) {
            numberOfPacket = lengthInByte / mss;
        }
        else {
            numberOfPacket = (lengthInByte / mss) + 1;
        }
        System.out.println(numberOfPacket);
        
        // read the whole file into a byte array
        byte[] rawdata = new byte[lengthInByte];
        inputStream.read(rawdata, 0, lengthInByte);
        inputStream.close();
        
        // cut the file into byte[] segments
        for (int i=0; i<numberOfPacket; i++) {
            if (i != numberOfPacket - 1) {
                byte[] newData = new byte[mss + 20];
                System.arraycopy(rawdata, i*mss, newData, 20, mss);
                //add header except for checksum
                newData = addHeader(newData, i);
                // add checksum into header
                int checkSum = new CalculateChecksum(newData).checksum();
                byte[] checksum = ByteBuffer.allocate(2).putShort((short)checkSum).array();
                System.arraycopy(checksum, 0, newData, 16, checksum.length);
                data.add(newData);
            }
            else {
                // the last segment with different length
                byte[] newData = new byte[(lengthInByte % mss) + 20];
                System.arraycopy(rawdata, i*mss, newData, 20, (lengthInByte % mss));
                //add header except for checksum
                newData = addHeader(newData, i, 1);
                // add checksum into header
                int checkSum = new CalculateChecksum(newData).checksum();
                byte[] checksum = ByteBuffer.allocate(2).putShort((short)checkSum).array();
                System.arraycopy(checksum, 0, newData, 16, checksum.length);
                data.add(newData);
            }
        }
        inputStream.close();
        
    }
    
    
    
    
    private byte[] addHeader(byte[] data, int seq) {
        //add header information ahead of the packet except for the last packet
        short receiverPort = (short) receiverport; // receiver's port#
        short receivePort = (short) receiveport; //sender's receive port#
        byte[] sourcePort = ByteBuffer.allocate(2).putShort(receivePort).array();
        byte[] destPort = ByteBuffer.allocate(2).putShort(receiverPort).array();
        byte[] sequence = ByteBuffer.allocate(4).putInt(seq).array();
        byte[] acknowledge = ByteBuffer.allocate(4).putInt(0).array();
        byte[] flag = ByteBuffer.allocate(2).putShort((short)16).array();
        byte[] receiveWindow = ByteBuffer.allocate(2).putShort((short)0).array();
        byte[] urgent = ByteBuffer.allocate(2).putShort((short)0).array();
        System.arraycopy(sourcePort, 0, data, 0, sourcePort.length);
        System.arraycopy(destPort, 0, data, 2, destPort.length);
        System.arraycopy(sequence, 0, data, 4, sequence.length);
        System.arraycopy(acknowledge, 0, data, 8, acknowledge.length);
        System.arraycopy(flag, 0, data, 12, flag.length);
        System.arraycopy(receiveWindow, 0, data, 14, receiveWindow.length);
        System.arraycopy(urgent, 0, data, 18, urgent.length);
        return data;
    }
    
    private byte[] addHeader(byte[] data, int seq, int index) {
        //add header information ahead of the last packets
        short receiverPort = (short) receiverport; // receiver's port#
        short receivePort = (short) receiveport; //sender's receive port#
        byte[] sourcePort = ByteBuffer.allocate(2).putShort(receivePort).array();
        byte[] destPort = ByteBuffer.allocate(2).putShort(receiverPort).array();
        byte[] sequence = ByteBuffer.allocate(4).putInt(seq).array();
        byte[] acknowledge = ByteBuffer.allocate(4).putInt(0).array();
        byte[] flag = ByteBuffer.allocate(2).putShort((short)17).array();
        byte[] receiveWindow = ByteBuffer.allocate(2).putShort((short)0).array();
        byte[] urgent = ByteBuffer.allocate(2).putShort((short)0).array();
        System.arraycopy(sourcePort, 0, data, 0, sourcePort.length);
        System.arraycopy(destPort, 0, data, 2, destPort.length);
        System.arraycopy(sequence, 0, data, 4, sequence.length);
        System.arraycopy(acknowledge, 0, data, 8, acknowledge.length);
        System.arraycopy(flag, 0, data, 12, flag.length);
        System.arraycopy(receiveWindow, 0, data, 14, receiveWindow.length);
        System.arraycopy(urgent, 0, data, 18, urgent.length);
        return data;
    }
    
    
    public ArrayList<byte[]> getData() {
        return data;
    }
    
    public int getByteLength() {
        return lengthInByte;
    }
    
    
}
