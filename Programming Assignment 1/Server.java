import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Server {
	//public static int port; 
	private int BLOCK_TIME = 60; //seconds
	private static final int TIME_OUT = 30; // minutes
	private List<String> onlineUser = new LinkedList<String>(); //initiate a string to store online user;	
	private List<Socket> sockets = new LinkedList<Socket>(); //initiate a list to store online user's socket
	private HashMap<String,Long> lastActionTime = new HashMap<String,Long>(); //the last time a user sent message to server
	private HashMap<String, Integer> failedTimes = new HashMap<String, Integer>(); //record failed time for a combination of user name and IP address
	private HashMap<String, Long> failedTime = new HashMap<String, Long>(); //record the time that a user-IP was blocked

	public static void main (String[] args) throws IOException{	
		new Server(Integer.parseInt(args[0]));
	}
	
	
	public Server(int port) throws IOException {
		List<String> list = new ArrayList<String>();//initiate a string array to store user name and password
		read("user_pass.txt",list); // get the pre-arranged user name and password
		doServer(list,port);
	}
	
	
	public void doServer (List<String> list, int port) throws IOException{
		ServerSocket serverSocket=new ServerSocket(port);
		// form connection with a client
		while(true){
			Socket socket=serverSocket.accept(); 
			new ServerThread(socket,list).start();
		}		
	}
	
	
	public class ServerThread extends Thread{
		Socket socket;
		List<String> list;
		public ServerThread(Socket socket,List<String> list){
			this.socket=socket;
			this.list=list;
		}
		
		public void run(){ 
			logIn(socket,list); //user login
		}
		
	}
	
	
	private void logIn (Socket socket,List<String> list)
	{
		// send message to verify user name and password
		String userpass1=null; // user input user name
		String userpass2=null; // user input password
		String myUserName=null;
		
		logintrial:
		while(true){
			String ip = socket.getInetAddress().toString();
			boolean a = true; // when a is true, it means log in fail because user name and password don't match
			// type in user name and password
			try {
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
				printWriter.println("Username: ");
				BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String str=bufferedReader.readLine();
				userpass1=str; //input user name
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
				printWriter.println("Password: ");
				BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
			    String str=bufferedReader.readLine();
			    userpass2=str; //input password
			    //System.out.println(userpass1+" "+userpass2);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String userIP=userpass1+ip;
			// judge whether it is blocked
			if (failedTime.containsKey(userIP) && (System.currentTimeMillis() - failedTime.get(userIP)) < BLOCK_TIME*1000) {
				a = false;
				try {
					PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
					long count = 60-(System.currentTimeMillis() - failedTime.get(userIP))/1000;
					printWriter.println("You cannot log in until "+ Long.toString(count) + " seconds later");
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			
			else {
				// verify whether user name and password match or not
				verify:
				for(int i=0; i < list.size(); i++)
				{
					 
					String obj_1 = list.get(i).split(" ")[0]; // split to get user name
					String obj_2 = list.get(i).split(" ")[1]; //split to get password
					if(obj_1.contains(userpass1) && userpass1.contains(obj_1) && obj_2.contains(userpass2) && userpass2.contains(obj_2)) {
						try {
							for(int j=0;j<onlineUser.size();j++){
								//check if the user is online
								if(onlineUser.get(j).contains(userpass1)) {
									PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
									printWriter.println("The user is online, you cannot log in");
									a = false;
									break verify;
								}
							}
							PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
							String welcome = "Welcome to simple chat server!";
							printWriter.println(welcome);
							a = false;
							onlineUser.add(userpass1);
							sockets.add(socket); //in the two LinkedList, user name and its socket's index are the same
							myUserName=userpass1;
							lastActionTime.put(myUserName,System.currentTimeMillis());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break logintrial;
					}
					
				}
			}
				
			while (a == true)
			{
				//log in failed. Keep track of the failed time for a server and set block time 
				if (failedTimes.containsKey(userIP)) {
					int t = failedTimes.get(userIP);
					failedTimes.put(userIP, t+1);
				}
				else {
					failedTimes.put(userIP, 1);
				}
				// 
				if (failedTimes.get(userIP) >= 3) {
					try {
						failedTimes.put(userIP, 0);
						PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
						String blockTime = String.valueOf(BLOCK_TIME);
						failedTime.put(userIP,System.currentTimeMillis());
						printWriter.println("Log in failed 3 times, you will be blocked for "+blockTime+" seconds");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					try {
						PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
						printWriter.println("User name and password not match, press enter to retry");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				a = false;
			}
		 
		}
		command(socket, myUserName,list);
	}
	
	private void command (Socket socket, String myUserName, List<String> list){
		PrintWriter printWriter;
		BufferedReader bufferedReader;
		boolean b=true;
 		while(b==true){		
			String message;
			try {
				printWriter=new PrintWriter(socket.getOutputStream(),true);
				printWriter.println("Control: ");
				bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
				message=bufferedReader.readLine();
				//check if the user has been inactive for a certain period
				if ((System.currentTimeMillis() - lastActionTime.get(myUserName)) <= TIME_OUT*1000*60) {
					lastActionTime.put(myUserName,System.currentTimeMillis());					
					while(message!=null){
						// whoelse
						if(message.indexOf("whoelse") == 0 && message.length() == 7 ){
							whoElse(socket,myUserName);
						}
						//broadcast message
						else if(message.indexOf("broadcast message ")==0 && message.length() > 18){
							broadcastMessage(socket,message,myUserName);
						}
						//broadcast messages to several certain users
						else if(message.indexOf("broadcast user ")==0 && message.length() > 24){
							broadcastMessages(socket,message,myUserName);
						}
						//private message to a user
						else if(message.indexOf("message ") == 0 && message.length() > 8){
							privateMessage(socket,message,myUserName);
						}
						//wholast
						else if(message.indexOf("wholast ")==0 && message.length() >= 9 && message.length() <= 10){
							
							whoLast(socket, lastActionTime, list, message, myUserName);
							//System.out.println(Integer.parseInt(message.substring(8)));
						}
						// log out
						else if(message.indexOf("logout")==0 && message.length() == 6){
							b=false;
							bufferedReader.close();
							logOut(socket,myUserName, printWriter);
						}
						else {
							printWriter=new PrintWriter(socket.getOutputStream(),true);
							printWriter.println("Error: not a valid command");
						}
						break;
					} 
				}
				else
				{
					long s = System.currentTimeMillis() - lastActionTime.get(myUserName);
					System.out.println(Long.toString(s));
					autoLogOut (socket, myUserName);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
	}
	
	
	private void logOut (Socket socket, String myUserName, PrintWriter printWriter) throws IOException{
		try {
			printWriter=new PrintWriter(socket.getOutputStream(),true);
			printWriter.println("You have logged out");
			for(int i=0;i<onlineUser.size();i++)
			{
				if(onlineUser.get(i).contains(myUserName)){
					System.out.println(myUserName);
					printWriter.close();
					onlineUser.remove(i);
					sockets.remove(i); //in the two LinkedList, user name and its socket's index are removed at the same time
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(myUserName + " has logged out");
		}
		socket.close();
	}
	
	
	private void whoElse (Socket socket, String myUserName) throws IOException{
		PrintWriter printWriter;
		if(onlineUser.size()==1 || onlineUser.size()==0){
			printWriter=new PrintWriter(socket.getOutputStream(),true);
			printWriter.println("No other user online");
		}
		else{
			for(int i=0;i<onlineUser.size();i++){
				if(onlineUser.get(i)!=myUserName){
					printWriter=new PrintWriter(socket.getOutputStream(),true);
					printWriter.println(onlineUser.get(i));
				}
			}
		}
		
	}
	
	
	private void whoLast (Socket socket, HashMap<String,Long> lastActionTime, List<String> list, String message, String myUserName){
		PrintWriter printWriter;
		int time_out = Integer.parseInt(message.substring(8));
		long timeNow = System.currentTimeMillis();
		if (time_out <= 60 && time_out >= 0) {
			for (int i=0; i<list.size(); i++) {
				String key=list.get(i).split(" ")[0];
				if (lastActionTime.containsKey(key) && !myUserName.contains(key) && timeNow - lastActionTime.get(key) <= time_out*1000*60) {
					try {
						printWriter=new PrintWriter(socket.getOutputStream(),true);
						printWriter.println(key);	
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
						
			}
		}
		else{
			try {
				printWriter=new PrintWriter(socket.getOutputStream(),true);
				printWriter.println("Input is invalid. Please input a number in 0-60 range");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	
	private void broadcastMessage (Socket socket, String message,String myUserName){
		PrintWriter printWriter;
		String message_send=message.substring(18);//delete "broadcast message " and left useful message only
		for (int i=0;i<sockets.size();i++){
			Socket s=sockets.get(i);
			try {
				//sent message to all clients online except for yourself
				if(socket!=s) {
					printWriter=new PrintWriter(s.getOutputStream(),true);
					printWriter.println("message from "+myUserName+": "+message_send);
				}
			} catch (IOException e) {

			}
			
		}
	}
	
	
	private void broadcastMessages (Socket socket, String message,String myUserName){
		int index=message.indexOf(" message");
		String userToSend=message.substring(15,index);//extract user name part from the input of client 
		PrintWriter printWriter;
		for(int i=0;i<onlineUser.size();i++){
			if (userToSend.contains(onlineUser.get(i))){
				Socket s=sockets.get(i); 
				try {
					printWriter = new PrintWriter(s.getOutputStream(),true);
					printWriter.println(myUserName+": "+message.substring(index+9));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void privateMessage (Socket socket, String message,String myUserName){
		int del1=message.indexOf("message");
		String message_send=message.substring(del1+8);//delete "message " from the input of client
		for(int i=0; i<onlineUser.size();i++){
			if(message_send.indexOf(onlineUser.get(i))==0) //find the user to send message
			{
				message_send=message_send.substring(onlineUser.get(i).length()+1);
				String sendTo=onlineUser.get(i); //match user online with its socket
				Socket s=(Socket)sockets.get(i);
				PrintWriter printWriter;
				try {
					printWriter = new PrintWriter(s.getOutputStream(),true);
					printWriter.println("Private message from "+myUserName+" to "+sendTo+": "+message_send);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private void autoLogOut (Socket socket, String userName) {
		if (System.currentTimeMillis() - lastActionTime.get(userName) > TIME_OUT*1000*60) {
			PrintWriter printWriter;
			try {
				printWriter = new PrintWriter(socket.getOutputStream(),true);
				printWriter.println("You have been inactive for more than " + TIME_OUT + " minutes");
				logOut (socket, userName, printWriter);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();	
			}
		}
	}
	
	private void read(String file,List<String> list){
		try {
			String line=null;
			FileReader fileReader=new FileReader(file);
			BufferedReader bufferedReader=new BufferedReader(fileReader);
			try {
				while((line = bufferedReader.readLine())!=null){
					list.add(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("File not found!");
		}
		
	}
		
}

