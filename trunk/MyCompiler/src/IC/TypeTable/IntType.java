package IC.TypeTable;


/**
 * Int type.
 * 
 */
public class IntType extends Type {
	
	
	public IntType() {
		super("int");
	}
	
	
	public boolean subtypeof(Type t) {
		return (this == t);
	}

}
