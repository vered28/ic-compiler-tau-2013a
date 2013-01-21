package IC.TypeTable;

import java.util.*;

/**
 * Method type.
 * 
 */
public class MethodType extends Type {
	
	private List<Type> params;
	private Type returnType;

	public MethodType(Type returnType, List<Type> params) {
		super(null); // name will be taken from toString.
		this.returnType = returnType;
		this.params = params;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	public List<Type> getParamsTypes() {
		return this.params;
	}

	public boolean subtypeof(Type t) {
		return (t == this);
	}

	/**
	 * Returns true iff mt equals to this method type. Checks return type and
	 * parameters.
	 * 
	 */
	public boolean equals(MethodType mt) {

		//different ret. type.
		if (mt.getReturnType().subtypeof(this.getReturnType()) == false) {
			return false;
		}

		//different # of params.
		if (params.size() != mt.getParamsTypes().size()) {
			return false;
		}

		//checking params. types respectively.
		for (int i = 0; i < this.params.size(); i++) {
			if (mt.getParamsTypes().get(i).subtypeof(params.get(i)) == false) {
				return false;
			}
		}

		return true;
	}

	public boolean isMainMethodType() throws SemanticError {
		return ((params.size()==1) && (params.get(0).subtypeof(TypeTable.getType("string[]")))
				     && (returnType == TypeTable.getType("void")));
	}

	@Override
	public String toString() {
		String str = "{";

		//concat. params.

		if (this.params.size() > 0) { // we have at least one param.
			str += this.params.get(0).getName(); // first param.
		}

		//other params.
		for (int i = 1; i < this.params.size(); i++) {
			str += ", " + this.params.get(i).getName();
		}

		//return type.
		str += " -> " + this.returnType.getName() + "}";

		return str;

	}
 
}
