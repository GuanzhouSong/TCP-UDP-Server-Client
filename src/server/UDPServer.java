/*
 * Threaded UDPServer
 * Compile: javac UDPServer.java
 * Run: java UDPServer PortNo
 */

package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.concurrent.locks.*;
import static java.time.temporal.ChronoUnit.SECONDS;

import client.CommandEnum;
import client.CommandInterface;
import client.User;


public class UDPServer extends Thread{

    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static byte[] sendData = new byte[1024];
    static DatagramSocket serverSocket;
    static int UPDATE_INTERVAL = 1000;//milliseconds
    static ReentrantLock syncLock = new ReentrantLock();
    static List<User> users = new ArrayList<User>();
    static List<User> onlineUsers = new ArrayList<User>();
    static int blockDuration;
    static int timeout;
    static Timer timer;
    
	public static void main(String[] args)throws Exception {
        //Assign Port no
        int serverPort = Integer.parseInt(args[0]);
        serverSocket = new DatagramSocket(serverPort);
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
        
		UDPServer us = new UDPServer();
		us.start();
		
	} // end of main()
	
	public void ResetUserTimeout(SocketAddress sAddr) {
		User user = getUserFromUsersBySAddr(sAddr);
		if (!(user == null) && user.getIsOnline()) {
			user.scheduleTimeoutTimer(timeout);	
		}
	}
	
	// We will send from this thread
    public void run(){
		//prepare buffers
		String sentence = null;
		byte[] receiveData = null;
		String serverMessage = null;
		SocketAddress sAddr = null;
    	
        while(true) {
        	// flag to determine if default sending is on
        	boolean sendFlag = true;
        	
            //receive UDP data
        	receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try { serverSocket.receive(receivePacket); } catch (IOException e) { e.printStackTrace(); }
            
         	//get data
            sentence = new String(receivePacket.getData());
            //Need only the data received not the spaces till size of buffer
            sentence=sentence.trim();
            String[] receiveIcmd = sentence.split(" ");
            String cmdType = receiveIcmd[0];
            System.out.println("cmdType: " + cmdType);
            // Get client's socket address
            sAddr = receivePacket.getSocketAddress();
            
            // Process user requests
            if(cmdType.equals("REQLOGIN")){
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
            }
            else if(cmdType.equals("REQWHOELSE")) {
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
            	
            	for (int j=0; j < clients.size();j++){
            		if (!(sAddr.equals(clients.get(j)))) {
            			userTarget = getUserFromUsersBySAddr(clients.get(j));
            			
                		if (!userTarget.getBlockedUserList().contains(senderUser)) {
    	            		String message = ("SYSTEM: " + getOnlineUserBySAddr(sAddr).getUserName() + " HAS LOGGED ON!");
    	            		sendData = message.getBytes();
    	            		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clients.get(j));
    	            		try{         		
    	        				serverSocket.send(sendPacket);
    	        			} catch (IOException e){ }
                		}
            		}
            	}
            	sendFlag = false;
            } else if(cmdType.equals("BROADCASTLOGOUT")) { 
            	User userTarget = null;
        		User senderUser = getUserFromUsersBySAddr(sAddr);
            	
            	for (int j=0; j < clients.size();j++){
            		if (!(sAddr.equals(clients.get(j)))) {
            			userTarget = getUserFromUsersBySAddr(clients.get(j));
            			
            			if (!userTarget.getBlockedUserList().contains(senderUser)) {
		            		String message = ("SYSTEM: " + getOnlineUserBySAddr(sAddr).getUserName() + " HAS LOGGED OUT!");
		            		sendData = message.getBytes();
		            		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clients.get(j));
		            		try{         		
		        				serverSocket.send(sendPacket);
		        			} catch (IOException e){ }
            			}
            		}
            	}
            	sendFlag = false;
        	} else if (cmdType.equals("MESSAGEUSER")) {
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
        	} else if (cmdType.equals("BROADCASTMESSAGE")) {
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
        	} else if (cmdType.equals("CHECKOFFLINEMSG")) {
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
        	} else if(cmdType.equals("BLOCKUSER")) {
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
        	else {
                serverMessage="Server: Unknown command, login and refer to --help";
            }
            
            //prepare to send reply back
            sendData = serverMessage.getBytes();
            
            //send it back to client on SocktAddress sAddr
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, sAddr);
            
            if (sendFlag == true) {
            	try { serverSocket.send(sendPacket); } catch (IOException e) { e.printStackTrace(); }
            }
            
