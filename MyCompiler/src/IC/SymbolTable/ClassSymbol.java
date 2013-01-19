package IC.SymbolTable;

import IC.AST.ICClass;
import IC.SemanticAnalysis.SymbolTableBuilder;
import IC.TypeTable.SemanticError;
import IC.TypeTable.TypeTable;

/**
 * Class symbol class.
 *
 */

public class ClassSymbol extends Symbol {

	private ClassSymbolTable cst;

	public ClassSymbol(ICClass c) throws SemanticError {
		
		super(c.getLine(), c.getName(), Kind.CLASS, TypeTable.getClassType(c.getName()));
		
		initClassSymbolTable(c);
		
		c.setEnclosingScope(cst.getParent());
		
	}

	/** 
	 * Initializes class symbol table.
	 *  
	 */
	private void initClassSymbolTable(ICClass c) throws SemanticError {
		
		SymbolTable parent;
		
		if (c.hasSuperClass()) {
			
			GlobalSymbolTable gst = SymbolTableBuilder.gst;   //getting global sym. table.
															
			ClassSymbol superClass = (ClassSymbol)(gst.lookup(c.getSuperClassName())); 
			if (superClass == null) {
				//super class not found.
				throw new SemanticError("Symbol cannot be resolved - super class wasn't previously defined", 
						c.getLine(), c.getSuperClassName());    
			}
				
			parent = superClass.getClassSymbolTable();
			
		} else {
			parent = SymbolTableBuilder.gst;
		}
		

		this.cst = new ClassSymbolTable(c, super.getID(), parent, this);
		parent.addChild(this.cst);
	}

	public ClassSymbolTable getClassSymbolTable() {
		return this.cst;
	}

	@Override
	public String toString() {
		return "Class: " + getID().toString();
	}

}
