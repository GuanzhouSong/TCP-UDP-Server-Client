package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import client.User;

public class TCPServer {
    static int UPDATE_INTERVAL = 1*1000; //milliseconds
	
    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static List<User> users = new ArrayList<User>();
    static List<User> onlineUsers = new ArrayList<User>();
    
    static ServerSocket serverSocket;
    static int blockDuration;
    static int timeout;
    static Timer timer;
    
	public static void main(String[] args)throws Exception {
        //Assign Port no
        int serverPort = Integer.parseInt(args[0]);
        serverSocket = new ServerSocket(serverPort);
        blockDuration = 0;
        timeout = 0;
		
		// Check and Assign block duration
		if (args.length >= 2) {
			blockDuration = Integer.parseInt(args[1]);
		}
		
		// Check and Assign timeout
		if (args.length >= 3) {
			timeout = Integer.parseInt(args[2]);
		}
		
		// Initializes all valid users from credentials.txt and adds to List<User> users
		initUsersFromCredentialsTXT();
		
		// Create timeout checker
 		timer = new Timer();
		timer.scheduleAtFixedRate(createTrackTimeout(), 500, 1000);
		
		System.out.println("Server is ready :");
		
		while (true) {
			Socket socket = serverSocket.accept();
			
			ServerThread st = new ServerThread(socket, clients, users, onlineUsers, blockDuration, timeout);
			st.start();
		}
	} // end main
	
    private static TimerTask createTrackTimeout() {
    	return new TimerTask() {
    		@Override
		    public void run() {
    			List<User> users = getOnlineUsers();
		    	for (User user : users) {
		    		if (user.getHasTimedOut() == true) {
		    			String sentence = attemptLogout(user.getsAddr());
		    			
		    			String logoutUserTimeOut = "USER TIMEOUT";
		    			try {
							sendTCPMessage(logoutUserTimeOut, user.getSocket());
						} catch (IOException e) {
							e.printStackTrace();
						}
		    			
		            	for (int j=0; j < clients.size();j++){
		            		try {
								sendTCPMessage(sentence, getOnlineUserBySAddr(clients.get(j)).getSocket());
							} catch (IOException e) {
								e.printStackTrace();
							}
		            	}
		    			break;
		    		}
		    	}
		    }
    	};
    }
    
    public static User getOnlineUserBySAddr(SocketAddress addr) {
    	for (User ExistingUser : onlineUsers) {
    		if (ExistingUser.getsAddr().equals(addr)) {
    			return ExistingUser;
    		}
    	}
    	
    	return null;
    }
    
    public static String attemptLogout(SocketAddress sAddr) {
    	String serverMessage = "Attemping Lougout";
    	User user = getOnlineUserBySAddr(sAddr);
    	if (user.getIsOnline()) {
    		if (user.getHasTimedOut() == false) {
	            user.setUserToOffline();
	            onlineUsers.remove(user);
	            clients.remove(sAddr);
	            serverMessage="Logout Success";
    		} else {
    			user.setUserToOffline();
	            onlineUsers.remove(user);
	            clients.remove(sAddr);
	            serverMessage = "SYSTEM: " + user.getUserName() + " has been logged out. [TIMEOUT]";
    		}
		} else {
        	serverMessage="You are not currently logged in. Please Log In and try again";
        }
    	return serverMessage;
    }
    
    
	private static void sendTCPMessage(String msg, Socket clientSocket) throws IOException {
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter outputWriter = new PrintWriter(output, true);
		
		outputWriter.println(msg);
	}
	
	private static List<User> getOnlineUsers() {
		return onlineUsers;
	}
	
    public static void initUsersFromCredentialsTXT() {
		String loginInfo = "";
    	String existingUsername = "";
    	String existingPassword = "";
    	
    	try {
			Scanner scanner = new Scanner(new File("server/credentials.txt"));
			while (scanner.hasNextLine()) {
				loginInfo = scanner.nextLine();
				String[] loginInfoArr = loginInfo.split(" ");
				existingUsername = loginInfoArr[0];
				existingPassword = loginInfoArr[1];
				
				User newUser = new User(existingUsername, existingPassword);
				users.add(newUser);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
} // end TCPServer class
