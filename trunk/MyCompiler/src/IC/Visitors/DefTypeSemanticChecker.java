package IC.Visitors;

import java.util.Iterator;
import java.util.NoSuchElementException;

import IC.AST.ArrayLocation;
import IC.AST.Assignment;
import IC.AST.Break;
import IC.AST.CallStatement;
import IC.AST.Continue;
import IC.AST.Expression;
import IC.AST.ExpressionBlock;
import IC.AST.Field;
import IC.AST.Formal;
import IC.AST.ICClass;
import IC.AST.If;
import IC.AST.Length;
import IC.AST.LibraryMethod;
import IC.AST.Literal;
import IC.AST.LocalVariable;
import IC.AST.LogicalBinaryOp;
import IC.AST.LogicalUnaryOp;
import IC.AST.MathBinaryOp;
import IC.AST.MathUnaryOp;
import IC.AST.Method;
import IC.AST.NewArray;
import IC.AST.NewClass;
import IC.AST.PrimitiveType;
import IC.AST.Program;
import IC.AST.Return;
import IC.AST.Statement;
import IC.AST.StatementsBlock;
import IC.AST.StaticCall;
import IC.AST.StaticMethod;
import IC.AST.This;
import IC.AST.UserType;
import IC.AST.VariableLocation;
import IC.AST.VirtualCall;
import IC.AST.VirtualMethod;
import IC.AST.Visitor;
import IC.AST.While;
import IC.SymbolTable.BlockSymbolTable;
import IC.SymbolTable.FieldSymbol;
import IC.SymbolTable.MethodSymbol;
import IC.SymbolTable.Symbol;
import IC.TypeTable.SemanticError;
import IC.TypeTable.TypeTable;

/**
 * Visitor for resolving the following issues:
 * - check illegal use of undefined symbols
 * - Type checks
 * - other semantic checks: "this" scope rules, "break" and "continue" scope rules
 */
public class DefTypeSemanticChecker implements Visitor {
	private IC.SymbolTable.GlobalSymbolTable global;
	private boolean inStatic = false;
	private int inLoop = 0;
	
	/**
	 * constructor
	 * @param global: the program's global symbol table
	 */
	public DefTypeSemanticChecker(IC.SymbolTable.GlobalSymbolTable global){
		this.global = global;
	}

	
	/**
	 * Program Visitor:
	 * - recursive calls to all classes
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(Program program) {
		// recursive call to class 
		for(ICClass c: program.getClasses()){
			if (c.accept(this) == null) return null;
		}
		return true;
	}

	
	/**
	 * ICClass Visitor:
	 * - recursive calls to all methods
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(ICClass icClass) {
		// by now all fields are defined legally
		// check only methods
		for(Method m: icClass.getMethods()){
			if (m.accept(this) == null) return null;
		}
		return true;
	}

	
	/**
	 * Field visitor: never called
	 */
	public Object visit(Field field) {
		return true;
	}
	
