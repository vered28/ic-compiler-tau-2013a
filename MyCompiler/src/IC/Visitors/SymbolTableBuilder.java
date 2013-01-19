package IC.Visitors;

import java.util.*;
import IC.AST.*;
import IC.SymbolTable.*;
import IC.TypeTable.*;

/**
 * Visitor for building all Symbol tables (Global, class, method and block), and check:
 * - illegal symbol redefinitions 
 * - illegal shadowing
 * - illegal methods overriding
 * - existence and uniqueness of "main" method
 */
public class SymbolTableBuilder implements IC.AST.Visitor{
	private String icFileName;
	private boolean hasMain = false;
	
	/**
	 * constructor
	 * @param icFileName
	 */
	public SymbolTableBuilder(String icFilePath){
		String[] path = icFilePath.split("\\\\");
		this.icFileName = path[path.length-1];
		TypeTable.initializeTypeTable(icFileName);
	}
	
	/**
	 * getter for the ic program file name
	 * @return
	 */
	public String getFileName(){
		return this.icFileName;
	}
	
	/**
	 * returns true iff the given method is the main method
	 */
	private static boolean isMainMethod(MethodSymbol ms, Method m){
		if (!ms.isStaticMethod()) return false; // method is not static
		try {
			if (!ms.isMainMethod()) return false;
		} catch (SemanticError e) {
			return false;
		} // method name is not "main"
		IC.TypeTable.MethodType mt = (IC.TypeTable.MethodType) ms.getType();
		try{
			if (!mt.getReturnType().subtypeof(TypeTable.getType("void"))) return false; // return type is not void
			Iterator<IC.TypeTable.Type> paramTypesIter = mt.getParamsTypes().iterator();
			if (!paramTypesIter.hasNext()) return false; // no parameters
			IC.TypeTable.Type t = paramTypesIter.next();
			if (!t.subtypeof(TypeTable.getArrayType(TypeTable.getType("string")))) return false; // param is not of type string[]
			if (paramTypesIter.hasNext()) return false; // too many parameters
		}catch(SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, Literal visitor");} // will never get here
		//if (m.getFormals().get(0).getName().compareTo("args") != 0) return false;
		return true;
	}
	
	
	/**
	 * Program visitor:
	 * - creates GlobalSymbolTable
	 * - adds classes and updates the type table
	 * - recursive calls to class visitor for building ClassSymbolTable
	 * - adds class symbol tables to global
	 * Program is the only node with a NULL enclosingScope
	 * returns GlobalSymbolTable, or null if encountered an error
	 */
	public Object visit(Program program){
		// create a new global symbol table, to be returned at the end of construction
		GlobalSymbolTable global = new GlobalSymbolTable(icFileName);

		// add classes to global and updates the type table
		for (ICClass c: program.getClasses()){
			try{
				global.addClassSymbol(c);
			} catch (SemanticError se){
				// class is previously defined or super class is not defined
				se.setLine(c.getLine());
				System.err.println(se);
				return null; // run will be killed in the compiler in this case
			}
		}
		
		// recursive class symbol tables build
		for (ICClass c: program.getClasses()){
			// set enclosing scope
			c.setEnclosingScope(global);
			ClassSymbolTable cst = (ClassSymbolTable) c.accept(this);
			if (cst == null) return null; // If anywhere in the recursion an error has been encountered, the run will terminate.
			else {
				if(c.hasSuperClass()){
					global.getClassSymbolTableRec(c.getSuperClassName()).addClassSymbolTable(cst);
				} else {
					global.addClassSymbolTable(cst);
				}
			}
		}
		
		// check if has main method
		if (!hasMain){
			System.err.println(new SemanticError("Program has no main method",0,""));
			return null;
		}
		return global;
	}
	
	
	/**
	 * Class visitor:
	 * - creates ClassSymbolTable
	 * - adds fields and methods to the symbol table, while checking semantic rules
	 * - recursive calls to method visitor for building MethodSymbolTable
	 * - adds method symbol tables to this class symbol table
	 * returns ClassSymbolTable, or null if encountered an error
	 */
	public Object visit(ICClass icClass) {
		ClassSymbolTable cst;
		GlobalSymbolTable global = (GlobalSymbolTable) icClass.getEnclosingScope();
		
		// create suitable class symbol table
		if (icClass.hasSuperClass()) {
			cst = new ClassSymbolTable(icClass.getName(),
					global.getClassSymbolTableRec(icClass.getSuperClassName()),
					global);
		} else { // no superclass
			cst = new ClassSymbolTable(icClass.getName(),global);
		}
		
		// recursively fill class symbol table
		// fields:
		for (Field f: icClass.getFields()){
			// set enclosing scope
			f.setEnclosingScope(cst);
			// check if previously defined
			try{
				cst.lookup(f.getName());
				// if got till here, field is previously defined as field, print error
				System.err.println(new SemanticError("field is previously defined",f.getLine(),f.getName()));
				return null;
			} catch (SemanticError se){
				try{
					cst.lookup(f.getName());
					// if got till here, field is previously defined as method, print error
					System.err.println(new SemanticError("field is previously defined",f.getLine(),f.getName()));
					return null;
				} catch (SemanticError se2){ // field is not previously defined
					try{
						cst.addField(f.getName(), f.getType().toString());
					} catch (SemanticError se3){
						// the field's type is undefined
						se3.setLine(f.getLine());
						System.err.println(se3);
						return null;
					}
				}
			}
			// recursively call visitor
			if (f.accept(this) == null) return null;
		}
		
		// methods:
		for (Method m: icClass.getMethods()){
			// set enclosing scope
			m.setEnclosingScope(cst);
			// create MethodSymbol
			MethodSymbol ms;
			try{
				ms = new MethodSymbol(m);
				// check if this method is "main" and check uniqueness
				if (isMainMethod(ms, m)){
					if (hasMain){
						// already have "main" method, throw error
						System.err.println(new SemanticError("Program already has main method",
								m.getLine(),
								ms.getID()));
						return null;
					} else {
						hasMain = true;
					}
				}
			} catch (SemanticError se){
				// semantic error while creating the method symbol (some semantic type error)
				se.setLine(m.getLine());
				System.err.println(se);
				return null;
			}
			
			// check if previously defined as field or method
			// if method is previously defined in this class scope or in a super class with
			// a different signature (including if it's static or not), error. else, add a new MethodSymbol to the Symbol Table.
			try{
				cst.lookup(m.getName());
				// if got here, method is previously defined as field, print error
				System.err.println(new SemanticError("method is previously defined",m.getLine(),m.getName()));
				return null;
			} catch (SemanticError e){ // e will not be handled
				try{
					cst.lookup(m.getName());
					// if got here, method is previously defined in this class, print error
					System.err.println(new SemanticError("method is previously defined",m.getLine(),m.getName()));
					return null;
				} catch (SemanticError e2){ // e2 will not be handled
					try{
						MethodSymbol prevMS = (MethodSymbol) cst.lookup(m.getName());
						if (!prevMS.getType().equals(ms.getType()) || (prevMS.isStaticMethod() != ms.isStaticMethod())){
							// if got here, method is previously defined in super-class with a different signature, print error
							System.err.println(new SemanticError(
									"method is previously defined, overloading not allowed",
									m.getLine(),
									m.getName()));
							return null;
						} else { // overriding method
							cst.addMethodSymbol(m.getName(), ms);
						}
					}catch(SemanticError se){
						// method is not previously defined
						cst.addMethodSymbol(m.getName(), ms);
					}
				}
			}
		}
		
		// recursive method symbol table build
		for (Method m: icClass.getMethods()){
			MethodSymbolTable mst = (MethodSymbolTable) m.accept(this);
			if (mst == null) return null;
			else cst.addMethodSymbolTable(mst);
		}
		
		return cst;
	}

	/**
	 * Method visit helper:
	 * - creates MethodSymbolTable for all method types (static, virtual and library methods)
	 * - adds returned type symbol and parameters symbols to the symbol table, while checking semantic rules 
	 * - recursive calls to all other statements, in which local variables and block symbol tables will be
	 *   added to this method's symbol table
	 * returns MethodSymbolTable, or null if encountered an error
	 */
	public MethodSymbolTable methodVisit(Method method){
		// create method symbol table
		MethodSymbolTable mst = new MethodSymbolTable(method,
				(ClassSymbolTable)method.getEnclosingScope());
		
		// add return type symbol
		// set enclosing scope for return type
		method.getType().setEnclosingScope(mst);
		try{
			mst.setReturnVarSymbol(method.getType().toString());
		} catch (SemanticError se){
			// semantic error while creating the return type symbol (some semantic type error)
			se.setLine(method.getLine());
			System.err.println(se);
			return null;
		}
		
		// fill method symbol table with parameters (formals)
		for (Formal f: method.getFormals()){
			// set enclosing scope
			f.setEnclosingScope(mst);
			try{
				mst.lookupVariable(f.getName());
				// if got here, parameter is previously defined in this method
				System.err.println(new SemanticError("parameter is previously defined in method "+method.getName(),
						f.getLine(),
						f.getName()));
				return null;
			} catch (SemanticError e){ // e will not be handled
				// parameter is undefined, insert its symbol to this method symbol table
				try{
					mst.addParamSymbol(f.getName(), f.getType().toString());
				} catch (SemanticError se){
					// semantic error while creating the parameter type symbol (some semantic type error)
					se.setLine(f.getLine());
					System.err.println(se);
					return null;
				}
			}
			// recursive call to visitor
			if (f.accept(this) == null) return null;
		}
		
		// recursive call to visitor
		for (Statement s: method.getStatements()){
			// set enclosing scope
			s.setEnclosingScope(mst);
			if (s.accept(this) == null) return null; 
		}

		return mst;
	}
	
	
	/**
	 * StaticMethod visitor: see methodVisit documentation
	 */
	public Object visit(StaticMethod method) {
		MethodSymbolTable mst = methodVisit(method);
		if (mst == null) return null;
		else return mst;
	}
	
	
	/**
	 * VirtualMethod visitor: see methodVisit documentation
	 */
	public Object visit(VirtualMethod method) {
		MethodSymbolTable mst = methodVisit(method);
		if (mst == null) return null;
		else return mst;
	}
	
	
	/**
	 * LibraryMethod visitor: see methodVisit documentation
	 */
	public Object visit(LibraryMethod method) {
		MethodSymbolTable mst = methodVisit(method);
		if (mst == null) return null;
		else return mst;
	}
	
	
	/**
	 * StatementsBlock visitor:
	 * - creates BlockSymbolTable
	 * - updates its father (method / block) to include this block symbol table in its bst list
	 * - recursive calls to all statements in this block, in which local variables will be added
	 *   to this block symbol table 
	 * returns BlockSymbolTable, or null if encountered an error
	 */
	public Object visit(StatementsBlock statementsBlock) {
		BlockSymbolTable bst = new BlockSymbolTable(statementsBlock.getEnclosingScope());
		// get this bst's father (block / method symbol table)
		BlockSymbolTable bst_father = (BlockSymbolTable) statementsBlock.getEnclosingScope();
		
		// recursive call to visitor
		for (Statement s: statementsBlock.getStatements()){
			// set enclosing scope
			s.setEnclosingScope(bst);
			if (s.accept(this) == null) return null;
		}
		
		// add this block symbol table to its father's (method/block symbol table) bst list
		bst_father.addBlockSymbolTable(bst);
		return bst;
	}
	

	
	/**
	 * LocalVariable visitor:
	 * - creates symbol for this local variable and updates its father's symbol table (method / block)
	 * - updates enclosing scope for initValue and type, and calls their visitors recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(LocalVariable localVariable) {
		BlockSymbolTable bst = (BlockSymbolTable)localVariable.getEnclosingScope();
		try{
			bst.lookupVariable(localVariable.getName());
			// if got here, local variable is previously defined in this method / block
			System.err.println(new SemanticError("variable is previously defined",
					localVariable.getLine(),
					localVariable.getName()));
			return null;
		} catch (SemanticError e){ // e will not be handled
			// local variable is undefined, insert its symbol to block/method symbol table
			try{
				bst.addLocalVariable(localVariable);
			} catch (SemanticError se){
				// semantic error while creating the local variable type symbol (some semantic type error)
				se.setLine(localVariable.getLine());
				System.err.println(se);
				return null;
			}
		}
		// recursive call to visitor
		if (localVariable.hasInitValue()){
			localVariable.getInitValue().setEnclosingScope(localVariable.getEnclosingScope());
			if (localVariable.getInitValue().accept(this) == null) return null;
		}
		
		localVariable.getType().setEnclosingScope(localVariable.getEnclosingScope());
		if (localVariable.getType().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Assignment visitor:
	 *  - updates enclosing scope for location and value, and calls their visitors recursively
	 *  returns true, or null if encountered an error
	 */
	public Object visit(Assignment assignment) {
		// recursive call to visitor
		assignment.getVariable().setEnclosingScope(assignment.getEnclosingScope());
		if (assignment.getVariable().accept(this) == null) return null;
		
		assignment.getAssignment().setEnclosingScope(assignment.getEnclosingScope());
		if (assignment.getAssignment().accept(this) == null) return null;
		
		return true;
	}
	
	
	/**
	 * Break visitor: does nothing, returns true
	 */
	public Object visit(Break breakStatement) {
		return true;
	}

	
	/**
	 * CallStatement visitor:
	 * - updates enclosing scope for call, and calls its visitor recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(CallStatement callStatement) {
		callStatement.getCall().setEnclosingScope(callStatement.getEnclosingScope());
		if (callStatement.getCall().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Continue visitor: does nothing, returns true
	 */
	public Object visit(Continue continueStatement) {
		return true;
	}
	
	
	/**
	 * If visitor:
	 * - updates enclosing scope for condition, operation and else-operation, and calls their visitors recursively
	 * - if operation or else-operation are a LocalVariable statement, creates new block symbol table (for each)
	 * returns true, or null if encountered an error
	 */
	public Object visit(If ifStatement) {
		ifStatement.getCondition().setEnclosingScope(ifStatement.getEnclosingScope());
		if (ifStatement.getCondition().accept(this) == null) return null;
		
		// in case of a LocalVariable statement, create new BlockSymbolTable
		Statement operation = ifStatement.getOperation();
		if (operation instanceof LocalVariable){
			BlockSymbolTable bst = new BlockSymbolTable(ifStatement.getEnclosingScope());
			((BlockSymbolTable)ifStatement.getEnclosingScope()).addBlockSymbolTable(bst);
			operation.setEnclosingScope(bst);	
		} else operation.setEnclosingScope(ifStatement.getEnclosingScope());
		if (operation.accept(this) == null) return null;
		
		if (ifStatement.hasElse()){
			// in case of a LocalVariable statement, create new BlockSymbolTable
			Statement elseOperation = ifStatement.getElseOperation();
			if (elseOperation instanceof LocalVariable){
				BlockSymbolTable bst = new BlockSymbolTable(ifStatement.getEnclosingScope());
				((BlockSymbolTable)ifStatement.getEnclosingScope()).addBlockSymbolTable(bst);
				elseOperation.setEnclosingScope(bst);	
			} else elseOperation.setEnclosingScope(ifStatement.getEnclosingScope());
			if (elseOperation.accept(this) == null) return null;
		}
		
		return true;
	}

	
	/**
	 * Return visitor:
	 * - updates enclosing scope for value, and calls its visitor recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(Return returnStatement) {
		if (returnStatement.hasValue()){
			returnStatement.getValue().setEnclosingScope(returnStatement.getEnclosingScope());
			if (returnStatement.getValue().accept(this) == null) return null;
		}
		
		return true;
	}

	
	/**
	 * While visitor:
	 * - updates enclosing scope for condition and operation, and calls their visitors recursively
	 * - if operation is a LocalVariable statement, creates new block symbol table
	 * returns true, or null if encountered an error
	 */
	public Object visit(While whileStatement) {
		whileStatement.getCondition().setEnclosingScope(whileStatement.getEnclosingScope());
		if (whileStatement.getCondition().accept(this) == null) return null;
		
		// in case of a LocalVariable statement, create new BlockSymbolTable
		Statement operation = whileStatement.getOperation();
		if (operation instanceof LocalVariable){
			BlockSymbolTable bst = new BlockSymbolTable(whileStatement.getEnclosingScope());
			((BlockSymbolTable)whileStatement.getEnclosingScope()).addBlockSymbolTable(bst);
			operation.setEnclosingScope(bst);	
		} else operation.setEnclosingScope(whileStatement.getEnclosingScope());
		if (operation.accept(this) == null) return null;
		
		return true;
	}
	
	
	/**
	 * ArrayLocation visitor:
	 * - updates enclosing scope for array and index, and calls their visitors recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(ArrayLocation location) {
		location.getArray().setEnclosingScope(location.getEnclosingScope());
		if (location.getArray().accept(this) == null) return null;
		
		location.getIndex().setEnclosingScope(location.getEnclosingScope());
		if (location.getIndex().accept(this) == null) return null;
		
		return true;
	}
	
	
	/**
	 * ExpressionBlock visitor:
	 * - updates enclosing scope for expression, and calls its visitor recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(ExpressionBlock expressionBlock) {
		expressionBlock.getExpression().setEnclosingScope(expressionBlock.getEnclosingScope());
		if (expressionBlock.getExpression().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Field visitor:
	 * - updates enclosing scope for type, and calls its visitor recursively
	 * adding field to class symbol table is handled in the class's visitor
	 * returns true, or null if encountered an error
	 */
	public Object visit(Field field) {
		field.getType().setEnclosingScope(field.getEnclosingScope());
		if (field.getType().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Formal visitor:
	 * - updates enclosing scope for type, and calls its visitor recursively
	 * adding formal (parameter) to method symbol table is handled in the method's visitor
	 * returns true, or null if encountered an error
	 */
	public Object visit(Formal formal) {
		formal.getType().setEnclosingScope(formal.getEnclosingScope());
		if (formal.getType().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Length visitor:
	 * - updates enclosing scope for array, and calls its visitor recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(Length length) {
		length.getArray().setEnclosingScope(length.getEnclosingScope());
		if (length.getArray().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * Literal visitor: does nothing, returns true
	 */
	public Object visit(Literal literal) {
		return true;
	}

	/**
	 * BinaryOp visit helper:
	 * - updates enclosing scope for operand1 and operand2, and calls their visitors recursively
	 * used for LogicalBinaryOp and MathBinaryOp
	 * returns true, or null if encountered an error
	 */
	public Object binaryOpVisit(BinaryOp binaryOp){
		binaryOp.getFirstOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		if (binaryOp.getFirstOperand().accept(this) == null) return null;
		
		binaryOp.getSecondOperand().setEnclosingScope(binaryOp.getEnclosingScope());
		if (binaryOp.getSecondOperand().accept(this) == null) return null;
		
		return true;
	}
	
	
	/**
	 * LogicalBinaryOp visitor: see binaryOpVisit documentation 
	 */
	public Object visit(LogicalBinaryOp binaryOp) {
		return binaryOpVisit(binaryOp);
	}
	
	
	/**
	 * MathBinaryOp visitor: see binaryOpVisit documentation
	 */
	public Object visit(MathBinaryOp binaryOp) {
		return binaryOpVisit(binaryOp);
	}
	
	/**
	 * UnaryOp visit helper:
	 * - updates enclosing scope for operand, and calls its visitor recursively
	 * used for LogicalUnaryOp and MathUnaryOp
	 * returns true, or null if encountered an error
	 */
	public Object unaryOpVisit(UnaryOp unaryOp){
		unaryOp.getOperand().setEnclosingScope(unaryOp.getEnclosingScope());
		if (unaryOp.getOperand().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * LogicalUnaryOp visitor: see unaryOpVisit documentation
	 */
	public Object visit(LogicalUnaryOp unaryOp) {
		return unaryOpVisit(unaryOp);
	}

	
	/**
	 * MathUnaryOp visitor: see unaryOpVisit documentation
	 */
	public Object visit(MathUnaryOp unaryOp) {
		return unaryOpVisit(unaryOp);
	}

	
	/**
	 * NewArray visitor:
	 * - updates enclosing scope for type and size, and calls their visitors recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(NewArray newArray) {
		newArray.getType().setEnclosingScope(newArray.getEnclosingScope());
		if (newArray.getType().accept(this) == null) return null;
		
		newArray.getSize().setEnclosingScope(newArray.getEnclosingScope());
		if (newArray.getSize().accept(this) == null) return null;
		
		return true;
	}

	
	/**
	 * NewClass visitor: does nothing, returns true
	 */
	public Object visit(NewClass newClass) {
		return true;
	}

	
	/**
	 * PrimitiveType visitor: does nothing, returns true
	 */
	public Object visit(PrimitiveType type) {
		return true;
	}

	
	/**
	 * This visitor: does nothing, returns true
	 */
	public Object visit(This thisExpression) {
		return true;
	}

	
	/**
	 * UserType visitor: does nothing, returns true
	 */
	public Object visit(UserType type) {
		return true;
	}

	
	/**
	 * VariableLocation visitor:
	 * - updates enclosing scope for location, and calls its visitor recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(VariableLocation location) {
		if (location.isExternal()){ // field location is not null
			location.getLocation().setEnclosingScope(location.getEnclosingScope());
			if (location.getLocation().accept(this) == null) return null;
		}else try{
			// check that the location is a previously defined variable
			((BlockSymbolTable) location.getEnclosingScope()).lookupVariable(location.getName());
		} catch (SemanticError se){
			se.setLine(location.getLine());
			System.err.println(se);
			return null;
		}
		
		return true;
	}

	
	/**
	 * StaticCall visitor:
	 * - updates enclosing scope for arguments, and calls their visitors recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(StaticCall call) {
		for (Expression e: call.getArguments()){
			e.setEnclosingScope(call.getEnclosingScope());
			if (e.accept(this) == null) return null;
		}
		
		return true;
	}
	
	
	/**
	 * VirtualCall visitor:
	 * - updates enclosing scope for location and arguments, and calls their visitors recursively
	 * returns true, or null if encountered an error
	 */
	public Object visit(VirtualCall call) {
		if (call.isExternal()) { // field location is not null
			call.getLocation().setEnclosingScope(call.getEnclosingScope());
			if (call.getLocation().accept(this) == null) return null;
		}
		
		for (Expression e: call.getArguments()){
			e.setEnclosingScope(call.getEnclosingScope());
			if (e.accept(this) == null) return null;
		}
		
		return true;
	}

}
