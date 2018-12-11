package formulation;

import exception.UnknownVariableName;

/**
 * Represent an integer point in a polytope
 * All the variables are set to 0 by default.
 * @author zach
 *
 */
public class IntegerPoint{
	
	int[] coordinates;
	
	AbstractPolytope polytope;
	
	public IntegerPoint(AbstractPolytope p) {
		this.polytope = p;
		coordinates = new int[p.variables.size()];
	}
	
	public void setVariable(String varName, int value) throws UnknownVariableName {
		int id = polytope.variablesBis.get(varName);
				
		if(id != -1) 
			this.coordinates[id - 1] =  value;
		else
			throw new UnknownVariableName(varName);
	}
	
	@Override
	public String toString() {
		String result = "";
		
		for(int i: coordinates)
			result += i + " ";
		
		return result;
	}
}
