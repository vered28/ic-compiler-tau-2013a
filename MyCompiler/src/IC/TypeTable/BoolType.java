package IC.TypeTable;

/**
 * Boolean type.
 * 
 */
public class BoolType extends Type {
	
	public BoolType() {
		super("boolean");
	}
	
	
	public boolean subtypeof(Type t) {
		return (this == t);
	}

}
