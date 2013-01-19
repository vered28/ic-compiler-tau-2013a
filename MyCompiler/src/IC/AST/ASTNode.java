package IC.AST;

import IC.SymbolTable.*;

/**
 * Abstract AST node base class.
 * 
 * @author Tovi Almozlino
 */
public abstract class ASTNode {

	private int line;

	/**
	 * will hold the number of registers required to store the value of the ASTNode
	 * (e.g. 2 registers for array location)
	 */
	private int requiredRegs;

	public int getRequiredRegs() {
		return requiredRegs;
	}

	public void setRequiredRegs(int requiredRegs) {
		this.requiredRegs = requiredRegs;
	}
	
	// Reference to symbol table of enclosing scope.
	private SymbolTable enclosingScope;

	/**
	 * Double dispatch method, to allow a visitor to visit a specific subclass.
	 * 
	 * @param visitor
	 *            The visitor.
	 * @return A value propagated by the visitor.
	 */
	public abstract Object accept(Visitor visitor);

	/**
	 * Constructs an AST node corresponding to a line number in the original
	 * code. Used by subclasses.
	 * 
	 * @param line
	 *            The line number.
	 */
	protected ASTNode(int line) {
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	/**
	 * Returns symbol table of enclosing scope.
	 * 
	 */
	public SymbolTable getEnclosingScope() {
		return this.enclosingScope;
	}

	/**
	 * Sets symbol table of enclosing scope.
	 * 
	 */
	public void setEnclosingScope(SymbolTable es) {
		this.enclosingScope = es;
	}

}
