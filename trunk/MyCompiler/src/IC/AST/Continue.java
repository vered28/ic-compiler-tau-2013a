package IC.AST;

import IC.SymbolTable.SymbolTable;

/**
 * Continue statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class Continue extends Statement {

	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}
	
	public Object accept(PropagatingVisitor visitor, SymbolTable context) {
		return visitor.visit(this, context);
	}

	/**
	 * Constructs a continue statement node.
	 * 
	 * @param line
	 *            Line number of continue statement.
	 */
	public Continue(int line) {
		super(line);
	}

}
