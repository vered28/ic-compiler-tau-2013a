package IC.SymbolTable;

import IC.AST.LocalVariable;
import IC.AST.Formal;

import IC.TypeTable.*; 

/**
 * Local Variable and Parameter symbol class.
 *
 */

public class VarSymbol extends Symbol {
	
	private boolean isFormal;   //true iff this instance represents formal param.
	
	public VarSymbol(LocalVariable lv) throws SemanticError {
		super(lv.getLine(), lv.getName(), Kind.VARIABLE, TypeTable.getType(lv.getType().toString()));
		super.type = TypeTable.getType(lv.getType().toString()); 
		
		this.isFormal = false;
	}
	

	public VarSymbol(Formal f) throws SemanticError {
		super(f.getLine(), f.getName(), Kind.VARIABLE, TypeTable.getType(f.getType().toString()));
		super.type = TypeTable.getType(f.getType().toString());
		
		this.isFormal = true;
	}
	
	@Override
	public String toString() {
		String str="";
		
		if (isFormal) {
			str += "Parameter: ";
		} else {
			str += "Local variable: ";
		}
		
		return (str + getType().toString() + " " + getID());
	}

}
