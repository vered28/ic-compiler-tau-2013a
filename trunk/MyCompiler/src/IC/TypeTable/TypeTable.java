package IC.TypeTable;

import java.util.*;  
import IC.AST.*;

/**
 * Holds the type table of the program, using 4 maps.
 *
 */
public class TypeTable {    
	
	private static Type intType = new IntType(); 
	private static Type boolType = new BoolType();
	private static Type nullType = new NullType();
	private static Type stringType = new StringType();   
	private static Type voidType = new VoidType();
	
	
	
	private static Map<String,ClassType> uniqueClassTypes = new LinkedHashMap<String,ClassType>();
	private static Map<Type,ArrayType> uniqueArrayTypes = new LinkedHashMap<Type,ArrayType>();  
	private static Map<String,MethodType> uniqueMethodTypes = new LinkedHashMap<String,MethodType>();
	private static Map<String,Type> uniquePrimitiveTypes = new LinkedHashMap<String,Type>();
	
	
	protected static int uniqueIdCounter = 0;
    private static String icFileName = null;
    
	
    /**
     * Initializes the type table.
     */
    public static void initializeTypeTable(String FileName) {
    	
        uniquePrimitiveTypes.put(intType.getName(), intType);
        uniquePrimitiveTypes.put(boolType.getName(), boolType);
        uniquePrimitiveTypes.put(nullType.getName(), nullType);
        uniquePrimitiveTypes.put(stringType.getName(), stringType);
        uniquePrimitiveTypes.put(voidType.getName(), voidType);
        
        icFileName = new String(FileName);
        
    }
	
    /**
     * Returns an ArrayType, whose elem. Type is elemType.
     * 
     */
	public static ArrayType getArrayType(Type elemType) {      
		
		//already exists.
		if (uniqueArrayTypes.containsKey(elemType)) {       
			return uniqueArrayTypes.get(elemType);   
			
		} else {   
			//creating new one.
			ArrayType arr_type = new ArrayType(elemType);        
			uniqueArrayTypes.put(elemType,arr_type);         
			return arr_type;      
		}   
	}
	
	
	/**
     * Returns a MethodType, with given AST Method node.
     * 
     */
	public static MethodType getMethodType(Method meth) throws SemanticError {
		
		Type returnType = getType(meth.getType().toString()); 
		
        List<Type> paramTypes = new ArrayList<Type>();
        
        //getting parameters.
        for (Formal f : meth.getFormals()) {
        	paramTypes.add(getType(f.getType().toString()));
        }
         
		MethodType mt1 = new MethodType(returnType, paramTypes);
		String key = mt1.toString();

		MethodType mt2 = uniqueMethodTypes.get(key);
		
		if (mt2 == null) {
			//creating new one.
			uniqueMethodTypes.put(key, mt1);
			return mt1; 
			
		} else { //already exists.
			return mt2;
		}

	}
	
	/**
     * Adds new ClassType to TypeTable. If the class is already defined
     * or extends a class that was not previously defined, throws SemanticError. 
     * 
     */
	 public static void addClassType(ICClass c) throws SemanticError {
		 
         String class_name = c.getName();
         
         if (uniqueClassTypes.containsKey(class_name)) {
        	 throw new SemanticError("Multiple definitons for class", c.getLine(), class_name);
         }
         
         if (c.hasSuperClass() && !(uniqueClassTypes.containsKey(c.getSuperClassName()))) {
        	 
        	 throw new SemanticError("Class inherits from undefined class", c.getLine(), c.getSuperClassName());
         }
         
         uniqueClassTypes.put(class_name, new ClassType(c));
	}
	 
	 
	/**
	 * Returns ClassType with given class name.
	 * 
	 */
	public static ClassType getClassType(String name) throws SemanticError {

		if (uniqueClassTypes.containsKey(name) == false) {
			throw new SemanticError("Class is undefined", name);
		} else { 
			return uniqueClassTypes.get(name);
		}
	}
	
	
	/**
     * Returns type object by given name (for primitive, array and class types).
     * 
     */
    public static Type getType(String typeName) throws SemanticError {
        Type t;
        
        //primitive type.
        t = uniquePrimitiveTypes.get(typeName);
        if (t != null) { 
        	return t;
        }
        
        //array type.
        if (typeName.endsWith("[]")) { 
        	return getArrayType(getType(typeName.substring(0, typeName.length()-2))); //elem. type name.
        }
        
        //class type.
        return getClassType(typeName);
        
    }
	
    
	/**
     * Returns true iff t is "int", "boolean" or "void" type.
     */
	public static boolean isPrimitiveType(Type t) {
		return (t.equals(intType) || t.equals(boolType) || t.equals(voidType));
         
	}

	
	public static int getUniqueId() {
        return ++uniqueIdCounter;   //first advancing, then returning.
	}

	
	/**
     * Returns string representation of the TypeTable, fitting the "-dump-symtab" coomand.
     * 
     */
	public static String staticToString() {
		
		String str = "Type Table: " + icFileName + "\n";
        
		//primitive types.
        for(Type t : uniquePrimitiveTypes.values()) {
        	str += "    " + t.getTypeID() + ": Primitive type: "+ t.getName()+ "\n";
        }
        
        //class types.
        for(Type t : uniqueClassTypes.values()) {
        	str += "    " + t.getTypeID() + ": Class: " + t.toString() + "\n";
        }
        
        //array types.
        for(Type t : uniqueArrayTypes.values()) {
        	str += "    " + t.getTypeID() + ": Array Type: " + t.toString() + "\n";
        }
        
        //method types.
        for(Type t : uniqueMethodTypes.values()) {
        	str += "    " + t.getTypeID() + ": Method type: " + t.toString() + "\n";
        }
        
        return str;
	}

}

