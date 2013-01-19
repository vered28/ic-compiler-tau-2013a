package IC.TypeTable;

import IC.AST.*;  

/**
 * Class type.
 * 
 */
public class ClassType extends Type {
	
	private ICClass classAST;
	
	public ClassType(ICClass c) {  
		super(c.getName());
		this.classAST = c;
	}
	
	
	public ICClass getClassAST() {
		return this.classAST;
	}
	

	public boolean subtypeof(Type t) {  

		if (!(t instanceof ClassType)) {  //t is not a class type
			return false;
		}
		
		if (t == this) {
			return true;    
		}
		
		String super_name = this.classAST.getSuperClassName();
		
		//no super class.
		if (super_name == null) {
			return false; 
		}
		
		//recursive check.
        try {
        	return TypeTable.getClassType(super_name).subtypeof(t);
        
        } catch (SemanticError se) { //we'll never get here.
                return false;
        }
	}
	
	
	@Override
	public String toString() {
		
		String str = super.getName();

        if (this.classAST.hasSuperClass()) {
        	try {
        		
        		String super_name = this.classAST.getSuperClassName();
        		str += ", Superclass ID: " + TypeTable.getClassType(super_name).getTypeID();
        		
            } catch (SemanticError se) { //will never be thrown.
            	
            }
        }
        
        return str;
	}
	

}
