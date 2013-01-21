package IC.AST;
import IC.LIR.*;
import IC.SymbolTable.SymbolTable;

/**
 * Break statement AST node.
 * 
 * @author Tovi Almozlino
 */
public class Break extends Statement {

	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}
	
	public Object accept(PropagatingVisitor visitor, SymbolTable context) {
		return visitor.visit(this, context);
	}
	
	public LIRUpType accept(LIRPropagatingVisitor<Integer,LIRUpType> visitor, Integer downInt) {
		return visitor.visit(this, downInt);
	}

	/**
	 * Constructs a break statement node.
	 * 
	 * @param line
	 *            Line number of break statement.
	 */
	public Break(int line) {
		super(line);
	}

}
