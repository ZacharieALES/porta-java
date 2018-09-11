package lpterms;

/**
 * Represent a term of an lp file (ex : a variable, an operator, a coefficient, ...)
 * @author zach
 *
 */
public abstract class AbstractTerm {
	
	/** Expression of the term in the lp file */
	public String expression;
	
	public AbstractTerm(String expression) {
		this.expression = expression;
	}
	
	@Override
	public String toString() {return expression;}

}
