package IC.SymbolTable;

import IC.TypeTable.*;
import IC.AST.Field;

/**
 * Field symbol class.
 *
 */
public class FieldSymbol extends Symbol {
	
	public FieldSymbol(Field fld) throws SemanticError {
		super(fld.getLine(), fld.getName(), Kind.FIELD, TypeTable.getType(fld.getType().toString()));
	}
	
	@Override
	public String toString() {
		
		return ("Field: " + getType().toString() + " " + getID());
	}
	
}
