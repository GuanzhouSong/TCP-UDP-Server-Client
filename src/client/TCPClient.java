package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import server.PrivateUser;

public class TCPClient {
	static List<PrivateUser> users = new ArrayList<PrivateUser>();
    
	public static void main(String[] args) throws Exception {
		boolean isConnected = false;
		
        if(args.length != 2){
            System.out.println("Usage: java TCPClient localhost PortNo");
            System.exit(1);
        }
     
        // Define socket parameters, address and Port No
        InetAddress IPAddress = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);
		Socket connSocket = null;
        
		// Define necessary Buffers, and Strings
		String serverReply = null;
		BufferedReader inFromServer = null;
		
		String clientName = null;
		
		// Prompt User for Login Details
		String loginDetails = promptLogin();
		
		// Create TCP port to Server
		connSocket = new Socket(IPAddress, serverPort);
		
		// OS assigned free port as client listener
		ServerSocket server = new ServerSocket(0);
		
		// While User is not logged in
		while(!isConnected) {
			sendTCPMessage(loginDetails, connSocket);

			// Accept data from server
			inFromServer = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));

			// Extract String from server reply
			serverReply = inFromServer.readLine();
			serverReply = serverReply.trim();

	        if (serverReply.equals("Login Success")) {
	        	// Extract ClientName
	        	String loginDetailsList[] = loginDetails.split(" ");
	        	clientName = loginDetailsList[1];
	        	
	        	isConnected = true;
	        	System.out.println("User has successfully logged onto the System! Enjoy messaging! [press ENTER to CONTINUE and to RESET the terminal!]");
	        	
	        	// Send Broadcast Notification for Login
	        	CommandInterface Icmd = new CommandInterface(CommandEnum.BROADCASTLOGIN);
	        	
	        	Icmd.addArg(Integer.toString(server.getLocalPort()) );
	        	
	        	sendTCPMessage(Icmd.toString(), connSocket);
		        
	        	// Check User's Offline Messages
		        Icmd = new CommandInterface(CommandEnum.CHECKOFFLINEMSG) ;
		        sendTCPMessage(Icmd.toString(), connSocket);
	        	break;
	        } else {
	        	System.out.println(serverReply);
	        	
	        	// If User Authentication fail, prompt user for further login attempts
	        	loginDetails = promptLogin();
	        }
		}
		
		// Create runnable handlers that manages processing incoming data, and outgoing data
		TCPMsgSender sender = new TCPMsgSender(connSocket, users, clientName);
		TCPMsgReceiver receiver = new TCPMsgReceiver(connSocket, users, clientName);
		PrivateClientThread privateReceiver = new PrivateClientThread(server);

		// Create new threads to handle send and receive
		Thread receiverThread = new Thread(receiver);
		Thread senderThread = new Thread(sender);
		Thread privateThread = new Thread(privateReceiver);
	
		// Start execution of threads
		receiverThread.start();
		senderThread.start();
		privateThread.start();
	}
	
	private static void sendTCPMessage(String msg, Socket clientSocket) throws IOException {
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter outputWriter = new PrintWriter(output, true);
		
		outputWriter.println(msg);
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
} // end of class TCPClient


class TCPMsgSender implements Runnable {
	int serverPort;
	int localClientPort;
	InetAddress IPAddress;
	Socket clientSocket;
	boolean isOnline = true;
	static List<PrivateUser> users = new ArrayList<PrivateUser>();
	String clientName;
	
	TCPMsgSender(Socket connSocket, List<PrivateUser> users, String clientName) throws IOException {
		this.serverPort = connSocket.getPort();
		this.IPAddress = connSocket.getInetAddress();
		this.clientSocket = connSocket;
		this.localClientPort = connSocket.getLocalPort();
		TCPMsgSender.users = users;
		this.clientName = clientName;
	}
	
