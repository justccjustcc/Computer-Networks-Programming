

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class bfclient {
    
    private static int localport;
    private static int timeout;
    private static long lastTime; //the last sending activity time
    private static String ipAddress; //remember to change in the final version!!!!!!!!!
    private static String identification;
    private DatagramSocket datagramSocket; //the listening socket
    private DatagramSocket dSkt; // the sending socket
    
    private List<String> neighbors = new ArrayList<String>();
    private Map<String, Double> neighbor_cost = new ConcurrentHashMap<String, Double>();//neighbors and the cost of the path
    private Map<String, Double> neighbor_cost_store = new ConcurrentHashMap<String, Double>();//LINKDOWN storage
    private Map<String, Map<String, Double>> neighbor_DV = new ConcurrentHashMap<String, Map<String, Double>>(); //neighbors' DV
    
    private Map<String, Long> lastAct = new ConcurrentHashMap<String, Long>();//neighbors' timeout
    
    private List<String> destinations = new ArrayList<String>();
    private Map<String, Double> dest_cost = new ConcurrentHashMap<String, Double> ();//all the clients and the cost to it
    private Map<String, String> dest_link = new ConcurrentHashMap<String, String> ();//the link nodes to other clients
    
    ListenThread listenThread;
    
    private boolean start_stop = true;
    
    public static void main(String[] args) throws UnknownHostException{
        if ((args.length - 2) % 3 != 0 || args.length <= 2) {
            System.out.println("The input is not the right format, please try again");
        }
        else {
            localport = Integer.parseInt(args[0]);
            timeout= Integer.parseInt(args[1]);
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            String[] neighborInfo = new String[args.length - 2];
            System.arraycopy(args, 2, neighborInfo, 0, neighborInfo.length);
            new bfclient(localport, timeout, neighborInfo);
        }
    }
    
    
    public bfclient(int localport, int timeout, String[] neighborInfo) {
        
        try {
            // initialization of listening and sending sockets
            datagramSocket = new DatagramSocket(localport); //listening socket
            dSkt = new DatagramSocket(localport + 1); //sending socket;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //System.out.println("localport: "+ localport);
        //start a UDP to receive distance vector
        listenThread = new ListenThread(datagramSocket);
        new Thread(listenThread).start();
        //Start time thread
        TimeThread timeThread = new TimeThread();
        new Thread(timeThread).start();
        //Start command thread
        CommandThread commandThread = new CommandThread();
        new Thread(commandThread).start();
        
        addNeighborInfo(neighborInfo);
        
    }
    
    //add neighbor information, cost to neighbors of the client and all the clients , initialization step
    private void addNeighborInfo(String[] neighborInfo) {
        int numOfNeighbor = neighborInfo.length / 3;
        String neighborName;
        int cost;
        //add "myself" into the DV, but this part maybe unnecessary
        identification = ipAddress + ":" + Integer.toString(localport);
        //System.out.println("identification: " + identification);
        destinations.add(identification);
        dest_cost.put(identification, (double)0);
        dest_link.put(identification, identification);
        
        for (int i = 0; i < numOfNeighbor; i++) {
            neighborName = neighborInfo[3*i] + ":" + neighborInfo[3*i + 1];
            //System.out.println("add new neighbor: " + neighborName);
            cost = Integer.parseInt(neighborInfo[3*i + 2]);
            //update neighbor information
            neighbors.add(neighborName);
            neighbor_cost.put(neighborName, (double)cost);
            //update DV information
            destinations.add(neighborName);
            dest_cost.put(neighborName, (double)cost);
            dest_link.put(neighborName, neighborName);
        }
        SendThread sendThread = new SendThread();
        new Thread(sendThread).start();
        lastTime = System.currentTimeMillis();
        
    }
    
    
    // the listen-only UDP
    public class ListenThread implements Runnable {
        DatagramSocket datagramSocket;
        DatagramPacket datagramPacket;
        
        public ListenThread(DatagramSocket datagramSocket) {
            this.datagramSocket = datagramSocket;
        }
        public void run() {
            while(start_stop) {
                byte[] buffer = new byte[256];
                datagramPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    datagramSocket.receive(datagramPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                translateDV(datagramPacket);
            }
        }
    }
    
    //translate the received message into information and update DV
    private void translateDV(DatagramPacket datagramPacket) {
        //raw data
        byte[] data; //translate the received packet into byte data
        byte[] data1_IP; //extract IP address
        byte[] data2_port;//extract port number
        byte[] data3_distance;//extract the distance
        //translated information
        String IP;
        String port;
        String IP_port; // identification in format of IP:port
        double distance;//the shortest distance from this client to the destination
        String link = null; // the identification of the sender
        
        int numVector; //the number of the clients whose distance to a client was sent
        int length = datagramPacket.getLength();
        //System.out.println(length);
        data = datagramPacket.getData();
        boolean change = false; //indicate if there is change to current DV of "me"
        
        if (length % 16 == 0) {
            numVector = length / 16 - 1;
            //System.out.println("The number of vector: " + numVector);
            Map<String, Double> DV = new HashMap<String, Double>();//generate sender's DV entry
            for (int i = 0; i<= numVector; i++) {
                //change = false;
                if (i == 0) {
                    //the first 16 bytes are the identification and cost of the sender
                    data1_IP = Arrays.copyOfRange(data, 0, 4);
                    data2_port = Arrays.copyOfRange(data, 4, 8);
                    data3_distance = Arrays.copyOfRange(data, 8, 16);
                    //ip translate
                    int[] ip_int = new int[4];
                    for (int j = 0; j < 4; j++) {
                        ip_int[j] = (int) data1_IP[j] + 128;
                    }
                    IP = Integer.toString(ip_int[0]) + "." + Integer.toString(ip_int[1]) + "." + Integer.toString(ip_int[2]) + "." + Integer.toString(ip_int[3]);
                    port = Integer.toString(ByteBuffer.wrap(data2_port).getInt());
                    IP_port = IP + ":" + port; //the identity of the client who send the message
                    link = IP_port;
                    distance = ByteBuffer.wrap(data3_distance).getDouble();
                    
                    if (distance == Double.MAX_VALUE) {
                        //LINKDOWN message
                        neighbor_cost_store.put(IP_port, neighbor_cost.get(IP_port));
                        neighbors.remove(IP_port);
                        neighbor_cost.remove(IP_port);
                        dest_cost.put(IP_port, Double.MAX_VALUE);
                        selfUpdateDV();
                    }
                    else if (distance == 0) {
                        //LINKUP message
                        neighbors.add(IP_port);
                        neighbor_cost.put(IP_port, neighbor_cost_store.get(IP_port));
                        neighbor_cost_store.remove(IP_port);
                        dest_cost.put(IP_port, neighbor_cost.get(IP_port));
                        selfUpdateDV();
                    }
                    else {
                        //ROUTE UPDATE message
                        
                        //System.out.println("from: " + IP_port);
                        //System.out.println(" ");
                        
                        // update neighbors and destinations information
                        if (neighbors.contains(IP_port) == false) {
                            neighbors.add(IP_port);
                            neighbor_cost.put(IP_port, distance);
                            if (destinations.contains(IP_port) == false) {
                                destinations.add(IP_port);
                                dest_cost.put(IP_port, distance);
                                dest_link.put(IP_port, IP_port);
                                change = true;
                            }
                            else {
                                if (dest_cost.get(IP_port) > distance)
                                    dest_cost.put(IP_port, distance);
                                dest_link.put(IP_port, IP_port);
                                change = true;
                            }
                        }
                        else {
                            //update cost of a neighbor and the DV of the neighbor
                            if (distance != neighbor_cost.get(IP_port)) {
                                neighbor_cost.put(IP_port, distance);
                                if (dest_cost.get(IP_port) > distance) {
                                    dest_cost.put(IP_port, distance);
                                    dest_link.put(IP_port, IP_port);
                                    change = true;
                                }
                            }
                            
                        }
                        lastAct.put(IP_port, System.currentTimeMillis());
                    }
                    
                }
                else {
                    // other bytes are the DV(not the information of the sender)
                    data1_IP = Arrays.copyOfRange(data, i * 16, i * 16 + 4);
                    data2_port = Arrays.copyOfRange(data, i * 16 + 4, i * 16 + 8);
                    data3_distance = Arrays.copyOfRange(data, i * 16 + 8, i * 16 + 16);
                    //ip translate
                    int[] ip_int = new int[4];
                    for (int j = 0; j < 4; j++) {
                        ip_int[j] = (int) data1_IP[j] + 128;
                    }
                    IP = Integer.toString(ip_int[0]) + "." + Integer.toString(ip_int[1]) + "." + Integer.toString(ip_int[2]) + "." + Integer.toString(ip_int[3]);
                    port = Integer.toString(ByteBuffer.wrap(data2_port).getInt());
                    IP_port = IP + ":" + port; //the identity of one of the nodes that are the neighbors of the sender
                    distance = ByteBuffer.wrap(data3_distance).getDouble();
                    DV.put(IP_port, distance); //update neighbor(the send)'s DV
                    if (destinations.contains(IP_port) == false) {
                        destinations.add(IP_port);
                        double distance_new = dest_cost.get(link) + distance;
                        dest_cost.put(IP_port, distance_new);
                        dest_link.put(IP_port, link);
                        change = true;
                    }
                    else {
                        //update the DV of "me"
                        if (dest_cost.get(IP_port) > neighbor_cost.get(link) + distance) {
                            dest_cost.put(IP_port, dest_cost.get(link) + distance);
                            dest_link.put(IP_port, link);
                            change = true;
                        }
                    }
                }
            }
            if (numVector > 0) {
                neighbor_DV.put(link, DV); //save neighbor's DV
            }
            
            if (change == true) {
                //start a UDP to send distance vector when the DV has changed
                SendThread sendThread = new SendThread();
                new Thread(sendThread).start(); //send updated DV to neighbors
            }
        }
        else {
            System.out.println("The DV received is not in right format!");
        }
    }
    
    
    public class SendThread implements Runnable {
        DatagramPacket dPkt;
        byte[] message; // ROUTE UPDATE message
        InetAddress ip;
        int port;
        int type; //indicate if send link down or link up message
        public SendThread() {
            //ROUTE UPDATE message
            type = 0;
        }
        public SendThread(int type, byte[] message, String ipAdd, String portNum) {
            //LINK DOWN OR LINK UP message
            this.type = type;
            this.message = message;
            try {
                ip = InetAddress.getByName(ipAdd);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            port = Integer.parseInt(portNum);
        }
        
        public void run() {
            // type = 0 means it is the ROUTE UPDATE message
            if (type == 0) {
                // generate the message byte array
                message = messageGenerator();
                //send message to neighbors
                Iterator<String> it = neighbor_cost.keySet().iterator();
                while(it.hasNext()) {
                    String iter = it.next();
                    if (iter != identification) {
                        // check whether the neighbor has disconnected
                        if ((!lastAct.containsKey(iter)) || (lastAct.containsKey(iter) &&
                                                             (int)(System.currentTimeMillis() - lastAct.get(iter)) < 3000 * timeout)) {
                            String[] key_part = iter.split(":");
                            try {
                                ip = InetAddress.getByName(key_part[0]);
                                //System.out.println("send to Ip: " + ip);
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                            port = Integer.parseInt(key_part[1]);
                            //System.out.println("Send to port: " + port);
                            //System.out.println(" ");
                            //add cost to message
                            byte[] destination = ByteBuffer.allocate(8).putDouble(neighbor_cost.get(iter)).array();
                            System.arraycopy(destination, 0, message, 8, destination.length);
                            dPkt = new DatagramPacket(message,message.length,ip,port);
                            try {
                                dSkt.send(dPkt);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            neighbor_cost.put(iter, Double.MAX_VALUE); // set the cost to a neighbor to the max int
                        }
                        lastTime = System.currentTimeMillis();
                    }
                }
            }
            // type = 1 means it is a LINKDOWN or LINKUP message
            if (type == 1) {
                dPkt = new DatagramPacket(message,message.length,ip,port);
                try {
                    dSkt.send(dPkt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    
    //generate the information of a sender whose
    public byte[] messageGenerator() {
        String[] key = new String[2]; //separate ip:port into ip and port
        String[] ip_string = new String[4]; // separate ip into 4 parts
        int[] ip_int = new int[4]; //save the 4 parts of ip address
        byte[] port; //save the 4 bytes of port number
        byte[] distance;
        byte[] message = new byte[256];
        byte[] real_message;
        
        //generate information of the sender
        key = identification.split(":");
        ip_string =key[0].split("[.]");
        for (int i = 0; i < 4 ; i++) {
            ip_int[i] = Integer.parseInt(ip_string[i]) - 128; //if not minus 128, the integer will be out of range
            message[i] = (byte)ip_int[i]; //
        }
        port = ByteBuffer.allocate(4).putInt(localport).array();
        System.arraycopy(port, 0, message, 4, port.length);
        
        // generate information of DV
        Iterator<String> it = dest_cost.keySet().iterator();
        int index = 0; //indicator of the position in message
        while(it.hasNext()) {
            String iter = it.next();
            if (iter != identification) {
                // add ip
                index ++;
                key = iter.split(":");
                //				System.out.println("DV ip: " + key[0]);
                //				System.out.println("DV port: " + key[1]);
                ip_string = key[0].split("[.]");
                for (int i = 0; i < 4; i++) {
                    ip_int[i] = Integer.parseInt(ip_string[i]) - 128;
                    message[16 * index + i] = (byte) ip_int[i];
                }
                // add port
                port = ByteBuffer.allocate(4).putInt(Integer.parseInt(key[1])).array();
                System.arraycopy(port, 0, message, 16 * index + 4, port.length);
                // add distance
                distance = ByteBuffer.allocate(8).putDouble(dest_cost.get(iter)).array();
                System.arraycopy(distance, 0, message, 16 * index + 8, distance.length);
            }
        }
        real_message = new byte[(index + 1) * 16];
        System.arraycopy(message, 0, real_message, 0, real_message.length);
        return real_message;
    }
    
    public class TimeThread implements Runnable {
        public void run() {
            while(start_stop) {
                try {
                    Thread.sleep(timeout * 1000); //every timeout check whether there is time out
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if((int)(System.currentTimeMillis() - lastTime) > timeout) {
                    SendThread sendThread = new SendThread();
                    new Thread(sendThread).start();
                }
            }
        }
    }
    
    public class CommandThread implements Runnable {
        Scanner sc;
        String command;
        public void run() {
            while(start_stop) {
                sc = new Scanner(System.in);
                command = sc.nextLine();
                //command SHOWRT:
                if (command.equals("SHOWRT")) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    System.out.println(dateFormat.format(date) + " Distance Vector list is:");
                    Iterator<String> it = dest_cost.keySet().iterator();
                    while(it.hasNext()) {
                        String iter = it.next();
                        if (iter != identification) {
                            System.out.println("Destination = " + iter + ", Cost = " + dest_cost.get(iter)
                                               + ", Link = (" + dest_link.get(iter) + ")");
                        }
                    }
                }
                //command LINKDOWN:
                else if (command.contains("LINKDOWN")) {
                    String[] command_split = command.split(" ");
                    byte[] message = new byte[16];// the message sent
                    if (command_split.length == 3 && command_split[0].equals("LINKDOWN")) {
                        String id = command_split[1] + ":" + command_split[2];
                        if (neighbor_cost.containsKey(id)) {
                            //remove the entry from neighbor list and save it to another map
                            neighbor_cost_store.put(id, neighbor_cost.get(id));
                            neighbors.remove(id);
                            neighbor_cost.remove(id);
                            //generate a LINKDOWN messsage and send
                            //ip part
                            String[] key = identification.split(":");
                            String[] ip_string =key[0].split("[.]");
                            int[] ip_int = new int[4];
                            for (int i = 0; i < 4 ; i++) {
                                ip_int[i] = Integer.parseInt(ip_string[i]) - 128; //if not minus 128, the integer will be out of range
                                message[i] = (byte)ip_int[i]; //
                            }
                            //port part
                            byte[] port = ByteBuffer.allocate(4).putInt(localport).array();
                            System.arraycopy(port, 0, message, 4, port.length);
                            //cost part
                            byte[] cost = ByteBuffer.allocate(8).putDouble(Double.MAX_VALUE).array();
                            System.arraycopy(cost, 0, message, 8, cost.length);
                            //send message
                            SendThread sendThread = new SendThread(1, message, command_split[1], command_split[2]);
                            new Thread(sendThread).start();
                            
                            dest_cost.put(id, Double.MAX_VALUE);
                            selfUpdateDV();
                        }
                        else {
                            System.out.println("LINKDOWN command: This link does not exist");
                        }
                    }
                    else
                    {
                        System.out.println("Invalid command");
                    }
                }
                //command LINKUP:
                else if (command.contains("LINKUP")) {
                    String[] command_split = command.split(" ");
                    byte[] message = new byte[16];// the message sent
                    if (command_split.length == 3 && command_split[0].equals("LINKUP")) {
                        String id = command_split[1] + ":" + command_split[2];
                        if (neighbor_cost_store.containsKey(id)) {
                            neighbors.add(id);
                            neighbor_cost.put(id, neighbor_cost_store.get(id));
                            neighbor_cost_store.remove(id);
                            //generate LINKUP message
                            //ip part
                            String[] key = identification.split(":");
                            String[] ip_string =key[0].split("[.]");
                            int[] ip_int = new int[4];
                            for (int i = 0; i < 4 ; i++) {
                                ip_int[i] = Integer.parseInt(ip_string[i]) - 128; //if not minus 128, the integer will be out of range
                                message[i] = (byte)ip_int[i]; //
                            }
                            //port part
                            byte[] port = ByteBuffer.allocate(4).putInt(localport).array();
                            System.arraycopy(port, 0, message, 4, port.length);
                            //cost part
                            byte[] cost = ByteBuffer.allocate(8).putDouble((double)0).array(); //set cost 0 to indicate it is a LINKUP message
                            System.arraycopy(cost, 0, message, 8, cost.length);
                            //send message
                            SendThread sendThread = new SendThread(1, message, command_split[1], command_split[2]);
                            new Thread(sendThread).start();
                            
                            dest_cost.put(id, neighbor_cost.get(id));
                            selfUpdateDV();
                            
                        }
                        else {
                            System.out.println("LINKUP command: This link does not exist before and cannot be restored");
                        }
                    }
                    else {
                        System.out.println("Invalid command");
                    }
                }
                else if (command.equals("CLOSE")) {
                    neighbors.clear();
                    neighbor_cost.clear();
                    neighbor_cost_store.clear();
                    neighbor_DV.clear();
                    destinations.clear();
                    dest_cost.clear();
                    dest_link.clear();
                    start_stop = false;
                    
                }
                else {
                    System.out.println("Invalid command");
                }
            }
            
            
        }
    }
    
    
    public void selfUpdateDV() {
        boolean change = false;
        String link = null;
        Iterator<String> it1 = dest_cost.keySet().iterator(); //destination iterator
        while(it1.hasNext()) {
            String dest = it1.next();
            if (dest != identification) {
                double distanceDV = Double.MAX_VALUE;
                Iterator<String> it2 = neighbor_cost.keySet().iterator(); //neighbor iterator
                while(it2.hasNext()) {
                    String neighbor = it2.next();
                    if (neighbor.equals(dest)) {
                        if (neighbor_cost.get(neighbor) < distanceDV) {
                            distanceDV = neighbor_cost.get(neighbor);
                            link = neighbor;
                        }
                    }
                    else {
                        //						System.out.println(neighbor);
                        //						System.out.println(neighbor_cost.get(neighbor));
                        //						System.out.println(neighbor_DV.get(neighbor));
                        //						System.out.println(neighbor_DV.get(neighbor).get(dest));
                        //						System.out.println(distanceDV);					
                        if (neighbor_cost.get(neighbor) + neighbor_DV.get(neighbor).get(dest) < distanceDV) {
                            distanceDV = neighbor_cost.get(neighbor) + neighbor_DV.get(neighbor).get(dest);
                            link = neighbor;
                        }
                    }
                    
                }
                if (distanceDV < dest_cost.get(dest)) {
                    dest_cost.put(dest, distanceDV);
                    dest_link.put(dest, link);
                    change = true;
                }
            }
            
        }
        if (change == true) {
            SendThread sendThread = new SendThread();
            new Thread(sendThread).start();
        }
    }
    
}










