package IC.SymbolTable;

import java.util.*; 

import IC.AST.ICClass;

import IC.TypeTable.*;

/**
 * Global symbol table.
 *
 */
public class GlobalSymbolTable extends SymbolTable {
	
	private MethodSymbol mainMethod = null;  //for single 'main' checks. 

	public GlobalSymbolTable() {
		super(null, null);
	}

	/**
	 * Looks up and returns class sym. table of class with given name.
	 * 
	 */
	public ClassSymbolTable lookupCST(String name) throws SemanticError {
		
		Symbol cs = super.lookup(name);
		
		if (cs == null) {
			throw new SemanticError("Class not found", name); 
		}
		
		return ((ClassSymbol)cs).getClassSymbolTable();
	}

	/**
	 * Creates ClassSymbol, adds it to this (global) sym. table, and return it.
	 * 
	 */
	public ClassSymbol addClassSymbol(ICClass c) throws SemanticError {
		
		ClassSymbol cs = new ClassSymbol(c);
		super.insert(cs);
		
		return cs;
	}

	
	/** 
	 * 
	 * ID of gst is icFileName.
	 *
	 */
	public void setID(String icFileName) {
		super.id = icFileName;
	}

	
	public boolean hasMainMethod() {
		return (this.mainMethod != null);
	}
	
	public void setMainMethod(MethodSymbol meth) {
		this.mainMethod = meth;
	}

	public MethodSymbol getMainMethod() {
		return this.mainMethod;
	}

	
	@Override
	public String toString() {
		
		String str = "Global Symbol Table" + ": " + getID() + "\n" + super.toString();
		return str;
	}
     
}
