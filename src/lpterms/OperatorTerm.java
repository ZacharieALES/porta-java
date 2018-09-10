package lpterms;

public class OperatorTerm extends AbstractTerm{

	public OperatorTerm(String expression) {
		super(expression);
		
		/* Convert the equality in porta format */
		if("=".equals(expression))
			this.expression = "==";
	}

}
