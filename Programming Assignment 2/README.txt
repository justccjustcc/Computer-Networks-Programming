CSEE 4119 Computer Networks Programming homework 2

Name: Chen Chen
UNI: cc3701



Brief Introduction:

There are a total of 5 files for Programming 2: Sender.java, Receiver.java, FileReader.java, InformationExtractor.java, CalculateChecksum.java. 

Among the five files, Sender.java and Receiver.java are the main files, representing sender and receiver. 

FileReader.java is utilized at sender’s side to retrieve
 message from origin file, separate raw message into bytes packets with TCP header. 

InformationExtractor.java is included in receiver. It can extract every part of header and message from the received packet.

CalculateChecksum.java is part of both FileReader.java and InformationExtractor.java. Just as its name indicates, it is used to compute checksum.



More Details:

(a) The TCP segment structure partly follows the one on textbook. The header is 20 bytes in length. Useful parts include:

- Source port# (2 bytes)
- Destination port# (2 bytes)
- Sequence# (4 bytes)
- Flag (ACK and FIN, both 1 bit)
- Checksum

Especially, in Flag, ACK is always set to 1, indicating it will be delivered to upper layer immediately. FIN is set to 1 when the packet is the last packet of a certain file, and equals 0 otherwise.

(b) States encountered and loss recovery mechanism

The program tries to mimic the behavior of a GBN TCP protocol. Below is what sender and receiver does when 

- Sender: Sender sends packets in the specific window and receive ACK. When a timeout happened for a packet, sender would resend the packet and all the packets afterwards inside the window. If receiving a wrong ACK, the sender simply discards it.

- Receiver: Receive packets from sender and send ACK back. All receiver does is if it receives the expected packets, it will store the packets and send ACK to sender. Otherwise, receiver does nothing but send the last ACK back to sender.

(c) Some bugs

After trials, my programs perform better when window size are 1, 2 or 3, and cannot guarantee get the right result all the time.


Some explanation:

- For the log file, the one in sender side are almost the same as the one in receiver’s side. The rows, from left to right, represent timestamp, source port #, destination port #, sequence #, ACK #, ACK flag, FIN flag (and estimated RTT for sender)


Example input:

java Receiver receive.txt 41194 127.0.0.1 41191 ReceiverlogFile.txt

java Sender trial.txt 127.0.0.1 41192 41191 SenderLogfile.txt 3





