package lpterms;

public abstract class AbstractTerm {
	
	public String expression;
	
	public AbstractTerm(String expression) {
		this.expression = expression;
	}
	
	@Override
	public String toString() {return expression;}

}