	@Override
	public void run() {
		while (isOnline) {
        	String userInput = null;
        	String[] userCmdList = null;
        	String userCmd = null;
        	CommandInterface Icmd = null;
        	
        	// Get User Input from terminal
            try {
                userInput = promptUserInput();
		    	userCmdList = userInput.split(" ");
		    	userCmd = userCmdList[0];
		    	
		    	// Take action depending on user command
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
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);		    		
		    		break;
		    	case "whoelsesince":
		    		if (userCmdList.length > 1) {
		    			// Add server command
			    		Icmd = new CommandInterface(CommandEnum.REQWHOELSESINCE);
		    			
			    		// Add information needed to process command
			    		Icmd.addArg(userCmdList[1]);
		    			
			    		// Send message to server
			    		sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: whoelsesince <time>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
	    			break;
		    	case "whoelse":
		    		if (userCmdList.length == 1) {
			    		// Request other online users
			    		Icmd = new CommandInterface(CommandEnum.REQWHOELSE);
			    		sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: whoelse");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "logout":
		    		// Send Broadcast Notification for Logout
		        	Icmd = new CommandInterface(CommandEnum.BROADCASTLOGOUT);
		        	sendTCPMessage(Icmd.toString(), clientSocket);
		        	
		        	Thread.sleep(100);
		    		
		        	// Request Logout
		        	CommandInterface Icmd2 = new CommandInterface(CommandEnum.REQLOGOUT);
		    		sendTCPMessage(Icmd2.toString(), clientSocket);
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
			        break;
		    	case "message":
		    		if (userCmdList.length > 2) {
			    		Icmd = new CommandInterface(CommandEnum.MESSAGEUSER);
			    		boolean tempflag = true;
			    		
			    		// Add Target user and message content to Interface
			    		for (String s : userCmdList) {
			    			if (tempflag == true) {
			    				tempflag = false;
			    				continue;
			    			} 
			    			
			    			Icmd.addArg(s);
			    		}
			    		
			    		sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: message <user> <message>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "broadcast":
		    		if (userCmdList.length > 1) {
			    		Icmd = new CommandInterface(CommandEnum.BROADCASTMESSAGE);
			    		boolean tempflag = true;
			    		
			    		// Add Target user and message content to Interface
			    		for (String s : userCmdList) {
			    			if (tempflag == true) {
			    				tempflag = false;
			    				continue;
			    			} 
			    			
			    			Icmd.addArg(s);
			    		}
			    		
			    		sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: broadcast <message>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "block":
		    		if (userCmdList.length == 2) {
		    			Icmd = new CommandInterface(CommandEnum.BLOCKUSER);
		    			
		    			Icmd.addArg(userCmdList[1]);
		    					    			
		    			sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: block <user>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "unblock":
		    		if (userCmdList.length == 2) {
		    			Icmd = new CommandInterface(CommandEnum.UNBLOCKUSER);
		    			
		    			Icmd.addArg(userCmdList[1]);
		    					    			
		    			sendTCPMessage(Icmd.toString(), clientSocket);
		    		} else {
		    			System.out.println("Error. Usage: unblock <user>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "startprivate":
		    		if (userCmdList.length == 2) {
		    			String targetUserName = userCmdList[1];
		    			
		    			if (!targetUserName.equals(clientName)) {
			    			if (isUserPrivateMsgEnabled(targetUserName, this.clientName)) {
			    				System.out.println("Private message between user is already initiated.");
			    			} else {	    			
					    		Icmd = new CommandInterface(CommandEnum.REQINITPRIVATE);
					    		
					    		Icmd.addArg(userCmdList[1]);
					    		
					    		sendTCPMessage(Icmd.toString(), clientSocket);
			    			}
		    			} else {
		    				System.out.println("Error. User cannot start private message with oneself");
		    			}
		    			
		    		} else {
		    			System.out.println("Error. Usage: startprivate <user>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "private":
		    		if (userCmdList.length > 2) {
		    			
		    			String targetUserName = userCmdList[1];
		    			
		    			if (!targetUserName.equals(clientName)) {
			    			if (isUserPrivateMsgEnabled(targetUserName, this.clientName)) {				
				    			Icmd = new CommandInterface(CommandEnum.SENDPRIVATE);
				    			Icmd.addArg(clientName);
					    		boolean tempflag = true;
					    		
					    		// Add Target user and message content to Interface
					    		for (String s : userCmdList) {
					    			if (tempflag == true) {
					    				tempflag = false;
					    				continue;
					    			} 
					    			
					    			Icmd.addArg(s);
					    		}
					    		
					    		Socket privateSocket = new Socket(getInetAddressFromPrivateUsers(targetUserName), getPortNumberFromPrivateUsers(targetUserName));
					    		
					    		sendTCPMessage(Icmd.toString(), privateSocket);
					    		
			    			} else {
			    				System.out.println("Error. Private Message not initiated. Usage: startprivate <user>");
			    			}
		    			} else {
		    				System.out.println("Error. User cannot private message oneself");
		    			}
		    			
		    		} else {
		    			System.out.println("Error. Usage: private <user> <message>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "stopprivate":
		    		if (userCmdList.length == 2) {
			    		String targetUserName = userCmdList[1];
		    			
		    			if (!targetUserName.equals(clientName)) {
			    			if (isUserPrivateMsgEnabled(targetUserName, this.clientName)) {
			    			
					    		Icmd = new CommandInterface(CommandEnum.REQSTOPINIT);
					    		
					    		// add target
					    		Icmd.addArg(userCmdList[1]);
					    		
					    		// add host
					    		Icmd.addArg(this.clientName);
					    		
					    		sendTCPMessage(Icmd.toString(), clientSocket);
			    			} else {
			    				System.out.println("Error. User has not initiated private messaging with given target user");
			    			}
		    			} else {
		    				System.out.println("Error. User cannot stop private message upon oneself");
		    			}
			    		
		    		} else {
		    			System.out.println("Error. Usage: stopprivate <user>");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case "\n":
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	case " ":
		    		System.out.println("Command not recognised, refer to --help for valid commands");

		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;
		    	default:
		    		if (userInput.length() != 0) {
		    			System.out.println("Command not recognised, refer to --help for valid commands");
		    		} else {
		    			System.out.println("");
		    		}
		    		
		    		sendTCPMessage("REFRESHTIMEOUT", clientSocket);
		    		break;	    		
		    	} // end case
	
                Thread.sleep(100);
		    	System.out.println("\n>>> Please Enter your next command: (Type --help for more info)");
            } catch(Exception e) { System.err.println(e); }
		}
	} // end of TCPMsgSender run
	
	private InetAddress getInetAddressFromPrivateUsers(String targetUserName) {
		for (PrivateUser user : users) {
			if (user.getUserName().equals(targetUserName)) {
				return user.getIPAddress();
			} else {
				if (user.getPairedUser().getUserName().equals(targetUserName)) {
					return user.getPairedUser().getIPAddress();
				} else {
					return null;
				}
			}
		}
		
		return null;
	}
	
	private int getPortNumberFromPrivateUsers(String targetUserName) {
		for (PrivateUser user : users) {
			if (user.getUserName().equals(targetUserName)) {
				return user.getServerPort();
			} else {
				if (user.getPairedUser().getUserName().equals(targetUserName)) {
					return user.getPairedUser().getServerPort();
				} else {
					return -1;
				}
			}
		}
		
		return -1;
	}
	
	private boolean isUserPrivateMsgEnabled(String name, String otherName) {
		for (PrivateUser user : users) {		
			if (user.getUserName().equals(name) && user.getPairedUser().getUserName().equals(otherName)) {
				return true;
			} else if (user.getUserName().equals(otherName) && user.getPairedUser().getUserName().equals(name)) {
				return true;
			} else {
				return false;
			}
		}
		
		return false;
	}
	
	private static void sendTCPMessage(String msg, Socket clientSocket) throws IOException {
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter outputWriter = new PrintWriter(output, true);
		
		outputWriter.println(msg);
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
} // end of TCPMsgSender class

class TCPMsgReceiver implements Runnable {
	int serverPort;
	Socket connectionSocket = null;
	static List<PrivateUser> users = new ArrayList<PrivateUser>();
	String clientName;
	
	TCPMsgReceiver(Socket connSocket, List<PrivateUser> users, String clientName) throws IOException {
		this.serverPort = connSocket.getPort();
		this.connectionSocket = connSocket;
		TCPMsgReceiver.users = users;
		this.clientName = clientName;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				BufferedReader inFromClient = null;
				String receivedString = null;
				
				// Create buffered reader for client message
				try {
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				// Get String from server
				try {
					receivedString = inFromClient.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (receivedString == null) {
					System.exit(1);
				}

				// Clean String and extract user command
				receivedString = receivedString.trim();
				String[] serverCmdList = receivedString.split(" ");
		    	String serverCmd = serverCmdList[0];
		    	
		    	// Handle Server Response
                if (receivedString.equals("Logout Success")) {
                	System.out.println("LOGOUT SUCCESSFULL");			
                	connectionSocket.close();
                	// Exit program (logout)
			        System.exit(0);
			    // Ignore these set of server messages
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
	    		} 
		        // End Ignored server messages
		        else if (serverCmd.equals("GIVEWHOELSESINCE")) {
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
	    		else if (serverCmd.equals("GIVEINITPRIVATE")) {
	    			String senderUserName = serverCmdList[1];
	    			InetAddress senderIPAddress = InetAddress.getByName("localhost");
	    			int senderPortNumber = Integer.parseInt(serverCmdList[3]);
	    			
	    			
	    			String targetUserName = serverCmdList[4];
	    			InetAddress targetIPAddress = InetAddress.getByName("localhost");
	    			int targetPortNumber = Integer.parseInt(serverCmdList[6]);
	    			
	    			PrivateUser senderUser = new PrivateUser(senderUserName, senderIPAddress, senderPortNumber);
	    			PrivateUser targetUser = new PrivateUser(targetUserName, targetIPAddress, targetPortNumber);
	    		
	    			senderUser.setPairedUser(targetUser);
	    			targetUser.setPairedUser(senderUser);
	    			
	    			users.add(senderUser);
	    			
	    			if (serverCmdList[7].equals("true")) {
	    				System.out.println("Private messaging started");
	    			}
	    		}
	    		else if (serverCmd.equals("GIVESTOPINIT")) {
	    			String senderUserName = serverCmdList[1];
	    			String targetUserName = serverCmdList[4];

	    			
	    			if (isUserPrivateMsgEnabled(senderUserName, targetUserName)) {
		    			stopPrivateConnection(senderUserName, targetUserName);
	    			}
			
	    			if (serverCmdList[7].equals("true")) {
	    				System.out.println("Private messaging stopped");
	    			}
	    		}
		        else {
		        	;
		        }
		    	
			} catch(Exception e) {
                System.err.println(e);
            }
		} // END WHILE
	} // END RUN
	
	private void stopPrivateConnection(String name, String otherName) {
		for (PrivateUser user : users) {		
			if (user.getUserName().equals(name) && user.getPairedUser().getUserName().equals(otherName)) {
				users.remove(user);
				break;
			} else if (user.getUserName().equals(otherName) && user.getPairedUser().getUserName().equals(name)) {
				users.remove(user);
				break;
			} 
		}
	}
	
	private boolean isUserPrivateMsgEnabled(String name, String otherName) {
		for (PrivateUser user : users) {	
			if (user.getUserName().equals(name) && user.getPairedUser().getUserName().equals(otherName)) {
				return true;
			} else if (user.getUserName().equals(otherName) && user.getPairedUser().getUserName().equals(name)) {
				return true;
			} else {
				return false;
			}
		}
		
		return false;
	}
	
	@SuppressWarnings("unused")
	private void sendTCPMessage(String msg, Socket clientSocket) throws IOException {
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter outputWriter = new PrintWriter(output, true);
		
		outputWriter.println(msg);
	}
}