            // Reset User Timeout
            ResetUserTimeout(sAddr);
        }
    } //run ends
    
    public void sendMessageToUser(String msgTarget, List<String> msgContent, SocketAddress sAddrOfSender) {
    	DatagramPacket sendPacket;
    	
    	// Get the target User
    	User targetUser = getUserFromUsersByName(msgTarget);
    	User sentUser = getUserFromUsersBySAddr(sAddrOfSender);
    	
    	String msgData = "MESSAGEREPLY " + sentUser.getUserName() + buildStringFromStringList(msgContent);
    	
    	if (targetUser != null) {	
    		if(!(targetUser.equals(sentUser))) {
		    	if (targetUser.getIsOnline()) {	
			    	// Get the sAddr of target user
			        SocketAddress sAddr = targetUser.getsAddr();
			        
			    	//prepare to send reply back
			        sendData = msgData.getBytes();
			        
			        // Forward message to message target
			        sendPacket = new DatagramPacket(sendData, sendData.length, sAddr);
		    	} else {
		    		msgData = sentUser.getUserName() + buildStringFromStringList(msgContent);
		    		targetUser.addToOfflineMsgQueue(msgData);
		    		
		    		// User is not online
		    		msgData = "SYSTEM: Target user is not currently Online. The message will be sent when the user becomes online.";
		    		
		        	//prepare to send reply back
		            sendData = msgData.getBytes();
		            
		            //send it back to client on SocktAddress sAddr
		            sendPacket = new DatagramPacket(sendData, sendData.length, sAddrOfSender);
		    	}
    		} else {
    			// User is not online
	    		msgData = "SYSTEM: User cannot send message to oneself. Use 'whoelse' to see list of users online currently.";
	    		
	        	//prepare to send reply back
	            sendData = msgData.getBytes();
	            
	            //send it back to client on SocktAddress sAddr
	            sendPacket = new DatagramPacket(sendData, sendData.length, sAddrOfSender);
    		}
    	} else {
    		msgData = "SYSTEM: Target user does not exist.";
    		
        	//prepare to send reply back
            sendData = msgData.getBytes();
            
            //send it back to client on SocktAddress sAddr
            sendPacket = new DatagramPacket(sendData, sendData.length, sAddrOfSender);
    	}
    	
    	
        try { serverSocket.send(sendPacket); } catch (IOException e) { e.printStackTrace(); }
    }
    
    
    public String buildStringFromStringList(List<String> msgList) {
    	String msg = "";
    	for (String s : msgList) {
    		msg = msg + " " + s;
    	}
    	
    	return msg;
    }
    
	/**
	 * 
	 * @param username
	 * @param pass
	 * @param sAddr
	 * @return an integer (0 - fail, 1 - success, 2 - invalid pass, 3 - blocked)
	 */
    public static int attemptLogin(String username, String pass, SocketAddress sAddr) {
    	
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
    
    public static void LoginUser(User user, SocketAddress sAddr) {
		user.setUserToOnline(timeout);
		user.setsAddr(sAddr);
		onlineUsers.add(user);
    }
    
    public static void FailedLoginUser(User user, SocketAddress sAddr) {		
    	user.incrNumLoginAttempt(blockDuration);
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
    
    public static User getOnlineUserByName(String username) {
    	for (User ExistingUser : onlineUsers) {
    		if (ExistingUser.getUserName().equals(username)) {
    			return ExistingUser;
    		}
    	}
    	
    	return null;
    }
    
    public static User getOnlineUserBySAddr(SocketAddress addr) {
    	for (User ExistingUser : onlineUsers) {
    		if (ExistingUser.getsAddr().equals(addr)) {
    			return ExistingUser;
    		}
    	}
    	
    	return null;
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
    
    private static List<User> getOtherOnlineUsers(SocketAddress addr) {
    	List<User> usersList = new ArrayList<User>();
    	
    	for (User user : onlineUsers) {
    		if (!user.getsAddr().equals(addr)) {
    			usersList.add(user);
    		}
    	}
    	
    	return usersList;
    }
    
    private static User getUserFromUsersBySAddr(SocketAddress addr) {
    	for (User user : users) {
    		if (user.getsAddr() != null) {
	    		if (user.getsAddr().equals(addr)) {
	    			return user;
	    		}
    		}
    	}
    	
    	return null;
    }
    
    private static User getUserFromUsersByName(String name) {
    	for (User user : users) {
    		if (user.getUserName().equals(name)) {
    			return user;
    		}
    	}
    	
    	return null;
    }
    
	private static List<User> getOtherUsers(SocketAddress addr) {
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
	
	private static List<User> getOnlineUsers( ) {
		return onlineUsers;
	}
    
    private static TimerTask createTrackTimeout() {
    	return new TimerTask() {
    		@Override
		    public void run() {
    			List<User> users = getOnlineUsers();
		    	for (User user : users) {
		    		if (user.getHasTimedOut() == true) {
		    			String sentence = attemptLogout(user.getsAddr());
		    			
		    			String logoutUserTimeOut = "USER TIMEOUT";
		    			sendData = logoutUserTimeOut.getBytes();
	            		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, user.getsAddr());
	            		try{         		
	        				serverSocket.send(sendPacket);
	        			} catch (IOException e){ }
		    			
		            	for (int j=0; j < clients.size();j++){
		            		sendData = sentence.getBytes();
		            		DatagramPacket sendPacket2 = new DatagramPacket(sendData, sendData.length, clients.get(j));
		            		try{         		
		        				serverSocket.send(sendPacket2);
		        			} catch (IOException e){ }
		            	}
		    			break;
		    		}
		    	}
		    }
    	};
    }
    
} // end of class UDPServer
