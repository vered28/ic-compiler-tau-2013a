package IC.LIR;

import java.util.*;
import IC.AST.*;
import IC.SymbolTable.*;

/**
 * ClassLayout
 * ===========
 * class layout implementation
 * holds methods and fields offsets
 */
public class ClassLayout {

	private ICClass icClass;
	private Map<Method,Integer> methodToOffset = new HashMap<Method,Integer>();
	private Map<Field,Integer> fieldToOffset = new HashMap<Field,Integer>();
	private Map<String,Method> nameToMethod = new HashMap<String,Method>();
	private int methodCounter = 0;
	private int fieldCounter = 1;
	
	/**
	 * constructor for class layout
	 * @param icClass
	 */
	public ClassLayout(ICClass icClass){
		this.icClass = icClass;
		
		// put methods
		for(Method m: icClass.getMethods()){
			if (!m.isStatic())
				methodToOffset.put(m, methodCounter++);
		}
		
		// put fields
		for(Field f: icClass.getFields()){
			fieldToOffset.put(f, fieldCounter++);
		}
		
		// create string to method
		for(Method m: icClass.getMethods()){
			nameToMethod.put(m.getName(), m);
		}
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * constructor for class layout with super-class
	 */
	public ClassLayout (ICClass icClass, ClassLayout superLayout){
		this.icClass = icClass;
		
		// start with super-class layout methods and fields offsets
		methodToOffset = (HashMap<Method, Integer>)((HashMap<Method, Integer>)superLayout.getMethodToOffsetMap()).clone();
		fieldToOffset = (HashMap<Field, Integer>)((HashMap<Field, Integer>)superLayout.getFieldToOffsetMap()).clone();
		
		// set offsets
		methodCounter = methodToOffset.size();
		fieldCounter = 1+fieldToOffset.size();
		
		// add new methods and override exiting ones
		for (Method m: icClass.getMethods()){
			boolean isOverriden = false;
			
			if (m.isStatic()) continue;
			
			for (Method existingMethod: methodToOffset.keySet()){
				// if method already exist, replace it with the overriding
				if (m.getName().equals(existingMethod.getName())){
					int offset = methodToOffset.remove(existingMethod);
					methodToOffset.put(m, offset);
					isOverriden = true;
					break;
				}
			}
			
			// if method has not been overridden, insert the method 
			if (!isOverriden)
				methodToOffset.put(m, methodCounter++);
		}
		
		// add new fields
		for(Field f: icClass.getFields()){
			fieldToOffset.put(f, fieldCounter++);
		}
		
		// create string to method
		for(Method m: methodToOffset.keySet()){
			nameToMethod.put(m.getName(), m);
		}
		
		for(Method m: icClass.getMethods()){
			if (m.isStatic())
				nameToMethod.put(m.getName(), m);
		}
		
	}
	
	
	//////////////
	//	getters	//
	//////////////
	
	/**
	 * getter for this class layout's ICClass
	 */
	public ICClass getICClass(){
		return this.icClass;
	}
	
	/**
	 * getter for this class name
	 * @return
	 */
	public String getClassName(){
		return this.icClass.getName();
	}
	
	/**
	 * getter for map of methods and offsets
	 * @return
	 */
	public Map<Method,Integer> getMethodToOffsetMap(){
		return this.methodToOffset;
	}
	
	/**
	 * getter for method's offset
	 * @param m
	 * @return
	 */
	public Integer getMethodOffset(Method m){
		return methodToOffset.get(m);
	}
	
	/**
	 * get method's offset by name
	 * @param name
	 * @return
	 */
	public Integer getMethodOffset(String name){
		return getMethodOffset(nameToMethod.get(name));
	}
	
	/**
	 * getter for map of fields and offsets
	 * @return
	 */
	public Map<Field,Integer> getFieldToOffsetMap(){
		return this.fieldToOffset;
	}
	
	/**
	 * getter for field's offset
	 * @param f
	 * @return
	 */
	public Integer getFieldOffset(Field f){
		return fieldToOffset.get(f);
	}
	
	/**
	 * getter for map of names and methods
	 * @return
	 */
	public Map<String,Method> getNameToMethodMap(){
		return this.nameToMethod;
	}
	
	/**
	 * getter for method by its name
	 * @param name
	 * @return
	 */
	public Method getMethodFromName(String name){
		return nameToMethod.get(name);
	}
	
	/**
	 * getter for the number of bytes needed for allocation
	 * @return
	 */
	public int getAllocSize(){
		return 4*(fieldToOffset.size()+1);
	}
	
	//////////////
	//	adders	//
	//////////////
	
	/**
	 * adder for method to offset
	 * @param m
	 * @param offset
	 */
	public void addMethodToOffset(Method m, Integer offset){
		methodToOffset.put(m, offset);
	}
	
	/**
	 * adder for field to offset
	 * @param f
	 * @param offset
	 */
	public void addFieldToOffset(Field f, Integer offset){
		fieldToOffset.put(f, offset);
	}
	
	//////////////////////////////
	//	string representation	//
	//////////////////////////////
	
	/**
	 * returns the string representation for the class dispatch table
	 */
	public String getDispatchTable(){
		String dispatch = "_DV_"+icClass.getName()+": [";
		
		// insert methods' labels ordered by increasing offset
		for(int i = 0; i < methodCounter; i++){
			for (Method m: methodToOffset.keySet()){
				// if this method is static, skip
				if (m.isStatic()) continue;
				
				// if the offset is correct, insert method label
				if (methodToOffset.get(m) == i){
					dispatch += "_";
					dispatch += ((ClassSymbolTable) m.getEnclosingScope()).getMyClassSymbol().getID();
					dispatch += "_"+m.getName()+",";
					break;
				}
			}
		}
		if (dispatch.endsWith(",")) dispatch = dispatch.substring(0, dispatch.length()-1);
		dispatch += "]\n";
		
		// get all fields and offsets as comments
		String fieldsOffsets = "# fields offsets:\n";
		for(int i = 1; i < fieldCounter; i++){
			for (Field f: fieldToOffset.keySet()){
				// if the offset is correct, insert field and its offset
				if (fieldToOffset.get(f) == i){
					fieldsOffsets += "# "+f.getName()+": ";
					fieldsOffsets += i+"\n";
					break;
				}
			}
		}
		dispatch += fieldsOffsets;
		
		return dispatch;
	}
}
