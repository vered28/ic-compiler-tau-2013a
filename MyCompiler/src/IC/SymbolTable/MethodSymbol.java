package IC.SymbolTable;

import IC.AST.Method; 
import IC.TypeTable.*;

/**
 * Method symbol class.
 *
 */
public class MethodSymbol extends Symbol {
	
	private boolean isStaticMethod;

	public MethodSymbol(Method method) throws SemanticError {
		super(method.getLine(), method.getName(), Kind.METHOD, TypeTable.getMethodType(method));
														
		this.isStaticMethod = method.isStatic();

	}

	public boolean isStaticMethod() {
		return this.isStaticMethod;
	}

	public Type getReturnType() {
		return ((MethodType)super.getType()).getReturnType();
	}

	public boolean isMainMethod() throws SemanticError {
		//main method type and name 'main' and static.
		return ((MethodType)super.getType()).isMainMethodType() && super.getID().equals("main") && isStaticMethod();
	}

	@Override
	public String toString() {
		
		String str;
		if (isStaticMethod()) {
			str = "Static method";
		} else {
			str = "Virtual method";
		}

		return (str + ": " + getID().toString() + " " + getType().toString());
	}
	
}
