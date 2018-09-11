package exception;

@SuppressWarnings("serial")
public class UnknownCommandException extends Exception{
		
	public UnknownCommandException(String name) {
		super("Unable to find the command \"" + name + "\" in your path.");
	}

}
