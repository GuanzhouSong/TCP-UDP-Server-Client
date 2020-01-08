/*
 *
 *  UDPClient
 *  * Compile: java UDPClient.java
 *  * Run: java UDPClient localhost PortNo
 *  * Mine: java client/UDPClient localhost 8080
 */

package client;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class UDPClient {
	public static void main(String[] args) throws Exception {
		boolean isConnected = false;
		
        if(args.length != 2){
            System.out.println("Usage: java UDPClinet localhost PortNo");
            System.exit(1);
        }
     
        // Define socket parameters, address and Port No
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);
		//change above port number if required
		
		// create socket which connects to server
		DatagramSocket clientSocket = new DatagramSocket();
		
		String loginDetails = promptLogin();
		
		while(!isConnected) {
	        //prepare for sending
	        byte[] sendData=new byte[1024];
	        sendData=loginDetails.getBytes();
			// write to server, need to create DatagramPAcket with server address and port No
	        DatagramPacket sendPacket=new DatagramPacket(sendData,sendData.length,IPAddress,serverPort);
	        //actual send call
	        clientSocket.send(sendPacket);
	        
	        //prepare buffer to receive reply
	        byte[] receiveData=new byte[1024];
			// receive from server
	        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
	        clientSocket.receive(receivePacket);
	
	        String reply = new String(receivePacket.getData());
	        reply = reply.trim();

	        if (reply.equals("Login Success")) {
	        	isConnected = true;
	        	System.out.println("User has successfully logged onto the System! Enjoy messaging! [press ENTER to CONTINUE and to RESET the terminal!]");
	        	
	        	// Send Broadcast Notification for Login
	        	CommandInterface Icmd = new CommandInterface(CommandEnum.BROADCASTLOGIN);
		        sendData=Icmd.toString().getBytes();
		        sendPacket=new DatagramPacket(sendData,sendData.length,IPAddress,serverPort);
		        clientSocket.send(sendPacket);
		        
		        Icmd = new CommandInterface(CommandEnum.CHECKOFFLINEMSG) ;
		        sendData=Icmd.toString().getBytes();
		        sendPacket=new DatagramPacket(sendData,sendData.length,IPAddress,serverPort);
		        clientSocket.send(sendPacket);
	        	break;
	        } else {
	        	loginDetails = promptLogin();
	        }
		}
		
		// Create runnable handlers that manages processing incoming data, and outgoing data
		ServerMsgHandler handle = new ServerMsgHandler(clientSocket);
		ClientMsgSender sender = new ClientMsgSender(clientSocket, IPAddress, serverPort);
		TCPMsgReciever TCPHandle = new TCPMsgReciever (serverPort);
		Thread handleThread = new Thread(handle);
		Thread senderThread = new Thread(sender);
		Thread TCPHandleThread = new Thread(TCPHandle);
		
		TCPHandleThread.start();
		handleThread.start();
		senderThread.start();
	}
	
	public static String promptLogin() {
		String username = "";
		String password = "";
		BufferedReader inFromUser =
			new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.println("Please Enter your Login Details:");
			System.out.print("Username: ");
			username = inFromUser.readLine();
			
			System.out.print("Password: ");
			password = inFromUser.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		CommandInterface Icmd = new CommandInterface(CommandEnum.REQLOGIN);
		
		Icmd.addArg(username);
		Icmd.addArg(password);
		
		return Icmd.toString();
	}
} // end of class UDPClient

class TCPMsgSender1 implements Runnable {
	int serverSocket;
	InetAddress IPAddress;
	String msg = "";
	
	TCPMsgSender1(String msg, int serverSocket, InetAddress IPAddress) {
		this.serverSocket = serverSocket;
		this.IPAddress = IPAddress;
		this.msg = msg;
	}
	
