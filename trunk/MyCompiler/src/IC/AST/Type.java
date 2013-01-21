package IC.AST;

import IC.SymbolTable.SymbolTable;

/**
 * Abstract base class for data type AST nodes.
 * 
 * @author Tovi Almozlino
 */
public abstract class Type extends ASTNode {

	/**
	 * Number of array 'dimensions' in data type. For example, int[][] ->
	 * dimension = 2.
	 */
	private int dimension = 0;

	/**
	 * Constructs a new type node. Used by subclasses.
	 * 
	 * @param line
	 *            Line number of type declaration.
	 */
	protected Type(int line) {
		super(line);
	}

	public abstract String getName();

	public int getDimension() {
		return dimension;
	}

	public void incrementDimension() {
		++dimension;
	}
	
	
	/**
	 * Returns string rep. of type, that is used in TypeTable's mappings.
	 * 
	 */
	@Override
	public String toString() {
		
		String id = this.getName();
		
		//add needed [] if this is an array type.
        for (int i=0; i<this.dimension; i++){
        	id += "[]";
        }
          
        return id;
	}
	
	
	
}