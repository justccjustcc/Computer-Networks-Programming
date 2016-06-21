CSEE4119 Computer Networks PA3

Name: Chen Chen
UNI: cc3701


Brief Introduction: 

There is only one file for PA3, bfclient.java. This file simulates the distance-vector routing algorithm that applied in routing.


Details:

(a) Functions

 - When the cost of a client change, the client would update its distance vector(DV) to all the destinations known for the client

 - When the distance vector of a client changes, it send ROUTE UPDATE MESSAGE to all its neighbors.

 - Each client contains the cost to every neighbor and DV to all the destinations

 - Four commands are supoorted:
    * SHOWRT: display the current routing table of the client in format
              <Current Time>Distance vector list is:              Destination = <ip address:port number>, Cost = <double>, Link = <(ip address:port number)>
    * LINKDOWN {ip_address port}: destroy and existing link and send a LINKDOWN MESSAGE to the certain neighbor
    * LINKUP {ip_address port}: restore a pre-destroyed link and send a LINKUP MESSAGE to the certain former neighbor
    * CLOSE: Shut down a client, which has the same function as Ctrl+C

(b) Message information

Here are three types of message send/receive in the process, that is, ROUTE UPDATE MESSAGE, LINKDOWN MESSAGE and LINKUP MESSAGE. Since all the clients communicate through UDP, the messages are in byte array format and capsuled in datagram packets. The format of these messages are listed below:

 All messages are inserted in the form of entries, and each entry is in the format {IP address, port number, cost}. IP address and port number are both string and each consists of four bytes, and cost is a double(eight bytes) value. The three types of messages have some important distinctions.

 - ROUTE UPDATE MESSAGE: The first entry is sender’s IP address, port number and its cost to the receiver. All the other entries are the sender’s DV to other destinations in the network, which means the cost to other destinations  in the identification of {IP address:port number}.

 - LINKDOWN MESSAGE: The message only contains one entry with the cost value set to infinity. Receiver identify a LINKDOWN MESSAGE based on the information that it has one entry and the cost is infinity.

 - LINKUP MESSAGE: Similar to LINKDOWN MESSAGE, LINKUP MESSAGE also contains one entry but set the cost to 0.



(c) Socket information

Each client uses two port. One of them is the listening port,which is also the identification port, and the other port is used to send messages, whose port number is the port number of the listening port plus 1. Therefore, when running, do not choose no two clients that are in the same machine and have adjacent identification port numbers.



(d) Running Sample

>make

Client A
>java bfclient 4115 3 127.0.0.1 4250 11.0

Client B
>java bfclient 4250 3 127.0.0.1 4115 7.0

Client A
SHOWRT
12/13/2015 02:00:06 Distance vector list is:
Destination = 127.0.0.1:41250, Cost = 7.0, Link = (127.0.0.1)

CLient B
CLOSE

Client A
CLOSE

