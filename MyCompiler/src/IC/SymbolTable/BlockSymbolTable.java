package IC.SymbolTable;

import IC.AST.LocalVariable;
import IC.TypeTable.*;

/**
 * Symbol table of block. 
 *
 */
public class BlockSymbolTable extends SymbolTable {
	
	protected ClassSymbolTable enclosingClass;  //sym. table of enclosing class.
	protected Type returnType;  //for method blocks.

	
	public BlockSymbolTable(String id, SymbolTable parent) {
		super(id, parent);
		
		if (parent instanceof BlockSymbolTable) {  //we're in block which is inside a method.
			
			//getting return type of the method we are inside, for return stmt. semantic checks.
			this.returnType = ((BlockSymbolTable)this.parentSymbolTable).getReturnType(); 
																		
			this.enclosingClass = ((BlockSymbolTable)this.parentSymbolTable).getEnclosingCST(); 																
		}

	}

	
	public ClassSymbolTable getEnclosingCST() {
		return this.enclosingClass;
	}

	public Type getReturnType() {
		return this.returnType;
	}
	
	public void addLocalVariable(LocalVariable lv) throws SemanticError {
		VarSymbol v = new VarSymbol(lv);
		super.insert(v);
	}
	
	public int getVarDepthRec(String name) {
		int vd = entries.containsKey(name) ? this.getDepth() : ((BlockSymbolTable) parentSymbolTable).getVarDepthRec(name);
		return vd;
	}

	public Symbol lookupVariable(String name) throws SemanticError {
		
		Symbol vs = super.lookup(name); 
		
		if (vs == null) {
			throw new SemanticError("Variable not found", name); 
		}
		
		return vs;
	}
	
	public boolean isVarField (String name){
		if (entries.containsKey(name) || !(parentSymbolTable instanceof BlockSymbolTable)) return false;
		else return ((BlockSymbolTable)parentSymbolTable).isVarField(name);
	}

	@Override
	public String toString() {
	
		String str = "Statement Block Symbol Table ( located in "
						+ super.parentSymbolTable.getID() + " )\n" + super.toString();
		return str;
	}
	
}
