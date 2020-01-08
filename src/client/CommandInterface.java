package client;

import java.util.ArrayList;
import java.util.List;

public class CommandInterface {
	private CommandEnum cmdType;
	private List<String> args;
	
	public CommandInterface(CommandEnum cmdType) {
		this.cmdType = cmdType;
		this.args = new ArrayList<>();
	}
	
	public void addArg(String arg) {
		args.add(arg);
	}
	
	public void updateArg(int i, String arg) {
		if (i < this.args.size()) {
			args.set(i, arg);
		}
	}
	
	@Override
	public String toString() { 
	    String result = this.cmdType.name();
	    for (String arg : args) {
	    	result = result + " " + arg;
	    }
	    
	    return result;
	}
}
