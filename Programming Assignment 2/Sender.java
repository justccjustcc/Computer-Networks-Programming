import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Sender {
    
    private String filename = new String(); // name of the file to send
    private InetAddress receiverIP; // receiver's IP address
    private int receiverPort; // receiver's port#
    private int receivePort; //sender's receive port#
    private String logFile = new String(); // the name of sender's logfile
    private final static int mss = 256; // the size of the data of a packet
    private int byteLength; //the total byte length of the file
    private int base = 0;
    private int nextSeq = 0;
    private int windowSize = 1; //default window size is 1
    private ArrayList<byte[]> data = new ArrayList<byte[]>();
    
    private int resendCount = 0; //count the number of resent segments
    private HashMap<Integer, Long> startTime = new HashMap<Integer, Long>(); // match sequence number with sending time
    //private HashMap<Integer, Long> endTime = new HashMap<Integer, Long>(); //match sequence number with receiving time
    private HashMap<Integer, Integer> timeOut = new  HashMap<Integer, Integer>(); //match sequence number with timeout interval for the packet
    private int estimatedRTT = 10;
    private int devRTT = 0;
    private int sampleRTT = 0;
    private int timeoutInterval = 200;
    private int totalByteSend;
    
    DatagramSocket datagramSocket = new DatagramSocket(41191);
    private ServerSocket serverSocket;
    
    public static void main(String[] args) throws IOException {
        if (args.length != 5 && args.length != 6) {
            System.out.println("Wrong format. Please re-enter");
        }
        else{
            if(args.length == 5) {
                new Sender(args[0], InetAddress.getByName(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4]);
            }
            if(args.length == 6) {
                 new Sender(args[0], InetAddress.getByName(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));
            }
        }
    }
    
    
    public Sender(String filename, InetAddress IP, int receiverPort, int receivePort, String logFile, int windowSize) throws IOException {
        this.filename = filename;
        this.receiverIP = IP;
        this.receiverPort = receiverPort;
        this.receivePort = receivePort;
        this.logFile = logFile;
        this.windowSize = windowSize;
        message();
    }
    
    public Sender(String filename, InetAddress IP, int receiverPort, int receivePort, String logFile) throws IOException {
        this.filename = filename;
        this.receiverIP = IP;
        this.receiverPort = receiverPort;
        this.receivePort = receivePort;
        this.logFile = logFile;
        message();
    }
    
    
    private void message() throws IOException {
        // read file
        FileReader fileReader = new FileReader(filename,receiverPort,receivePort);
        data = fileReader.getData();
        byteLength = fileReader.getByteLength();
        totalByteSend = byteLength;
        // sending part
        SendThread sendThread = new SendThread(datagramSocket);
        sendThread.start();
        
        // receive part
        serverSocket = new ServerSocket(receivePort);
        Socket socket = serverSocket.accept();
        ReceiveThread receiveThread = new ReceiveThread(socket);
        receiveThread.start();
    }
    
    
    public class SendThread extends Thread{
        DatagramSocket datagramSocket;
        public SendThread(DatagramSocket datagramSocket){
            this.datagramSocket = datagramSocket;
        }
        
        public void run() {
            int len = data.size();
            int end;
            while(base < len) {
                end = Math.min(base + windowSize - 1, len - 1);
                if(nextSeq <= end) {
                    sendMsg(datagramSocket,nextSeq,end); // send the No.(nextSeq-end) packet
                    nextSeq = end + 1;
                }
            go: //detect the change of nextSeq, which means to resend
                while((nextSeq > end && nextSeq > base + windowSize - 1 && nextSeq < len) || (nextSeq > end && nextSeq < base + windowSize - 1 && nextSeq >= len) ) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (base >= len) {
                        break go;
                    }
                }
                
            }
            System.out.println("Delivery completed successfully");
            System.out.println("Total bytes sent = " + totalByteSend);
            System.out.println("Segments sent = " + data.size());
            System.out.println("Segments retransmitted = " + resendCount);
            datagramSocket.close();
        }
    }
    
    private void sendMsg(DatagramSocket datagramSocket, int start, int end) {
        for (int i = start; i <= end; i++) {
            try {
                byte[] b = data.get(i);
                InetAddress receiveHost = InetAddress.getByName("localhost");
                DatagramPacket sendPkt = new DatagramPacket(b,b.length,receiveHost,receiverPort);
                datagramSocket.send(sendPkt);
                //System.out.println("send"+ i);
                startTime.put(i,System.currentTimeMillis()); //write the send time of the sequence
                timeOut.put(i, timeoutInterval);
                if (i == start) {
                    Thread.sleep(50);
                    TimeThread timeThread = new TimeThread(i, end);
                    timeThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    
    
    public class ReceiveThread extends Thread{
        Socket socket;
        public ReceiveThread(Socket socket){
            this.socket = socket;
        }
        public void run(){
            try {
                runRun(socket);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        public void runRun(Socket socket) throws FileNotFoundException {
            // set log writer
            PrintWriter logWriter;
            if (logFile == "stdout") {
                logWriter = new PrintWriter(System.out, true);
            }
            else {
                logWriter = new PrintWriter(logFile);
            }
            // the receiving part
            int len = data.size();
            while(base < len){
                try {
                    BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String str = bufferedReader.readLine();
                    int acknum = Integer.parseInt(str);
                    long receiveTime = System.currentTimeMillis(); // the time to receive a packet
                    if (acknum == base &&  receiveTime - startTime.get(acknum) < timeOut.get(acknum)) {
                        //System.out.println("receive"+str);
                        sampleRTT = (int)(receiveTime - startTime.get(acknum));
                        calculateTime(acknum); //calculate estimatedRTT, devRTT and timeoutInterval
                        writeLogFile(acknum, logWriter);
                        if (acknum == data.size() - 1) {
                            logWriter.close();
                        }
                        base ++;
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                socket.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public class TimeThread extends Thread {
        int start;
        int end;
        public TimeThread(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public void run() {
            for (int i = start; i <= end; i++) {
            hi:
                while(true) {
                    if(base <= i) {
                        int gap = (int) (System.currentTimeMillis() - startTime.get(i));
                        if (gap > timeOut.get(i)) {
                            //System.out.println(i);
                            if(i == 0) {
                                nextSeq = i; //change nextSeq means to resend the packet
                            }
                            else {
                                nextSeq = i-1;
                            }
                            
                            resendCount = resendCount + end - i +1;
                            calculateTotalByteSend(i,end);
                            break hi;
                        }
                        else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        break hi;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void calculateTime(int acknum) {
        
        estimatedRTT = (int) (0.875 * estimatedRTT + sampleRTT);
        
        devRTT = (int)(0.75 * devRTT + 0.25 * Math.abs(estimatedRTT - sampleRTT));
        timeoutInterval = estimatedRTT + 4 * devRTT;
    }
    
    
    public void writeLogFile(int ackNum, PrintWriter logWriter) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        String timeStamp = sdf.format(date); // timestamp
        String source = Integer.toString(receivePort);
        String destination = Integer.toString(receiverPort);
        String sequence = Integer.toString(ackNum);
        String acknum = sequence;
        String estRTT = Integer.toString(estimatedRTT);
        
        String ackFlag = null;
        String finFlag = null;
        if (ackNum == data.size() - 1) {
            ackFlag = "1";
            finFlag = "1";
        }
        else {
            ackFlag = "1";
            finFlag = "0";
        }
        String log = timeStamp + " " + source + " " + destination + " " + sequence + " " + acknum+ " " + ackFlag+ " " + finFlag + " " + estRTT;
        logWriter.println(log);
    }
    
    public void calculateTotalByteSend (int start, int end) {
        for (int i = start; i <= end; i ++) {
            totalByteSend = totalByteSend + data.get(i).length;
        }
    }
    
}
