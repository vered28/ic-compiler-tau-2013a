package IC.SymbolTable;

import java.util.*; 

import IC.TypeTable.*;

/**
 * Base class for symbol tables. 
 * Holds entries map, parent and children symbol tables.
 *
 */

public class SymbolTable {
	
	protected int depth;
    protected String id;
    protected SymbolTable parentSymbolTable;
    private List<SymbolTable> childrenTables;
    protected Map<String, Symbol> entries;
    protected boolean StaticScope;

    public SymbolTable(String id, SymbolTable parent) {
            
    	this.id = id;
    	this.parentSymbolTable = parent;
        this.depth = (parent == null ? 0 : parent.depth + 1);   
        this.entries = new LinkedHashMap<String, Symbol>();
        this.childrenTables = new ArrayList<SymbolTable>();
        
        if (this.parentSymbolTable == null) { //global scope.
        	StaticScope = false;
        } else {
        	StaticScope = this.parentSymbolTable.isStaticScope();
        }
    }

    
    public SymbolTable getParent() {
    	return this.parentSymbolTable;
    }

    
    public void setParent(SymbolTable parent) {
    	this.parentSymbolTable = parent;
    }

    
    public int getDepth() {
    	return this.depth;
    }
    
    public void setDepth(int Depth) {
    	this.depth = depth;
    }
    
    public String getID() {
    	return this.id;
    }
    
    
    public List<SymbolTable> getChildrenTables() {
        return this.childrenTables;
    }
    
    
    public void addChild(SymbolTable chld) {
        this.childrenTables.add(chld);
    }

    
    
    private boolean isStaticScope() {
    	return this.StaticScope;
    }
    
    
    protected boolean hasNoParentScope() {
    	return (this.parentSymbolTable == null) || 
    			(isStaticScope() && (getParent() instanceof ClassSymbolTable)); 
    			//e.g. class static method scope has no parent scope. 
    }

    /**
     * Adds new symbol to the table. Since re-definitions and overloading are 
     * not supported, SemanticError will be thrown in any case of multiple definitions.
     *
     */
    public void insert(Symbol sym) throws SemanticError {

    	if (this.entries.containsKey(sym.getID())) {
    		throw new SemanticError("Multiple definitions for symbol in scope", sym.getID());
        }
            
    	this.entries.put(sym.getID(), sym);
    }

    
    /**
     * Lookup a symbol name in table, recursively.
     *
     */
    public Symbol lookup(String sym_name) {
            
    	Symbol ret = this.entries.get(sym_name);
    	
        if (ret == null) {
                    
        	if (this.hasNoParentScope() == true) {
        		
        		return null;   
        		
        	} else {
        		return this.parentSymbolTable.lookup(sym_name);
            } 
        	
        } else {
        	return ret;
        }
    }
    
    /**
     * Returns enclosing sym. table of symbol with name sym_name. 
     *
     */
    public SymbolTable getEnclosingST(String sym_name) { 
            
    	SymbolTable ret = this;
    	
    	while (ret.entries.get(sym_name) == null) {
    		if (ret.hasNoParentScope()) {
    			return null;
    		}
    		ret = ret.getParent();
    	}
    	
        return ret;
    }
    
    
    @Override
	public String toString() {
		String str = "";

		for (Symbol e : entries.values()) {
			str += "    " + e.toString() + "\n";
		}

		if (!this.childrenTables.isEmpty()) {
			str += "Children tables:";
			for (SymbolTable e : childrenTables) {
				str += " " + e.getID() + ",";
			}
			
			str = str.substring(0, str.length()-1) + "\n";
			str += "\n";

			for (SymbolTable e : childrenTables) {
				str += e.toString() + "\n";
			}
			
			str = str.substring(0, str.length()-1);
		}
		
		return str;
	}
 
}
