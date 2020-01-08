package server;

import java.net.InetAddress;
import java.net.Socket;

public class PrivateUser {
	String userName;
	InetAddress IPAddress;
	int serverPort;
	Socket socket;
	PrivateUser pairedUser;
	
	public PrivateUser(String userName, InetAddress IPAddress, int serverPort) {
		this.userName = userName;
		this.IPAddress = IPAddress;
		this.serverPort = serverPort;
	}
	
	public void setPairedUser(PrivateUser user) {
		this.pairedUser = user;
	}
	
	public PrivateUser getPairedUser() {
		return this.pairedUser;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public InetAddress getIPAddress() {
		return IPAddress;
	}

	public void setIPAddress(InetAddress iPAddress) {
		IPAddress = iPAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	
}
