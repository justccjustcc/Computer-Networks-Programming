import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;



public class Receiver {
    private String fileName;
    private int listenPort;
    private InetAddress IP;
    private int receivePort;
    private String logFile;
    private final static int mss = 256; // the size of the data of a packet
    private int base = 0;
    
    public static void main(String[] args) throws IOException {
        if (args.length != 5){
            System.out.println("Wrong format. Please re-enter.");
        }
        else {
           
            
            new Receiver(args[0], Integer.parseInt(args[1]), InetAddress.getByName(args[2]), Integer.parseInt(args[3]), args[4]);
        }
    }
    
    
    public Receiver(String fileName, int listenPort, InetAddress IP, int receivePort, String logFile) throws UnknownHostException, IOException {
        this.fileName = fileName;
        this.listenPort = listenPort;
        this.IP = IP;
        this.receivePort = receivePort;
        this.logFile = logFile;
        message();
    }
    
    
    
    
    public void message() throws UnknownHostException, IOException {
        //initiate receive socket
        DatagramSocket skt = new DatagramSocket(listenPort);
        //initiate send socket
        Socket socket = null;
        
        receiveMsg(skt,socket);
        System.out.println("Delivery completed successfully");
    }
    
    
    public void receiveMsg(DatagramSocket skt, Socket socket) throws IOException {
        int seqnum;
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(fileName); //write message into file
        PrintWriter logWriter; //writer for log file
        if (logFile == "stdout") {
            logWriter = new PrintWriter(System.out, true);
        }
        else {
            logWriter = new PrintWriter(logFile);
        }
        
        int acknum = -1;
        boolean b = true;
        while(b == true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // receive a packet
            byte[] buffer = new byte[mss + 20];
            DatagramPacket receivePkt = new DatagramPacket(buffer,buffer.length);
            skt.receive(receivePkt);
            //extract data from packet
            InformationExtractor  informationExtractor= new InformationExtractor(receivePkt);
            seqnum = informationExtractor.getSequenceNumber();
            
            if (seqnum == base && informationExtractor.checkChecksum() == true) {
                //if it is the expected packet, write a message into the appointed file
                //System.out.println("receive"+seqnum);
                String message = informationExtractor.getMessage();
                //System.out.println(message);
                writer.print(message);
                writeLogFile(informationExtractor,logWriter);// also write the log file at the same time
                acknum = seqnum;
                if (informationExtractor.getflag() == 17) {
                    writer.close();
                    b = false;
                }
                base ++;
                // after receiving a packet and write it into a file, send ack back to sender
                if (base == 1) {
                    socket = new Socket("localhost",receivePort);
                }
                new SendThread(socket,acknum).start();
                
            }
            else {
                //if it is not the expected packet
                new SendThread(socket,acknum).start();
                
            }
            
        }
        
    }
    
    
    public void writeLogFile(InformationExtractor  informationExtractor, PrintWriter logWriter) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
        String timeStamp = sdf.format(date); // timestamp
        String source = Integer.toString(informationExtractor.getSourceNum());
        String destination = Integer.toString(informationExtractor.getDestinationNum());
        String sequence = Integer.toString(informationExtractor.getSequenceNumber());
        String acknum = sequence;
        String ackFlag = null;
        String finFlag = null;
        if (informationExtractor.getflag() == 17) {
            ackFlag = "1";
            finFlag = "1";
        }
        if (informationExtractor.getflag() == 16) {
            ackFlag = "1";
            finFlag = "0";
        }
        String log = timeStamp + " " + source + " " + destination + " " + sequence + " " + acknum+ " " + ackFlag+ " " + finFlag;
        logWriter.println(log);
        if (informationExtractor.getflag() == 17) {
            logWriter.close();
        }
        
    }
    
    
    public class SendThread extends Thread {
        Socket socket;
        int acknum;
        public SendThread(Socket socket, int acknum) {
            this.socket = socket;
            this.acknum = acknum;
        }
        
        
        public void run() {
            String msg = Integer.toString(acknum);
            try {
                //System.out.println("send" +acknum);
                if (acknum != -1) {
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
                    printWriter.println(msg);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
