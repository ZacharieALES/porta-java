package exception;

public class UnknownVariableName extends Exception{

	private static final long serialVersionUID = -1462577144602765382L;
		
	public UnknownVariableName(String variableName){
		super("Unregistered variable name: \"" + variableName + "\"");
	}

}
