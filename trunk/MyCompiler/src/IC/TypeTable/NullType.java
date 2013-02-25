package IC.TypeTable;

/**
 * Null type.
 * 
 */
public class NullType extends Type {
	
	public NullType() {
		super("null");
	}
	
	
	/** 
	 * null is a sub-type of any type, except of int, boolean and void.
	 */
	public boolean subtypeof(Type t) {
		
		//returns true iff t isn't "int", "boolean" or "void" type.
		if (TypeTable.isPrimitiveType(t)) {
			return false;
		} else {
			return true;
		}
	}
	
	
}
