package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PrivateClientThread implements Runnable {
	int localClientPort;
	ServerSocket clientServerSocket = null;
	Socket connSocket = null;
	
	PrivateClientThread(ServerSocket clientServerSocket) {
		this.clientServerSocket = clientServerSocket;
		localClientPort = clientServerSocket.getLocalPort();
	}
	
	@Override
	public void run() {
		while (true) {
			BufferedReader inFromClient = null;
			String clientSentence = null;
			
			// Accept incoming message (private message)
			try {connSocket = clientServerSocket.accept();} catch (IOException e) {e.printStackTrace();}
				   
			try {
				inFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			   
			try {
				clientSentence = inFromClient.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Clean String and extract user command
			clientSentence = clientSentence.trim();
			String[] clientSentenceList = clientSentence.split(" ");
			
		    
			System.out.print("(Private) " + clientSentenceList[1] + ": ");
			
			boolean flag = false;
			
			for (String s : clientSentenceList) {
				if (flag == false) {
					if (s.equals(clientSentenceList[2])) {
						flag = true;
					}
					continue;
				} else {
					System.out.print(s + " ");
				}
			}
			
			// New line
			System.out.println("");
	
		} // end while
	} // end run
	
} // end class
