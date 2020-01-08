package client;

public enum CommandEnum {
	REQLOGIN, // Client -> Server: request login attempt
	REQLOGOUT, // Client -> Server: request logout attempt
	REQWHOELSE, // Client -> Server: request list of other online users
	REQWHOELSESINCE, // Client -> Server: request who else since last <time>
	GIVEWHOELSE, // Server -> Client: give list of other online users
	GIVEWHOELSESINCE, // Server -> Client: give list of other since <time>
	BROADCASTLOGIN, // Client -> Server: broadcast to all other users LOGIN
	BROADCASTLOGOUT, // Client -> Server: broadcast to all other users LOGOUT
	MESSAGEUSER, // Client -> Server: message server to forward to user
	MESSAGEREPLY, // Server -> Client: message client from server (forwarded message)
	BROADCASTMESSAGE, // Client -> Server: Broadcast message to all other users
	CHECKOFFLINEMSG, // Client -> Server: Check Offline message of user
	BLOCKUSER, // Client -> Server" block target user
	UNBLOCKUSER, // Client -> Server: unblock target user
	REQINITPRIVATE, // Client -> Server: request start of private
	GIVEINITPRIVATE, // Server -> Client: accept start of private
	REQSTOPINIT, // Client -> Server: Stop private 
	GIVESTOPINIT, // Server -> Client: Stop private done
	SENDPRIVATE, // Client -> Server: send private msg
	GIVEPRIVATE, // Server -> Client: accept private msg
	REFRESHTIMEOUT; // Client -> Server: refresh user timeout
	
	
	
	
	public static void main(String[] args) {
		
	}
}
