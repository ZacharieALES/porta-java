package exception;

@SuppressWarnings("serial")
public class UnknownCommandException extends Exception{
	
	String commandName;
	
	public UnknownCommandException(String name) {
		this.commandName = name;
	}

}
