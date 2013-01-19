package IC.SymbolTable;

import IC.AST.ICClass;
import IC.AST.Method;
import IC.AST.Field;

import IC.SemanticAnalysis.SymbolTableBuilder;

import IC.TypeTable.*;

/**
 * Symbol table of class.
 *
 */

public class ClassSymbolTable extends SymbolTable {

	private ClassSymbol myClassSymbol;  //for This semantic check.

	public ClassSymbolTable(ICClass c, String id, SymbolTable parent, ClassSymbol myCS) throws SemanticError {
		super(id, parent);
		
		this.myClassSymbol = myCS;
		
		//adding fields.
		for (Field fld : c.getFields()) {
			this.addField(fld);
		}

		//adding methods.
		for (Method meth : c.getMethods()) {
			this.addMethod(meth);
		}
	}
	
	/**
	 * Adds field to class sym. table.
	 * 
	 */
	private void addField(Field fld) throws SemanticError {
		
		Symbol sym = lookup(fld.getName());
		
		if ((sym != null) && (sym.getKind() == Kind.FIELD || sym.getKind() == Kind.METHOD)) {
			//we already have field or method with same name in this class hierarchy.
			throw new SemanticError("Multiple definitions of symbol in class hierarchy", fld.getLine(), fld.getName());
		} else {
			
			super.insert(new FieldSymbol(fld));
		}
	}
	
	/**
	 * Adds method to class sym. table.
	 * 
	 */
	private void addMethod(Method meth) throws SemanticError {
		
		Symbol sym = lookup(meth.getName());
		
		MethodSymbol meth_sym = new MethodSymbol(meth);
		
		boolean stat1;
		boolean stat2;

		if (sym != null) {  //already exists.
			
			if (sym.getKind() != Kind.METHOD) { //we have field with same name.
				throw new SemanticError("Multiple definitions for symbol in class hierarchy",meth.getLine(),meth.getName());
			}

			if ( ((MethodType)sym.getType()).equals((MethodType)meth_sym.getType()) == false ) { 
				//this is overloading, and not overriding.
				throw new SemanticError("Overloading is not supported",meth.getLine(),meth.getName());
			}

			stat1 = ((MethodSymbol)sym).isStaticMethod();
			stat2 = meth_sym.isStaticMethod();
			
			//one of them static and other not, with same signature.
			if ((stat1 && !stat2) || (!stat1 && stat2)) {
				throw new SemanticError("Overriding mix of static and dynamic methods is not supported",meth.getLine(),meth.getName());
			}

		}
		
		//doesn't exist.
		super.insert(meth_sym); 
		
		if (meth_sym.isMainMethod()) {
			
			if (SymbolTableBuilder.gst.hasMainMethod()) {  //more than one 'main'.
				throw new SemanticError("More than one 'main' method", meth.getLine(), meth_sym.getID());
			} else {
				SymbolTableBuilder.gst.setMainMethod(meth_sym);
			}
		}

	}

	public Symbol getField(String name) throws SemanticError {
		
		Symbol f = super.lookup(name); 
		
		if (f == null) {
			throw new SemanticError("Field not found", name); 
		}
		
		return f;
	}

	public Symbol getMethod(String name) throws SemanticError {
		
		Symbol m = super.lookup(name); 
		
		if (m == null) {
			throw new SemanticError("Method not found", name); 
		}
		
		return m;
	}

	public ClassSymbol getMyClassSymbol() {
		return this.myClassSymbol;
	}
	
	public String toString() {
		String str = "Class Symbol Table" + ": " + getID() + "\n" + super.toString();
		return str;
	}


}
