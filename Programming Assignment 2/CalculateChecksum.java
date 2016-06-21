import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CalculateChecksum {
    private byte[] data;
    public CalculateChecksum(byte[] data) {
        this.data = data;
    }
    public int checksum() {
        int length = data.length;
        byte[] otherData = new byte[length - 2]; //byte array without the checksum part
        System.arraycopy(data, 0, otherData, 0, 16);
        System.arraycopy(data, 18, otherData, 16, length - 18);
        ArrayList<Integer> dataByte = new ArrayList<Integer>(); // store all the 2-bytes arrays in int
        int index;
        if (otherData.length % 2 == 1) {
            index = (otherData.length / 2) + 1;
        }
        else {
            index = otherData.length / 2;
        }
        // separate the packet in byte[] into several byte 2-bits arrays, and convert into int
        for (int i = 0; i < index; i++) {
            if (i != index - 1 || otherData.length % 2 != 1) {
                byte[] b = new byte[2];
                System.arraycopy(otherData, i*2, b, 0, 2);
                dataByte.add((int)ByteBuffer.wrap(b).getShort());
            }
            else {
                byte[] b = new byte[2];
                System.arraycopy(otherData, i*2, b, 0, 1);
                dataByte.add((int)ByteBuffer.wrap(b).getShort());
            }
        }
        int result = dataByte.get(0);
        for (int i = 1; i < index; i++) {
            result = dataByte.get(i) ^ result;
        }
        return result;
    }
}

