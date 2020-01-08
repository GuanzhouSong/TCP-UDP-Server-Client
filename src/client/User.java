package client;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer; 
import java.util.TimerTask; 
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;    

public class User {
	private String userName;
	private String pass;
	private SocketAddress sAddr = null;
	private boolean hasTimedOut;
	private Boolean isOnline;
	private int numLoginAttempt;
	private Boolean isBlocked;
	private LocalTime lastLoggedInTime;
	Timer timer;
	private Socket socket;
	private List<User> blockedUsers;
	private List<String> offlineMessageQueue;
	private List<User> privateMessageUsers;
	private int ClientServerPort;
	
	public User(String userName, String pass) {
		this.userName = userName;
		this.pass = pass;
		this.isOnline = false;
		this.numLoginAttempt = 0;
		this.isBlocked = false;
		this.lastLoggedInTime = null;
		this.hasTimedOut = false;
		this.blockedUsers = new ArrayList<User>();
		this.offlineMessageQueue = new ArrayList<String>();
		this.privateMessageUsers = new ArrayList<User>();
	}
	
	public int getClientServerPort() {
		return ClientServerPort;
	}

	public void setClientServerPort(int clientServerPort) {
		ClientServerPort = clientServerPort;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public List<User> getPrivateMessageUsers() {
		return privateMessageUsers;
	}

	public void addPrivateMessageUsers(User u) {
		this.privateMessageUsers.add(u);
	}
	
	public void rmvPrivateMessageUsers(User u) {
		this.privateMessageUsers.remove(u);
	}

	public void addToOfflineMsgQueue(String msg) {
		this.offlineMessageQueue.add(msg);
	}
	
	public List<String> getAllOfflineMsg() {
		return this.offlineMessageQueue;
	}
	
	public void resetOfflineMessageQueue() {
		this.offlineMessageQueue.clear();
	}
	
	public void blockUser(User user) {
		this.blockedUsers.add(user);
	}
	
	public void removeBlockUser(User user) {
		this.blockedUsers.remove(user);
	}
	
	public List<User> getBlockedUserList() {
		return this.blockedUsers;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public SocketAddress getsAddr() {
		return sAddr;
	}

	public void setsAddr(SocketAddress sAddr) {
		this.sAddr = sAddr;
	}
	
	public Boolean getIsOnline() {
		return isOnline;
	}
	
	public LocalTime getLastLoggedIntime() {
		return this.lastLoggedInTime;
	}

	public void setUserToOnline(int timeout) {
		if (this.isOnline == false) {	
			this.lastLoggedInTime = LocalTime.now();
			timer = new Timer();
			timer.schedule(createTrackTimeout(), timeout);
		}
		
		this.hasTimedOut = false;
		this.isOnline = true;
	}
	
	public void scheduleTimeoutTimer(int timeout) {
		timer.cancel();
		timer = new Timer();
		timer.schedule(createTrackTimeout(), timeout);
	}
	
	public void setUserToOffline() {
		if (this.isOnline == true) {
			timer.cancel();
			timer.purge();
		}
		
		this.isOnline = false;
	}

	public int getNumLoginAttempt() {
		return numLoginAttempt;
	}

	public void setNumLoginAttempt(int numLoginAttempt) {
		this.numLoginAttempt = numLoginAttempt;
	}
	
	public void incrNumLoginAttempt(int blockDuration) {
		this.numLoginAttempt++;
		if (this.numLoginAttempt == 3) {
			this.setAcctBlocked(true, blockDuration);
			System.out.println(getUserName() + "is now blocked");
		}
	}
	
	public Boolean isAcctBlocked() {
		return this.isBlocked;
	}
	
	public void setAcctBlocked(Boolean b, int blockDuration) {
		if (b == true) {
			// From: https://stackoverflow.com/questions/2258066/java-run-a-function-after-a-specific-number-of-seconds
			new java.util.Timer().schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			            	System.out.println(getUserName() + "is now unblocked");
			            	setAcctBlocked(false, blockDuration);
			            	this.cancel();
			            }
			        }, 
			        blockDuration
			);
		}
		
		this.isBlocked = b;
	}

	public Boolean getIsBlocked() {
		return isBlocked;
	}

	public void setIsBlocked(Boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public void setIsOnline(Boolean isOnline) {
		this.isOnline = isOnline;
	}
	
	public boolean getHasTimedOut() {
		return this.hasTimedOut;
	}
	
	private TimerTask createTrackTimeout() {
		 return new TimerTask() {
			 @Override
			 public void run() {
		    	System.out.print("User has TIMED OUT: ");
		    	System.out.println(userName + " Is Set to Offline");
		    	hasTimedOut = true;
			 }
		 };
	}
}
