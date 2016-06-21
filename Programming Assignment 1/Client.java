import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class Client
{
	public static int port;
	public static String ip;
	public static void main(String[] args) throws IOException, InterruptedException {
		ip = args[0];
		port = Integer.parseInt(args[1]);
		new Client().doClient(ip,port);
	}
	
	public void doClient(String ip,int port) throws IOException, InterruptedException  {
		Socket socket = null;
		try {
			socket = new Socket("127.0.0.1",port);
			ReceiveThread receiveThread = new ReceiveThread(socket); //a thread to receive message
			receiveThread.start();
			SendThread sendThread = new SendThread(socket); //a thread to send message
			sendThread.start();
			receiveThread.join();
			sendThread.join();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public class ReceiveThread extends Thread{
		Socket socket;
		public ReceiveThread(Socket socket){
			this.socket = socket;
		}
		
		public void run(){ 
			BufferedReader bufferedReader;
			while(true){
				try {
					bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					System.out.println(bufferedReader.readLine());
				} catch (IOException e) {
					System.exit(0);
					// TODO Auto-generated catch block
				} 
			}
		}
	}
	

	public class SendThread extends Thread{
		Socket socket;
		public SendThread(Socket socket){
			this.socket = socket;
		}
		
		public void run() {
			Scanner sc;
			PrintWriter printWriter;
			while(!socket.isClosed()){
				try {
					sc = new Scanner(System.in);
					printWriter = new PrintWriter(socket.getOutputStream(),true);
					printWriter.println(sc.nextLine());
				} catch (IOException e) {
					System.exit(0);
					// TODO Auto-generated catch block
				} 
			}
		}
	}
	
}
	
		

	
	
	