	@Override
	public void run() {
		try {
			sendTCPMessage(msg, serverSocket, IPAddress);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendTCPMessage(String msg, int serverSocket, InetAddress IPAddress) throws IOException {
		Socket clientSocket = new Socket(IPAddress, serverSocket);
		  
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		 
		outToServer.writeBytes(msg);
		  
		clientSocket.close();
	}
}


class TCPMsgReciever implements Runnable {
	String clientMessage;
	ServerSocket serverSocket = null;
	  
	TCPMsgReciever(int serverPort) throws IOException {
		serverSocket = new ServerSocket(serverPort);
	}
	
	@Override
	public void run() {
		while (true) {
			Socket connectionSocket = null;
			BufferedReader inFromClient = null;
			
			// Set up welcoming connection socket
			try {
				connectionSocket = serverSocket.accept();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			// Create buffered reader for client message
			try {
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			try { clientMessage = inFromClient.readLine(); } catch (IOException e) {	e.printStackTrace(); }
		   
		    System.out.println("TCPReceived: " + clientMessage);
		}
	}
}


class ClientMsgSender implements Runnable {
	private DatagramSocket clientSocket = null;
	private InetAddress IPAddress = null;
	private int serverPort = -1;
	boolean isOnline = true;
	
	ClientMsgSender(DatagramSocket clientSocket, InetAddress IPAddress, int serverPort) {
		this.clientSocket = clientSocket;
		this.IPAddress = IPAddress;
		this.serverPort = serverPort;
	}
	
	private void sendMessage(String msg) {
       	byte sendDataBuffer[] = msg.getBytes();
	    DatagramPacket packet = new DatagramPacket(sendDataBuffer, sendDataBuffer.length, IPAddress, serverPort);
	    
	    try {
			clientSocket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
			
	@Override
	public void run() {		
        while (isOnline) {
        	String userInput = null;
        	String[] userCmdList = null;
        	String userCmd = null;
            try {
                userInput = promptUserInput();
		    	userCmdList = userInput.split(" ");
		    	userCmd = userCmdList[0];

		    	switch (userCmd) {
		    	case "--help":
		    		System.out.println("-------- HELP MENU --------");
		    		System.out.println("send message: message <user> <message>");
		    		System.out.println("broadcast message: broadcast <message>");
		    		System.out.println("see all other online users: whoelse");
		    		System.out.println("online since: whoelsesince <time>");
		    		System.out.println("block user: block <user>");
		    		System.out.println("unblock user: unblock <user>");
		    		System.out.println("logout from system: logout");
		    		System.out.println("---------------------------");
		    		break;
		    	case "whoelsesince":
		    		if (userCmdList.length > 1) {
			    		CommandInterface Icmd2 = new CommandInterface(CommandEnum.REQWHOELSESINCE);
		    			Icmd2.addArg(userCmdList[1]);
		    			
		    			sendMessage(Icmd2.toString());
		    		} else {
		    			System.out.println("Error. Usage: whoelsesince <time>");
		    		}
	    			break;
		    	case "whoelse":
		    		CommandInterface Icmd = new CommandInterface(CommandEnum.REQWHOELSE);
		    		
		    		sendMessage(Icmd.toString());
		    		break;
		    	case "logout":
		    		// Send Broadcast Notification for Logout
		        	CommandInterface Icmd4 = new CommandInterface(CommandEnum.BROADCASTLOGOUT);;
		        	sendMessage(Icmd4.toString());
		    		
		    		CommandInterface Icmd3 = new CommandInterface(CommandEnum.REQLOGOUT);
		    		sendMessage(Icmd3.toString());
			        break;
		    	case "message":
		    		if (userCmdList.length <= 2) {
		    			System.out.println("Error. Usage: message <user> <message>. Please Try Again");
		    		} else {
			    		CommandInterface Icmd5 = new CommandInterface(CommandEnum.MESSAGEUSER);
			    		boolean tempflag = true;
			    		
			    		// Add Target user and message content to Interface
			    		for (String s : userCmdList) {
			    			if (tempflag == true) {
			    				tempflag = false;
			    				continue;
			    			} 
			    			
			    			Icmd5.addArg(s);
			    		}
			    		
			    		sendMessage(Icmd5.toString());
		    		}
		    		break;
		    	case "broadcast":
		    		if (userCmdList.length > 1) {
			    		CommandInterface Icmd6 = new CommandInterface(CommandEnum.BROADCASTMESSAGE);
			    		boolean tempflag = true;
			    		
			    		// Add Target user and message content to Interface
			    		for (String s : userCmdList) {
			    			if (tempflag == true) {
			    				tempflag = false;
			    				continue;
			    			} 
			    			
			    			Icmd6.addArg(s);
			    		}
			    		
			    		sendMessage(Icmd6.toString());
		    		} else {
		    			System.out.println("Error. Usage: broadcast <message>");
		    		}
		    		break;
		    	case "block":
		    		if (userCmdList.length == 2) {
		    			CommandInterface Icmd7 = new CommandInterface(CommandEnum.BLOCKUSER);
		    			
		    			Icmd7.addArg(userCmdList[1]);
		    					    			
		    			sendMessage(Icmd7.toString());
		    		} else {
		    			System.out.println("Error. Usage: block <user>");
		    		}
		    		break;
		    	case "unblock":
		    		if (userCmdList.length == 2) {
		    			CommandInterface Icmd8 = new CommandInterface(CommandEnum.UNBLOCKUSER);
		    			
		    			Icmd8.addArg(userCmdList[1]);
		    					    			
		    			sendMessage(Icmd8.toString());
		    		} else {
		    			System.out.println("Error. Usage: unblock <user>. Please Try Again.");
		    		}
		    		break;
		    	case "startprivate":
		    		if (userCmdList.length == 2) {
			    		CommandInterface Icmd9 = new CommandInterface(CommandEnum.REQINITPRIVATE);
			    		
			    		Icmd9.addArg(userCmdList[1]);
			    		
			    		sendMessage(Icmd9.toString());
		    		} else {
		    			System.out.println("Error. Usage: startprivate <user>. Please Try Again.");
		    		}
		    		break;
		    	case "private":
		    		if (userCmdList.length <= 2) {
		    			System.out.println("Error. Usage: private <user> <message>. Please Try Again");
		    		} else {
		    			CommandInterface Icmd10 = new CommandInterface(CommandEnum.SENDPRIVATE);
			    		boolean tempflag = true;
			    		
			    		// Add Target user and message content to Interface
			    		for (String s : userCmdList) {
			    			if (tempflag == true) {
			    				tempflag = false;
			    				continue;
			    			} 
			    			
			    			Icmd10.addArg(s);
			    		}
			    		
			    		sendMessage(Icmd10.toString());
		    		}
		    		break;
		    	case "stopprivate":
		    		if (userCmdList.length == 2) {
			    		CommandInterface Icmd11 = new CommandInterface(CommandEnum.REQSTOPINIT);
			    		
			    		Icmd11.addArg(userCmdList[1]);
			    		
			    		sendMessage(Icmd11.toString());
		    		} else {
		    			System.out.println("Error. Usage: stopprivate <user>. Please Try Again.");
		    		}
		    		break;
		    	case "\n":
		    		break;
		    	case " ":
		    		break;
		    	default:
		    		if (userInput.length() != 0) {
		    			System.out.println("Command not recognised, refer to --help for valid commands");
		    		}
		    		break;
		    		
		    	}
	
                Thread.sleep(100);
		    	System.out.println("\n>>> Please Enter your next command: (Type --help for more info)");
            } catch(Exception e) {
                System.err.println(e);
            }
        }
    }
	
	public static String promptUserInput() {
		String userCmd = "";
		
		BufferedReader inFromUser =
				new BufferedReader(new InputStreamReader(System.in));
			
			try {
                while (!inFromUser.ready()) {
                    try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
			
				userCmd = inFromUser.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return userCmd;
	}

}

class ServerMsgHandler implements Runnable {
	private DatagramSocket clientSocket = null;
	private byte receiveDataBuffer[] = null;
	
	ServerMsgHandler(DatagramSocket clientSocket) {
		this.clientSocket = clientSocket;
		receiveDataBuffer = new byte[1024];
	}
	
	@Override
	public void run() {
		 while (true) {
	            try {
	            	// receive from server
	            	receiveDataBuffer = new byte[1024];
	                DatagramPacket receivePacket = new DatagramPacket(receiveDataBuffer, receiveDataBuffer.length);
	                clientSocket.receive(receivePacket);
	                
	                // process result to String
	                String receivedString = new String(receivePacket.getData());
	                receivedString = receivedString.trim();
			    	String[] serverCmdList = receivedString.split(" ");
			    	String serverCmd = serverCmdList[0];

	                if (receivedString.equals("Logout Success")) {
	                	System.out.println("LOGOUT SUCCESSFULL");			
	                	
				        //close the socket
				        System.exit(0);
			        } else if (receivedString.equals("Login Failed - Incorrect Password")) {
			        	;
			        } else if (receivedString.equals("Login Failed")) {
			        	;
			        } else if (receivedString.equals("Login Failed - User does not exist")) {
			        	;
			        } else if (receivedString.equals("Your account is blocked due to multiple login failures. Please try again later")) {
			        	;
			        } else if (receivedString.equals("Unknown command, login and refer to --help")) {
			        	;
			        } else if (receivedString.contains("Current time is")) {
			        	;
		    		} else if (serverCmd.equals("GIVEWHOELSESINCE")) {
		    			String[] otherOnlineUsersList = receivedString.split(" ");
		    			
		    			if (otherOnlineUsersList.length == 1) {
		    				System.out.println("--- No Other Users Has Logged on since --- ");
		    			} else {
		    				System.out.println("--- List of other users who are online since: ---");

		    				for (String u : otherOnlineUsersList) {
		    					if (u.equals("GIVEWHOELSESINCE")) continue;
		    						System.out.println("> " + u);
			    			}
		    				
		    				System.out.println("-------------------------------------------");
		    			}
		    			
		    		} else if (serverCmd.equals("GIVEWHOELSE")) {
		    			String[] otherOnlineUsersList = receivedString.split(" ");

		    			if (otherOnlineUsersList.length == 1) {
		    				System.out.println("--- No Other Users Online Currently --- ");
		    			} else {
		    				System.out.println("--- List of other users who are online: ---");

		    				for (String u : otherOnlineUsersList) {
		    					if (u.equals("GIVEWHOELSE")) continue;
		    						System.out.println("> " + u);
			    			}
		    				
		    				System.out.println("-------------------------------------------");
		    			}
	            	} else if (receivedString.contains("SYSTEM") ) {
	            		if (receivedString.contains("While user was offline")) {
	            			String[] eachMsgList = receivedString.split(",");
	            			
	            			for (String s : eachMsgList) {
	            				System.out.println(s);
	            			}
	            		} else {
	            			System.out.println(receivedString);
	            		}
            		} else if (receivedString.equals("USER TIMEOUT")) {
		    			System.out.println("YOU HAVE BEEN LOGGED OUT. [TIMEOUT]");
		    			System.exit(0);
		    		} else if (serverCmd.equals("MESSAGEREPLY")) {
		    			System.out.print("FROM " + serverCmdList[1] + ": ");
		    			
		    			int tempCounter = 0;
		    			
		    			for (String s : serverCmdList) {
		    				if (tempCounter >= 2) {
		    					System.out.print(s + " ");
		    				}
		    				tempCounter++;
		    			}
		    			
		    			System.out.println("\n");
		    		} else if (serverCmd.equals("BLOCKREPLY")) {
		    			// Print everything after serverCmd
		    			for (String s : serverCmdList) {
		    				if (serverCmd.equals(s)) {
		    					continue;
		    				}
		    				
		    				System.out.print(s + " ");
		    			}
		    			
		    			System.out.print("\n");
		    		} else if (serverCmd.equals("UNBLOCKREPLY")) {
		    			// Print everything after serverCmd
		    			for (String s : serverCmdList) {
		    				if (serverCmd.equals(s)) {
		    					continue;
		    				}
		    				
		    				System.out.print(s + " ");
		    			}
		    			
		    			System.out.print("\n");
		    		}
			        else {
			        	;
			        }
	                
//	                System.out.println("FROM SERVER: " + receivedString);
	            } catch(Exception e) {
	                System.err.println(e);
	            }
		 }
	}
	
}
