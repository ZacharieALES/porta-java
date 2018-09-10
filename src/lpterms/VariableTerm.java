package lpterms;

import exception.UnknownVariableName;
import formulation.LPReader;
import formulation.Variable;

public class VariableTerm extends AbstractTerm{
	
	public String originalExpression;
	
	public VariableTerm(String expression, LPReader reader) {
		super(expression);

		originalExpression = expression;
		
		/* Define the variable if is not already */
		if(!reader.isDefined(expression)) {
			reader.addVariable(new Variable(expression));
//			System.out.println("Create a new variable \"" + expression + "\"");
		}
		
		try {
			this.expression = reader.portaName(expression);
		} catch (UnknownVariableName e) {
			// Should never be called 
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {return originalExpression;} 
}
