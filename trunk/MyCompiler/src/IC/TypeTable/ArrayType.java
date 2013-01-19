package IC.TypeTable;

/**
 * Array type.
 * 
 */
public class ArrayType extends Type {
	
	private Type elemType;
	
	
	public ArrayType(Type elType) {
		
		super(elType.getName()+"[]");
		this.elemType = elType;
	}
	
	
	public Type getElemType() {
		return this.elemType;
	}
	
	
	public boolean subtypeof(Type t) {
		return (this == t);
	}

}