	/**
	 * Method Visitor:
	 * - recursive calls to all statements (used by static, virtual and library method)
	 * returns null if encountered an error, true otherwise
	 */
	public Object methodVisitHelper(Method method){
		// recursive call to all statements in method
		for(Statement s: method.getStatements()){
			if (s.accept(this) == null) return null;
		}
		return true;
	}

	
	/**
	 * VirtualMethod visitor: see methodVisitHelper documentation
	 */
	public Object visit(VirtualMethod method) {
		return methodVisitHelper(method);
	}

	
	/**
	 * StaticMethod visitor: see methodVisitHelper documentation
	 */
	public Object visit(StaticMethod method) {
		inStatic = true;
		Object ret = methodVisitHelper(method);
		inStatic = false;
		return ret;
	}

	
	/**
	 * LibraryMethod visitor: see methodVisitHelper documentation
	 */
	public Object visit(LibraryMethod method) {
		return methodVisitHelper(method);
	}

	
	/**
	 * Formal visitor: never called
	 */
	public Object visit(Formal formal) {
		return true;
	}

	
	/**
	 * PrimitiveType visitor: never called
	 */
	public Object visit(PrimitiveType type) {
		return true;
	}

	
	 /**
	  * UserType visitor: never called
	  */
	public Object visit(UserType type) {
		return true;
	}

	
	/**
	 * Assignment visitor:
	 * - recursive calls to location and assignment
	 * - type check: check that the assignment type <= location type
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(Assignment assignment) {
		// check location recursively
		IC.TypeTable.Type locationType = (IC.TypeTable.Type) assignment.getVariable().accept(this);
		if (locationType == null) return null;
		// check assignment recursively
		IC.TypeTable.Type assignmentType = (IC.TypeTable.Type) assignment.getAssignment().accept(this);
		if (assignmentType == null) return null;
		
		// type check
		// check that the assignment is of the same type / subtype of the location type
		if (!assignmentType.subtypeof(locationType)){
			System.err.println(new SemanticError("type mismatch, not of type "+locationType.getName(),
					assignment.getLine(),
					assignmentType.getName()));
			return null;
		}
		
		return true;
	}

	
	/**
	 * CallStatement visitor:
	 * - recursive calls to call
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(CallStatement callStatement) {
		if (callStatement.getCall().accept(this) == null) return null;
		else return true;
	}

	
	/**
	 * returnStatement visitor:
	 * - recursive call to call (static or virtual call)
	 * - type check: check that the returned call type <= enclosing method's type
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(Return returnStatement) {
		// check return statement recursively
		IC.TypeTable.Type returnedValueType = null; // dummy initialization
		if (returnStatement.hasValue()){
			returnedValueType = (IC.TypeTable.Type) returnStatement.getValue().accept(this);
			if (returnedValueType == null) return null;
		} else try{
			returnedValueType = TypeTable.getType("void");
		} catch(SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, Return visitor");} // will never get here
		
		// type check
		// check that the return type is the same type / subtype of the enclosing method's type
		try{
			IC.TypeTable.Type returnType = ((BlockSymbolTable) returnStatement.getEnclosingScope()).lookupVariable("_ret").getType();
			if (!returnedValueType.subtypeof(returnType)){
				System.err.println(new SemanticError("type mismatch, not of type "+returnType.getName(),
						returnStatement.getLine(),
						returnedValueType.getName()));
				return null;
			}
		} catch (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, Return visitor");} // will never get here
		
		return true;
	}

	
	/**
	 * If visitor:
	 * - recursive calls condition, operation and elseOperation
	 * - type check: check that the condition type is of type boolean
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(If ifStatement) {
		// check condition recursively
		IC.TypeTable.Type conditionType = (IC.TypeTable.Type) ifStatement.getCondition().accept(this);
		if (conditionType == null) return null;
		
		// type check
		// check that the condition is of type boolean
		try{
			if (!conditionType.subtypeof(TypeTable.getType("boolean"))){
				System.err.println(new SemanticError("condition in if statement not of type boolean",
						ifStatement.getCondition().getLine(),
						conditionType.getName()));
				return null;
			}
		} catch (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, If visitor");} // will never get here
		
		// check operation, elseOperation recursively
		if (ifStatement.getOperation().accept(this) == null) return null;
		if (ifStatement.hasElse()){
			if (ifStatement.getElseOperation().accept(this) == null) return null;
		}
		
		return true;
	}

	
	/**
	 * While visitor:
	 * - recursive calls condition and operation
	 * - type check: check that the condition type is of type boolean
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(While whileStatement) {
		// check condition recursively
		IC.TypeTable.Type conditionType = (IC.TypeTable.Type) whileStatement.getCondition().accept(this);
		if (conditionType == null) return null;
		
		// type check
		// check that the condition is of type boolean
		try{
			if (!conditionType.subtypeof(TypeTable.getType("boolean"))){
				System.err.println(new SemanticError("condition in while statement not of type boolean",
						whileStatement.getCondition().getLine(),
						conditionType.getName()));
				return null;
			}
		} catch (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, While visitor");} // will never get here
		
		// check operation recursively
		inLoop++;
		if (whileStatement.getOperation().accept(this) == null) {
			inLoop--;
			return null;
		}
		inLoop--;
		
		return true;
	}

	
	/**
	 * Break visitor: checks that in while loop
	 */
	public Object visit(Break breakStatement) {
		if (inLoop == 0){
			System.err.println(new SemanticError("'break' statement not in loop",
					breakStatement.getLine(),
					"break"));
			return null;
		}
		
		return true;
	}

	
	/**
	 * Continue visitor: checks that in while loop
	 */
	public Object visit(Continue continueStatement) {
		if (inLoop == 0){
			System.err.println(new SemanticError("'continue' statement not in loop",
					continueStatement.getLine(),
					"continue"));
			return null;
		}

		return true;
	}

	
	/**
	 * StatementsBlock visitor:
	 * - recursive calls to all statements
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(StatementsBlock statementsBlock) {
		// recursive call to all statements
		for(Statement s: statementsBlock.getStatements()){
			if (s.accept(this) == null) return null;
		}
		return true;
	}

	
	/**
	 * LocalVariable visitor:
	 * - recursive call to initValue (if exists)
	 * - type check: check that the initValue type is a subtype of the local variable's type
	 * returns null if encountered an error, true otherwise
	 */
	public Object visit(LocalVariable localVariable) {
		// recursive call to initValue
		if (localVariable.hasInitValue()){
			IC.TypeTable.Type initValueType = (IC.TypeTable.Type) localVariable.getInitValue().accept(this);
			if (initValueType == null) return null;
			
			try{
				// type check
				// check that the initValue type is a subtype of the local variable's type
				IC.TypeTable.Type localVariableType = ((BlockSymbolTable) localVariable.getEnclosingScope()).lookupVariable(localVariable.getName()).getType();
			
				if (!initValueType.subtypeof(localVariableType)){
					System.err.println(new SemanticError("type mismatch, not of type "+localVariableType.getName(),
							localVariable.getLine(),
							initValueType.getName()));
					return null;
				}
			} catch (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, LocalVariable visitor");} // will never get here
		}
		
		return true;
	}

	
	/**
	 * VariableLocation visitor:
	 * - recursive call to location (if exists)
	 * returns null if encountered an error, and the location type otherwise
	 */
	public Object visit(VariableLocation location) {
		// recursive call to location (if exists)
		if (location.isExternal()){
			IC.TypeTable.Type locationType = (IC.TypeTable.Type) location.getLocation().accept(this);
			if (locationType == null) return null;
			// check if the location is a class type
			try{
				TypeTable.getClassType(locationType.getName());
				// if location is a class, check that it has a field with this name
				IC.SymbolTable.ClassSymbolTable cst = this.global.lookupCST(locationType.getName());
				try{
					Symbol s = cst.lookup(location.getName());
					if(s == null) {
						throw new SemanticError("location of type "+locationType.getName()+" does not have field",
								location.getLine(),
								location.getName());
					}
					IC.SymbolTable.FieldSymbol fs = (FieldSymbol) s;
					// return the type of this field
					return fs.getType(); // this line will never throw error
				} catch(SemanticError se){ // the external location has no field with this name 
					se.setLine(location.getLine());
					System.err.println(se);
					return null;
				}
			} catch(SemanticError se){ // in case the external location is not a user defined class 
				System.err.println(new SemanticError("location of type "+locationType.getName()+" does not have field",
						location.getLine(),
						location.getName()));
				return null;
			}
		} else { // this location is not external
			try{
				IC.TypeTable.Type thisLocationType = ((BlockSymbolTable) location.getEnclosingScope()).lookupVariable(location.getName()).getType();
				return thisLocationType;
			} catch(SemanticError se){ // in case this location is not defined
				se.setLine(location.getLine());
				System.err.println(se);
				return null;
			}
		}
	}

	
	/**
	 * ArrayLocation visitor:
	 * - recursive call to array and index
	 * - type check: check that the index is of type int
	 * returns null if encountered an error, and the array element type otherwise
	 */
	public Object visit(ArrayLocation location) {
		// recursive call to array
		IC.TypeTable.ArrayType arrayType = (IC.TypeTable.ArrayType) location.getArray().accept(this);
		if (arrayType == null) return null;
		//recursive call to index
		IC.TypeTable.Type indexType = (IC.TypeTable.Type) location.getIndex().accept(this);
		if (indexType == null) return null;
		// type check
		// check that index is of type int
		try{
			if (!indexType.subtypeof(TypeTable.getType("int"))){
				System.err.println(new SemanticError("Array index must be of type int, type is",
						location.getLine(),
						arrayType.getName()));
				return null;
			}
		}catch(SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, ArrayLocation visitor");} // will never get here
		
		return arrayType.getElemType();
	}

	
	/**
	 * StaticCall visitor:
	 * - recursive call to arguments
	 * - check that the method is defined in the enclosing class
	 * - type checks: check that all argument correspond to the method's arguments types
	 * returns null if encountered an error, and the method return type otherwise
	 */
	public Object visit(StaticCall call) {
		// check if the class in the static call exists
		try{
			IC.SymbolTable.ClassSymbolTable cst = global.lookupCST(call.getClassName());
			if (cst == null){ // class does not exist
				System.err.println(new SemanticError("Class does not exist",
						call.getLine(),
						call.getClassName()));
				return null;
			}
		
			// check that the method is defined (as static) in enclosing class
			IC.SymbolTable.MethodSymbol ms = (MethodSymbol) cst.lookup(call.getName());
			// check if the method is static
			if (!ms.isStaticMethod()){
				System.err.println(new SemanticError("Method is not static",
						call.getLine(),
						call.getName()));
				return null;
			}
			// otherwise (method exists in class and is static) check arguments types
			Iterator<IC.TypeTable.Type> methodArgsTypeIter = ((IC.TypeTable.MethodType) ms.getType()).getParamsTypes().iterator();
			for(Expression arg: call.getArguments()){
				IC.TypeTable.Type argType = (IC.TypeTable.Type) arg.accept(this);
				
				if (argType == null) return null;
				
				if (!argType.subtypeof(methodArgsTypeIter.next())){ // wrong argument type sent to method
					System.err.println(new SemanticError("Wrong argument type passed to method",
							call.getLine(),
							argType.getName()));
					return null;
				}
			}
			// check if method expects more parameters
			if (methodArgsTypeIter.hasNext()){
				System.err.println(new SemanticError("Too few arguments passed to method",
						call.getLine(),
						call.getName()));
				return null;
			}
			// Finally if got here, return the method's return type
			///////////////////////////////////////////////////////
			return ((IC.TypeTable.MethodType) ms.getType()).getReturnType();
		}catch (SemanticError se) { // class (or its supers) doesn't have this method
			se.setLine(call.getLine());
			System.err.println(se);
			return null;
		}catch (NoSuchElementException nsee){ // method's parameters list is shorter than the arguments list
			System.err.println(new SemanticError("Too many arguments passed to method",
					call.getLine(),
					call.getName()));
			return null;
		}
	}

	
	/**
	 * VirtualCall visitor:
	 * - recursive call to arguments
	 * - check that the method is defined in the enclosing class
	 * - type checks: check that all argument correspond to the method's arguments types
	 * returns null if encountered an error, and the method return type otherwise
	 */
	public Object visit(VirtualCall call) {
		
		IC.SymbolTable.ClassSymbolTable cst = null;
		
		if (call.isExternal()){// call has an external location
			IC.TypeTable.Type locType = (IC.TypeTable.Type) call.getLocation().accept(this); 
			if (locType == null) return null; // visitor of the location encountered an error.
			try{
				cst = global.lookupCST(locType.getName());
			} catch (SemanticError se) {// When class name is invalid.
				se.setLine(call.getLine());
				System.err.println(se);
				return null;
			}
			
			if (cst == null){ // Location not a class
				System.err.println(new SemanticError("Location not of a user defined type",
						call.getLine(),
						locType.getName()));
				return null;
			}
		}else { // not an external call, check you are not in a static scope, and check the enclosing class you are in now
			cst = ((BlockSymbolTable)call.getEnclosingScope()).getEnclosingCST();
			if (inStatic){
				System.err.println(new SemanticError("Calling a local virtual method from a static scope",
						call.getLine(),
						call.getName()));
				return null;
			}
		}
		
		MethodSymbol ms = null;
		try{
			ms = (MethodSymbol) cst.lookup(call.getName());
			if(ms == null) {
				throw new SemanticError("Method not found", call.getName());
			}
		} catch (SemanticError se) {// When method name is invalid.
			se.setLine(call.getLine());
			System.err.println(se);
			return null;
		}
		
		if (ms.isStaticMethod()){
			System.err.println(new SemanticError("Static method is called virtually",
					call.getLine(),
					call.getName()));
			return null;
		}
		// otherwise (method exists in class and is virtual) check arguments types
		Iterator<IC.TypeTable.Type> methodArgsTypeIter = ((IC.TypeTable.MethodType) ms.getType()).getParamsTypes().iterator();
		for(Expression arg: call.getArguments()){
			IC.TypeTable.Type argType = (IC.TypeTable.Type) arg.accept(this);
			
			if (argType == null) return null;
			try{
				if (!argType.subtypeof(methodArgsTypeIter.next())){ // wrong argument type sent to method
					System.err.println(new SemanticError("Wrong argument type passed to method",
							call.getLine(),
							argType.getName()));
					return null;
				}
			}catch (NoSuchElementException nsee){
				System.err.println(new SemanticError("Too many arguments passed to method",
						call.getLine(),
						call.getName()));
				return null;
			}
		}
		// check if method expects more parameters
		if (methodArgsTypeIter.hasNext()){
			System.err.println(new SemanticError("Too few arguments passed to method",
					call.getLine(),
					call.getName()));
			return null;
		}
		// Finally if got here, return the method's return type
		///////////////////////////////////////////////////////
		return ((IC.TypeTable.MethodType) ms.getType()).getReturnType();
	}

	
	
	/**
	 * a Visitor for 'this' expression
	 * checks that it is not referenced inside a static method.
	 */
	public Object visit(This thisExpression) {
		if (inStatic) {
			System.err.println(new SemanticError("'this' referenced in static method",
					thisExpression.getLine(),
					"this"));
			return null;
		}
		return ((BlockSymbolTable) thisExpression.getEnclosingScope()).getEnclosingCST().getMyClassSymbol().getType();
	}

	
	/**
	 * a Visitor for the newClass expression
	 * checks that the class type exists
	 */
	public Object visit(NewClass newClass) {
		IC.TypeTable.ClassType ct = null;
		try{
			ct = IC.TypeTable.TypeTable.getClassType(newClass.getName());
		}catch (SemanticError se){ // No such class exists
			se.setLine(newClass.getLine());
			System.err.println(se);
			return null;
		}
		
		return ct;
	}

	
	/**
	 * a Visitor for NewArray expression.
	 * checks that elem type is a legal type.
	 * checks that size is of int type.
	 * returns the arrayType.
	 */
	public Object visit(NewArray newArray) {
		IC.TypeTable.Type elemType = null;
		
		try {
			elemType = TypeTable.getType(newArray.getType().toString());
		}catch (SemanticError se){ // illegal array elem type
			se.setLine(newArray.getLine());
			System.err.println(se);
			return null;
		}
		
		IC.TypeTable.Type sizeType = (IC.TypeTable.Type) newArray.getSize().accept(this);
		
		if (sizeType == null) return null; // size type visitor has encountered an error
		try {
			if (!sizeType.subtypeof(TypeTable.getType("int"))){
				System.err.println(new SemanticError("Array size not of int type",
						newArray.getLine(),
						sizeType.getName()));
				return null;
			}
		} catch (SemanticError se) {System.err.println("*** BUG1: DefTypeCheckingVisitor, newArray visitor");} // will never get here
		
		try{
			return TypeTable.getType(elemType.getName()+"[]");
		}catch (SemanticError se) {System.err.println("*** BUG2: DefTypeCheckingVisitor, newArray visitor");} // will never get here
		
		return null;
	}

	
	/**
	 * a Visitor for the array.length type.
	 * checks that array is an array.
	 * returns the type int.
	 */
	public Object visit(Length length) {
		IC.TypeTable.Type arrType = (IC.TypeTable.Type) length.getArray().accept(this);
		
		if (arrType == null) return null;
		
		if (!arrType.getName().endsWith("[]")){ // not array type.
			System.err.println(new SemanticError("Not of array type",
					length.getLine(),
					arrType.getName()));
			return null;			
		}
				
		try { // array type. length is legal - return int type.
			return TypeTable.getType("int");
		} catch (SemanticError se) {System.err.println("*** BUG: DefTypeCheckingVisitor, Length visitor");} // will never get here
		
		return null;
	}

	
	/**
	 * a Visitor for MathBinaryOp
	 * checks that types are legal (int or string for +, int for everything else)
	 * returns the type of the operation.
	 */
	public Object visit(MathBinaryOp binaryOp) {
		IC.TypeTable.Type op1Type = (IC.TypeTable.Type) binaryOp.getFirstOperand().accept(this);
		IC.TypeTable.Type op2Type = (IC.TypeTable.Type) binaryOp.getSecondOperand().accept(this);
		if ((op1Type == null) || (op2Type == null)) return null;
		if (op1Type != op2Type){ // check that both operands are of the same type
			System.err.println(new SemanticError("Math operation on different types",
					binaryOp.getLine(),
					binaryOp.getOperator().getOperatorString()));
			return null;
		}

		if (binaryOp.getOperator() != IC.BinaryOps.PLUS){// operator is one of "-","*","/","%"			
			try{
				if (!op1Type.subtypeof(TypeTable.getType("int"))){// enough to check only one of the operands' type, since they are of the same type
					System.err.println(new SemanticError("Math operation on a non int type",
							binaryOp.getLine(),
							op1Type.getName())); // returns the name of the type.
					return null;
				}
			} catch (SemanticError se){System.err.println("*** BUG1: DefTypeCheckingVisitor, MathBinaryOP visitor");} // will never get here
		}else{
			try{
				if (!op1Type.subtypeof(TypeTable.getType("int")) && !op1Type.subtypeof(TypeTable.getType("string"))){
					System.err.println(new SemanticError("+ operation on an illegal type",
							binaryOp.getLine(),
							op1Type.getName()));
					return null;
				}
			}catch (SemanticError se){System.err.println("*** BUG2: DefTypeCheckingVisitor, MathBinaryOP visitor");} // will never get here
		}
		// Legal types
		return op1Type;
	}

	
	/**
	 * a Visitor for LogicalBinaryOp
	 * checks that operands are of the correct type
	 * returns boolean type.
	 */
	public Object visit(LogicalBinaryOp binaryOp) {
		IC.TypeTable.Type op1Type = (IC.TypeTable.Type) binaryOp.getFirstOperand().accept(this);
		IC.TypeTable.Type op2Type = (IC.TypeTable.Type) binaryOp.getSecondOperand().accept(this);
		if ((op1Type == null) || (op2Type == null)) return null;
		if (!op1Type.subtypeof(op2Type) && !op2Type.subtypeof(op1Type)){ // neither operand is a subtype of the other operand
			if (binaryOp.getOperator() == IC.BinaryOps.LAND || 
					binaryOp.getOperator() == IC.BinaryOps.LOR){ // operator is one of "||","&&" 
				System.err.println(new SemanticError("Logical operation on non boolean type",
						binaryOp.getLine(),
						binaryOp.getOperator().getOperatorString()));
				return null;
			} else if (binaryOp.getOperator() == IC.BinaryOps.EQUAL || 
					binaryOp.getOperator() == IC.BinaryOps.NEQUAL) { // operator is one of "==", "!=" 
				System.err.println(new SemanticError("Comparing foreign types (at least one has to be subtype of another, or of the same type)",
						binaryOp.getLine(),
						binaryOp.getOperator().getOperatorString()));
				return null;				
			}else { // operator is one of "<=",">=","<",">"
				System.err.println(new SemanticError("Comparing non int values",
						binaryOp.getLine(),
						binaryOp.getOperator().getOperatorString()));
				return null;
			}
		}
		
		if ((binaryOp.getOperator() == IC.BinaryOps.LAND) ||
				(binaryOp.getOperator() == IC.BinaryOps.LOR)){// operator is one of "||","&&"
			try{
				if (!op1Type.subtypeof(TypeTable.getType("boolean"))){
					System.err.println(new SemanticError("Logical operation on non boolean values",
							binaryOp.getLine(),
							op1Type.getName()));
					return null;
				}
			} catch (SemanticError se){System.err.println("*** BUG1: DefTypeCheckingVisitor, LogicalBinaryOP visitor");} // will never get here
		} else if (binaryOp.getOperator() != IC.BinaryOps.EQUAL && 
				binaryOp.getOperator() != IC.BinaryOps.NEQUAL) {// operator is one of "<=",">=", "<", ">"
			try{
				if (!op1Type.subtypeof(TypeTable.getType("int"))){
					System.err.println(new SemanticError("Comparing non int values",
							binaryOp.getLine(),
							op1Type.getName()));
					return null;
				}
			} catch (SemanticError se){System.err.println("*** BUG2: DefTypeCheckingVisitor, LogicalBinaryOP visitor");} // will never get here
		}
		
		// types are legal. return boolean type.
		IC.TypeTable.Type ret = null;
		try{
			ret = TypeTable.getType("boolean");
		}catch (SemanticError se){System.err.println("*** BUG3: DefTypeCheckingVisitor, LogicalBinaryOP visitor");} // will never get here
		
		return ret;
	}

	
	/**
	 * a Visitor for MathUnaryOp - only one math unary operation - unary minus. 
	 * checks that the operand is of type int.
	 * returns type int.
	 */
	public Object visit(MathUnaryOp unaryOp) {
		IC.TypeTable.Type opType = (IC.TypeTable.Type) unaryOp.getOperand().accept(this);
		if (opType == null) return null;
		
		try{
			if (!opType.subtypeof(TypeTable.getType("int"))){// opType is not an integer
				System.err.println(new SemanticError("Mathematical unary operation on a non int type",
						unaryOp.getLine(),
						opType.getName()));
				return null;
			}
		}catch  (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, MathUnaryOP visitor");} // will never get here
		return opType; // int
	}

	
	/**
	 * a Visitor for LogicalUnaryOp - only one logic unary operation - unary logical negation. 
	 * checks that the operand is of type boolean.
	 * returns type boolean.
	 */
	public Object visit(LogicalUnaryOp unaryOp) {
		IC.TypeTable.Type opType = (IC.TypeTable.Type) unaryOp.getOperand().accept(this);
		if (opType == null) return null;
		
		try{
			if (!opType.subtypeof(TypeTable.getType("boolean"))){// opType is not a boolean
				System.err.println(new SemanticError("Logical unary operation on a non boolean type",
						unaryOp.getLine(),
						opType.getName()));
				return null;
			}
		}catch  (SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, LogicalUnaryOP visitor");} // will never get here
		return opType; // boolean
	}

	
	/**
	 * Literal visitor:
	 * returns the type of the literal
	 */
	public Object visit(Literal literal) {
		IC.LiteralTypes type = literal.getType();
		try{
			// return the corresponding type of the literal
			switch (type){
			case STRING: return TypeTable.getType("string");
			case INTEGER: return TypeTable.getType("int");
			case TRUE: return TypeTable.getType("boolean");
			case FALSE: return TypeTable.getType("boolean");
			case NULL: return TypeTable.getType("null");
			}
		}catch(SemanticError se){System.err.println("*** BUG: DefTypeCheckingVisitor, Literal visitor");} // will never get here
		return null;
	}

	
	/**
	 * ExpressionBlock visitor:
	 * - recursive call to expression
	 * returns null if encountered an error, and the type of the expression otherwise 
	 */
	public Object visit(ExpressionBlock expressionBlock) {
		return (IC.TypeTable.Type) expressionBlock.getExpression().accept(this);  // will return null if encounters an error
	}

}
