package IC.TypeTable;

/**
 * Void type.
 *
 */

public class VoidType extends Type {

	public VoidType() {
		super("void");
	}
	
	
	public boolean subtypeof(Type t) {
		return (this == t);
	}
	
}
