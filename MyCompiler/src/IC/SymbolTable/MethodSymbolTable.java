package IC.SymbolTable;

import IC.AST.LocalVariable; 
import IC.AST.Method;
import IC.AST.Formal;

import IC.TypeTable.*;

/**
 * Symbol table of method.
 *
 */

public class MethodSymbolTable extends BlockSymbolTable {
	
	public MethodSymbolTable(Method meth, SymbolTable parent) throws SemanticError {
		super(meth.getName(), parent);
		
		//adding all formal params. to method sym. table.
		for (Formal f : meth.getFormals()) {
			VarSymbol form = new VarSymbol(f);
			super.insert(form);
							
		}
		
		super.StaticScope = meth.isStatic();
		
		super.returnType = TypeTable.getType(meth.getType().toString()); //return type of method.
															
		super.enclosingClass = (ClassSymbolTable)(parent);   //CST of enclosing class.
	}


	@Override
	public String toString() {
		
		String str = super.toString();
		
		str = str.substring(str.indexOf('\n')+1);  //cutting the first line of block ST.
		return "Method Symbol Table" + ": " + getID() + "\n" + str;
	}

}
