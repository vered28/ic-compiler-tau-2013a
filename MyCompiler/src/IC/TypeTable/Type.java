package IC.TypeTable;

import IC.*; 

/**
 * IC type abstract class.
 * 
 */

public abstract class Type {
	
	private String name;  
	private int typeID;
	
	public Type(String tname) {  
		this.name = tname;
		this.typeID = TypeTable.getUniqueId();
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getTypeID() {
		return this.typeID;
	}
	
	@Override
	public String toString() {
        return getName();
	}
	
	public abstract boolean subtypeof(Type t);
	
}
