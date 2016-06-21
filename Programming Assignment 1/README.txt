CSEE 4119 Computer Networks Programming homework 1

Name: Chen Chen


a. Code Description: 
My code consist of two .java files (Client.java and Server.Java).
It is a relatively comprehensive client-server application, which can realize the functions of sending message to other online users or certain users, finding other online users, displaying the users who were online in the last a period of time, logging in and logging out.

In Client.java, two thread are created to separately receive and send messages.

In Server.java, several clients can work at the same time. The information of clients are mainly stored in List or HashMap.



b. Development Environment:

Development Software:
Eclipse IDE for Java Developers
Luna Service Release 2 (4.4.2)

My laptop:
MacBook Pro (13-inch, Late 2011)
Operating System: OS X Yosemite 10.10.5
Processor: 2.8 GHz Intel Core i7
Memory: 4 GB 1333 MHz DDR3
Graphics: Intel HD Graphics 3000 384 MB



c. How to run my code

1) type “make” to compile or using command javac Client.java and javac Server.java to compile;
2) run the code in the format below:
   - java Server <port number>
   - java Client <server IP> <port number>



d. Sample Command

Username:
columbia

Password:
116bway

Welcome to simple chat server!
Command: 
whoelse
google

Command:
wholast 6
facebook
google

Command:
broadcast message hi, I’m columbia

Command:
broadcast user google message hello google

Command:
message facebook It’s a secret

Command:
logout


e. additional information

the function doesn’t work well:
Automatic log out after 30 minutes inactivity
Graceful exit of client and server programs using control + c
