package server;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import client.CommandEnum;
import client.CommandInterface;
import client.User;

public class ServerThread extends Thread {
	static int UPDATE_INTERVAL = 1*1000; //milliseconds
	
	private SocketAddress sAddr = null;
	private Socket connectionSocket;
    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static List<User> users = new ArrayList<User>();
    static List<User> onlineUsers = new ArrayList<User>();
    
    static int blockDuration;
    static int timeout;
	
	ServerThread(Socket socket, List<SocketAddress> clients, List<User> users, List<User> onlineUsers, int blockDuration, int timeout) throws SocketException {
		this.connectionSocket = socket;
		ServerThread.clients = clients;
		ServerThread.users = users;
		ServerThread.onlineUsers = onlineUsers;
		ServerThread.blockDuration = blockDuration;
		ServerThread.timeout = timeout;
		this.sAddr = socket.getRemoteSocketAddress();
	}
	
	@Override
	public void run() {
		while (true) {
			boolean sendFlag = true;
			
			if (!connectionSocket.isClosed()) {
				BufferedReader inFromClient = null;
				String receivedString = null;
				String serverMessage = null;
				
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
				
				// Ensure that when user gets timedout, the input stream doesn't process null
				if (receivedString == null) {
					System.out.println("processed null on server. Execution ended gracefully");
					break;
				}
				
		        // Process received Strings
				receivedString = receivedString.trim();
		        String[] receiveIcmd = receivedString.split(" ");
		        String cmdType = receiveIcmd[0];
		        
		        // Debugging, and for tracking Server Executions
//	            System.out.println("CMDTYPE: " + cmdType);
	            
	        	if (cmdType == null) {
	        		continue;
				} else if(cmdType.equals("REQLOGIN")){
		        	// Check empty requests
		        	if (receiveIcmd.length > 2) {
		            	switch (attemptLogin(receiveIcmd[1], receiveIcmd[2], sAddr)) {
		            	case 0:
		            		serverMessage="Login Failed - User does not exist";
		            		break;
		            	case 1:
		            		clients.add(sAddr);
		                	serverMessage="Login Success";
		                	break;
		            	case 2:
		            		serverMessage="Login Failed - Incorrect Password";
		            		break;
		            	case 3:
		            		serverMessage="Your account is blocked due to multiple login failures. Please try again later";
		            		break;
		            	case 4:
		            		serverMessage = "Login Failed - User is already logged in from another location";
		            		break;
		            	default:
		            		serverMessage = "Login Failed";
		            		break;
		            	}
		        	} else {
		        		serverMessage = "Login Failed - Username and Password cannot be empty";
		        	}
		        } 
				else if(cmdType.equals("REQLOGOUT")){
		            serverMessage = attemptLogout(sAddr);
		            if (serverMessage.equals("Logout Success")) {
		            	try {
							connectionSocket.close();
							break;
						} catch (IOException e) {
							e.printStackTrace();
						}
		            }
		        }	
				else if(cmdType.equals("REQWHOELSE")){
	            	CommandInterface Icmd = new CommandInterface(CommandEnum.GIVEWHOELSE);
	            	List<User> otherOnlineUsersList = getOtherOnlineUsers(sAddr);
	            	for (User user : otherOnlineUsersList) {
	            		Icmd.addArg(user.getUserName());
	            	}
	            	
			        serverMessage = Icmd.toString();
				}
	            else if (cmdType.equals("REQWHOELSESINCE")) {
	            	int time =  Integer.parseInt(receiveIcmd[1]);
	            	
	            	CommandInterface Icmd = new CommandInterface(CommandEnum.GIVEWHOELSESINCE);
	            	List<User> otherOnlineUsersList = getOtherUsers(sAddr); // change to users
	            	for (User user : otherOnlineUsersList) {
	            		// calculate time diff
	            		LocalTime userLastLoginTime = user.getLastLoggedIntime();
	            		LocalTime currentTime = LocalTime.now();
	            		
	            		if (SECONDS.between(userLastLoginTime, currentTime) < time) {
	            			Icmd.addArg(user.getUserName());
	            		}
	            	}
	            	
	            	
			        serverMessage = Icmd.toString();
	            }
	            else if(cmdType.equals("BROADCASTLOGIN")) {
	            	User userTarget = null;
	        		User senderUser = getUserFromUsersBySAddr(sAddr);
	        		
	        		senderUser.setClientServerPort(Integer.parseInt(receiveIcmd[1]));
	            	
	            	for (int j=0; j < clients.size();j++){
	            		if (!(sAddr.equals(clients.get(j)))) {
	            			userTarget = getUserFromUsersBySAddr(clients.get(j));
	            			
	                		if (!senderUser.getBlockedUserList().contains(userTarget)) {
	    	            		String message = ("SYSTEM: " + getOnlineUserBySAddr(sAddr).getUserName() + " HAS LOGGED ON!");
	    	            		try {sendTCPMessage(message, userTarget.getSocket());} catch (IOException e) {e.printStackTrace();}	
	                		}
	            		}
	            	}
	            	sendFlag = false;
	            }
	            else if(cmdType.equals("BROADCASTLOGOUT")) { 
	            	User userTarget = null;
	        		User senderUser = getUserFromUsersBySAddr(sAddr);
	            	
	            	for (int j=0; j < clients.size();j++){
	            		if (!(sAddr.equals(clients.get(j)))) {
	            			userTarget = getUserFromUsersBySAddr(clients.get(j));
	            			
	            			if (!senderUser.getBlockedUserList().contains(userTarget)) {
			            		String message = ("SYSTEM: " + getOnlineUserBySAddr(sAddr).getUserName() + " HAS LOGGED OUT!");
			            		try {sendTCPMessage(message, userTarget.getSocket());} catch (IOException e) {e.printStackTrace();}
	            			}
	            		}
	            	}
	            	sendFlag = false;
	        	}
	            else if (cmdType.equals("MESSAGEUSER")) {
	        		String msgTarget = receiveIcmd[1];
	        		User userTarget = getUserFromUsersByName(msgTarget);
	        		User senderUser = getUserFromUsersBySAddr(sAddr);

	        		if (userTarget.getBlockedUserList().contains(senderUser)) {
	        			serverMessage = "SYSTEM: Your message could not be delivered as the recipient has blocked you";
	        		} else {
		        		List<String> msgContent = new ArrayList<String>();
		        		
		        		boolean tempflag = false;
		        		
		        		for (String s : receiveIcmd) {
		        			if (s.equals(receiveIcmd[2])) {
		        				tempflag = true;
		        			}
		        			
		        			if (tempflag == true) {
		        				msgContent.add(s);
		        			}
		        		}
		        		sendMessageToUser(msgTarget, msgContent, sAddr);
		            
		        		sendFlag = false;
	        		}
	        	}
	            else if (cmdType.equals("BROADCASTMESSAGE")) {
	        		List<String> msgContent = new ArrayList<String>();
	        		User userTarget = null;
	        		User senderUser = getUserFromUsersBySAddr(sAddr);
	        		
	        		boolean tempflag = false;
	        		
	        		for (String s : receiveIcmd) {
	        			if (s.equals(receiveIcmd[1])) {
	        				tempflag = true;
	        			}
	        			
	        			if (tempflag == true) {
	        				msgContent.add(s);
	        			}
	        		}
	        		
	            	sendFlag = false;
	        		
	            	for (int j=0; j < clients.size();j++){
	            		if (!(sAddr.equals(clients.get(j)))) {
	            			userTarget = getUserFromUsersBySAddr(clients.get(j));
	            			
	                		if (userTarget.getBlockedUserList().contains(senderUser)) {
	                			serverMessage = "SYSTEM: Your message could not be delivered to some recipients";
	                			sendFlag = true;
	                		} else {
	                			sendMessageToUser(getOnlineUserBySAddr(clients.get(j)).getUserName(), msgContent, sAddr);
	                		}
	            		}
	            	}
	        	}
	            else if (cmdType.equals("CHECKOFFLINEMSG")) {
	        		User user = getOnlineUserBySAddr(sAddr);
	        		List<String> offlineMsgList = user.getAllOfflineMsg();
	        		
	        		if (user.getAllOfflineMsg().size() >= 1) {
	        			serverMessage = "SYSTEM: While user was offline; the following messages has been received:, ";
	        			
	        			for (String s : offlineMsgList) {
	        				serverMessage = (serverMessage + "> " + s + " , ");
	        			}
	        		
	        			// Reset Offline Message Queue
	        			user.resetOfflineMessageQueue();
	        		} else {
	        			sendFlag = false;
	        		}
	        	}
	            else if(cmdType.equals("BLOCKUSER")) {
	        		User targetUser = getUserFromUsersByName(receiveIcmd[1]);
	        		User senderUser = getUserFromUsersBySAddr(sAddr);
	        		
	        		// target user does not exist
	        		if (targetUser == null) {
	        			serverMessage = "BLOCKREPLY " + "Error. Cannot block a non existing user";
	        		} else {
	        			if (targetUser.equals(senderUser)) {
	        				serverMessage = "BLOCKREPLY " + "Error. Cannot block self.";
	        			} else {
	        				senderUser.blockUser(targetUser);
	        				serverMessage = "BLOCKREPLY " + targetUser.getUserName() + " has been blocked";
	        			}
	        		}
	        	} else if(cmdType.equals("UNBLOCKUSER")) {
	        		User targetUser = getUserFromUsersByName(receiveIcmd[1]);
	        		User senderUser = getUserFromUsersBySAddr(sAddr);
	        		
	        		// target user does not exist
	        		if (targetUser == null) {
	        			serverMessage = "UNBLOCKREPLY " + "Error. Cannot unblock a non existing user";
	        		} else {
	        			if (targetUser.equals(senderUser)) {
	        				serverMessage = "UNBLOCKREPLY " + "Error. Cannot unblock self.";
	        			} else {
	        				if (senderUser.getBlockedUserList().contains(targetUser)) {
	        					senderUser.removeBlockUser(targetUser);
	        					serverMessage = "UNBLOCKREPLY " + targetUser.getUserName() + " has been unblocked";
	        				} else {
	        					serverMessage = "UNBLOCKREPLY " + targetUser.getUserName() + " was not blocked";
	        				}
	        			}
	        		}
	        	} 
	        	else if(cmdType.equals("REQINITPRIVATE")) {
	            	User targetUser = getUserFromUsersByName(receiveIcmd[1]);
	            	User sentUser = getUserFromUsersBySAddr(sAddr);
	            	String msgData;
	        		
	            	if (targetUser != null) {
	            		if(!(targetUser.equals(sentUser))) {
	        		    	if (targetUser.getIsOnline()) {
	        		    		if (!targetUser.getBlockedUserList().contains(sentUser)) {
	        		            	CommandInterface Icmd = new CommandInterface(CommandEnum.GIVEINITPRIVATE);
	        		            	
	        		            	Socket sSender = sentUser.getSocket();
	        		            	Socket sTarget = targetUser.getSocket();
	        		            	
	        		            	// Add sender name
	        		            	Icmd.addArg(sentUser.getUserName());
	        		            	// Add sender IP
	        		            	Icmd.addArg(sSender.getInetAddress().toString());
	        		            	// Add sender port
	        		            	Icmd.addArg(Integer.toString(sentUser.getClientServerPort()) );	
	        		            	
	        		            	// Add target name
	        		            	Icmd.addArg(targetUser.getUserName());
	        		            	// Add target IP address
	        		            	Icmd.addArg(sTarget.getInetAddress().toString());
	        		            	// Add target server port number
	        		            	Icmd.addArg(Integer.toString(targetUser.getClientServerPort()));
	        		            	
	        		            	int counter = 0;
	        		            	
	        		            	// Init private message between sender and receiver
	        		            	while (counter < 2) {
	        		            		if (counter == 0) {
	    	        		            	// Add display message flag
	    	        		            	Icmd.addArg("true");
	        		            			try {sendTCPMessage(Icmd.toString(), sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	        		            		} else if (counter == 1) {
	        		            			Icmd.updateArg(6, "false");
	        		            			try {sendTCPMessage(Icmd.toString(), targetUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	        		            		}
	        		            		
	        		            		counter++;
	        		            	}
	        		            	
	        		    		} else {
	        		    			msgData = "SYSTEM: Could not initiate private messaging with target user. User may have blocked you.";
	        		    			try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	        		    		}
	        		    	} else {
	        		    		// User is not online
	        		    		msgData = "SYSTEM: " + targetUser.getUserName() + " is not currently Online. Failed to start private chat.";
	        		    		
	        		    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	        		    	}
	            		} else {
	            			// User is not online
	        	    		msgData = "SYSTEM: User cannot start private chat with oneself. Use 'whoelse' to see list of users online currently.";
	        	    		
	        	    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	            		}
	            	} else {
	            		msgData = "SYSTEM: Target user does not exist.";
	            		
	            		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	            	}
	            	
	            	sendFlag = false;
	        	}
	        	else if(cmdType.equals("REQSTOPINIT")) {
	            	User targetUser = getUserFromUsersByName(receiveIcmd[1]);
	            	User sentUser = getUserFromUsersBySAddr(sAddr);
	            	String msgData;
	        		
	            	if (targetUser != null) {
	            		if(!(targetUser.equals(sentUser))) {
	        		    	if (targetUser.getIsOnline()) {
	        		    		
        		            	CommandInterface Icmd = new CommandInterface(CommandEnum.GIVESTOPINIT);
        		            	
        		            	Socket sSender = sentUser.getSocket();
        		            	Socket sTarget = targetUser.getSocket();
        		            	
        		            	// Add sender name
        		            	Icmd.addArg(sentUser.getUserName());
        		            	// Add sender IP
        		            	Icmd.addArg(sSender.getInetAddress().toString());
        		            	// Add sender port
        		            	Icmd.addArg(Integer.toString(sentUser.getClientServerPort()) );	
        		            	
        		            	// Add target name
        		            	Icmd.addArg(targetUser.getUserName());
        		            	// Add target IP address
        		            	Icmd.addArg(sTarget.getInetAddress().toString());
        		            	// Add target server port number
        		            	Icmd.addArg(Integer.toString(targetUser.getClientServerPort()));
        		            	
        		            	int counter = 0;
        		            	
        		            	// Init private message between sender and receiver
        		            	while (counter < 2) {
        		            		if (counter == 0) {
    	        		            	// Add display message flag
    	        		            	Icmd.addArg("true");
        		            			try {sendTCPMessage(Icmd.toString(), sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
        		            		} else if (counter == 1) {
        		            			Icmd.updateArg(6, "false");
        		            			try {sendTCPMessage(Icmd.toString(), targetUser.getSocket());} catch (IOException e) {e.printStackTrace();}
        		            		}
        		            		
        		            		counter++;
        		            	}

	        		    	} else {
	        		    		// User is not online
	        		    		msgData = "SYSTEM: " + targetUser.getUserName() + " is not currently Online. Failed to start private chat.";
	        		    		
	        		    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	        		    	}
	            		} else {
	            			// User is not online
	        	    		msgData = "SYSTEM: User cannot start private chat with oneself. Use 'whoelse' to see list of users online currently.";
	        	    		
	        	    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	            		}
	            	} else {
	            		msgData = "SYSTEM: Target user does not exist.";
	            		
	            		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
	            	}
	            	
	            	sendFlag = false;
	        	}
	        	else {
	                serverMessage="Server: Unknown command, login and refer to --help";
	            }
		        
	        	if (sendFlag == true) {
	        		try {sendTCPMessage(serverMessage, connectionSocket);} catch (IOException e) {e.printStackTrace();}	
	        	}	
	        	
	        	// Reset User Timeout
	            ResetUserTimeout(sAddr);
			} // end while
		}
	}
	
	private void sendTCPMessage(String msg, Socket clientSocket) throws IOException {
		OutputStream output = clientSocket.getOutputStream();
		PrintWriter outputWriter = new PrintWriter(output, true);
		
		outputWriter.println(msg);
	}
	
	// List of Functions
	/**
	 * 
	 * @param username
	 * @param pass
	 * @param sAddr
	 * @return an integer (0 - fail, 1 - success, 2 - invalid pass, 3 - blocked)
	 */
    public int attemptLogin(String username, String pass, SocketAddress sAddr) {
    	
    	int loginSuccess = 0;
    	
    	// Check If User exists, and if so, check validity of password
    	for (User user : users) {
    		if (user.getUserName().equals(username)) {
    			if(!user.getIsBlocked()) {
	    			if (user.getPass().equals(pass)) {
	    				if (getOnlineUserByName(username) == null) {
	    					loginSuccess = 1;
	    					LoginUser(user, sAddr);
	    					break;
	    				} else {
		    				loginSuccess = 4;
		    				break;
	    				}
	    			} else {
	    				loginSuccess = 2;
	    				if (getOnlineUserByName(username) == null) {
	    					FailedLoginUser(user, sAddr);
	    				}
	    				
	    		    	if (user.getIsBlocked()) {
	    		    		loginSuccess = 3;
	    		    	} 
	    				break;
	    			}
    			} else {
    				loginSuccess = 3;
    				break;
    			}
    		}
    	}
    	
    	return loginSuccess;
	}
    
    public String attemptLogout(SocketAddress sAddr) {
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
    
    public void LoginUser(User user, SocketAddress sAddr) {
    	user.setSocket(connectionSocket);
		user.setUserToOnline(timeout);
		user.setsAddr(sAddr);
		onlineUsers.add(user);
    }
    
    public void FailedLoginUser(User user, SocketAddress sAddr) {		
    	user.incrNumLoginAttempt(blockDuration);
    }
	
    private List<User> getOtherOnlineUsers(SocketAddress addr) {
    	List<User> usersList = new ArrayList<User>();
    	
    	for (User user : onlineUsers) {
    		if (!user.getsAddr().equals(addr)) {
    			usersList.add(user);
    		}
    	}
    	
    	return usersList;
    }
    
	private List<User> getOtherUsers(SocketAddress addr) {
    	List<User> usersList = new ArrayList<User>();
    	
    	for (User user : users) {
    		if (!(user.getLastLoggedIntime() == null)) {
	    		if (!user.getsAddr().equals(addr)) {
	    			usersList.add(user);
	    		}
    		}
    	}
    	
    	return usersList;
	}
	
    public User getOnlineUserByName(String username) {
    	for (User ExistingUser : onlineUsers) {
    		if (ExistingUser.getUserName().equals(username)) {
    			return ExistingUser;
    		}
    	}
    	
    	return null;
    }
    
    public User getOnlineUserBySAddr(SocketAddress addr) {
    	for (User ExistingUser : onlineUsers) {
    		if (ExistingUser.getsAddr().equals(addr)) {
    			return ExistingUser;
    		}
    	}
    	
    	return null;
    }
	
    private User getUserFromUsersBySAddr(SocketAddress addr) {
    	for (User user : users) {
    		if (user.getsAddr() != null) {
	    		if (user.getsAddr().equals(addr)) {
	    			return user;
	    		}
    		}
    	}
    	
    	return null;
    }
    
    private User getUserFromUsersByName(String name) {
    	for (User user : users) {
    		if (user.getUserName().equals(name)) {
    			return user;
    		}
    	}
    	
    	return null;
    }
    
    public void sendMessageToUser(String msgTarget, List<String> msgContent, SocketAddress sAddrOfSender) {
    	// Get the target User
    	User targetUser = getUserFromUsersByName(msgTarget);
    	User sentUser = getUserFromUsersBySAddr(sAddrOfSender);
    	String msgData = "MESSAGEREPLY " + sentUser.getUserName() + buildStringFromStringList(msgContent);
    	
    	if (targetUser != null) {
    		if(!(targetUser.equals(sentUser))) {
		    	if (targetUser.getIsOnline()) {
		    		try {sendTCPMessage(msgData, targetUser.getSocket());} catch (IOException e) {e.printStackTrace();}
		    	} else {
		    		msgData = sentUser.getUserName() + buildStringFromStringList(msgContent);
		    		targetUser.addToOfflineMsgQueue(msgData);
		    		
		    		// User is not online
		    		msgData = "SYSTEM: Target user is not currently Online. The message will be sent when the user becomes online.";
		    		
		    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
		    	}
    		} else {
    			// User is not online
	    		msgData = "SYSTEM: User cannot send message to oneself. Use 'whoelse' to see list of users online currently.";
	    		
	    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
    		}
    	} else {
    		msgData = "SYSTEM: Target user does not exist.";
    		
    		try {sendTCPMessage(msgData, sentUser.getSocket());} catch (IOException e) {e.printStackTrace();}
    	}
    }
    
    public String buildStringFromStringList(List<String> msgList) {
    	String msg = "";
    	for (String s : msgList) {
    		msg = msg + " " + s;
    	}
    	
    	return msg;
    }
    
	public void ResetUserTimeout(SocketAddress sAddr) {
		User user = getUserFromUsersBySAddr(sAddr);
		if (!(user == null) && user.getIsOnline()) {
			user.scheduleTimeoutTimer(timeout);	
		}
	}
} // end of class
