package IC.TypeTable;


/**
 * String type.
 * 
 */
public class StringType extends Type {

	public StringType() {
		super("string");
	}
	
	
	public boolean subtypeof(Type t) {
		return (this == t);
	}
	
}
