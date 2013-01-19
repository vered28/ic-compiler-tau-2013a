package IC.LIR;

import java.lang.reflect.Array;
import java.util.Arrays;

import IC.AST.*;

/**
 * Visitor to update the number of registers used in each AST node
 * for the Setti-Ulman optimization
 */
public class RegCounterVisitor implements Visitor {

	
	/**
	 * Program visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(Program program) {
		int maxRequired = 0;
		for (ICClass c: program.getClasses()){
			// update maximum required registers
			int classReq = (Integer) c.accept(this);
			maxRequired = Math.max(classReq, maxRequired);
		}
		program.setRequiredRegs(maxRequired);
		return maxRequired;
	}

	
	/**
	 * ICClass visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(ICClass icClass) {
		int maxRequired = 0;
		for (Method m: icClass.getMethods()){
			// update maximum required registers
			int methodReq = (Integer) m.accept(this);
			maxRequired = Math.max(methodReq, maxRequired);
		}
		icClass.setRequiredRegs(maxRequired);
		return maxRequired;
	}

	
	/**
	 * Field visitor: always 0
	 */
	public Object visit(Field field) {
		field.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * VirtualMethod visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(VirtualMethod method) {
		return methodsHelper(method);
	}

	
	/**
	 * StaticMethod visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(StaticMethod method) {
		return methodsHelper(method);
	}
	
	/**
	 * method visit helper for static and virtual methods
	 * @param method
	 * @return
	 */
	public Object methodsHelper(Method method){
		int maxRequired = 0;
		for (Statement s: method.getStatements()){
			// update maximum required registers
			int statReq = (Integer) s.accept(this);
			maxRequired = Math.max(statReq, maxRequired);
		}
		method.setRequiredRegs(maxRequired);
		return maxRequired;
	}

	
	/**
	 * LibraryMethod visitor: always 0
	 */
	public Object visit(LibraryMethod method) {
		method.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * Formal visitor: always 0
	 */
	public Object visit(Formal formal) {
		formal.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * PrimitiveType visitor: always 0
	 */
	public Object visit(PrimitiveType type) {
		type.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * UserType visitor: always 0
	 */
	public Object visit(UserType type) {
		type.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * Assignment visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(Assignment assignment) {
		int res = getSettiUlmanVal(assignment.getAssignment().accept(this),
									assignment.getVariable().accept(this));
		assignment.setRequiredRegs(res);
		return res;
		
	}

	
	/**
	 * CallStatement visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(CallStatement callStatement) {
		int res = (Integer)callStatement.getCall().accept(this);
		callStatement.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * Return visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(Return returnStatement) {
		int res = (Integer)returnStatement.getValue().accept(this);
		returnStatement.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * If visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(If ifStatement) {
		int res = Math.max((Integer)ifStatement.getCondition().accept(this),
							(Integer)ifStatement.getOperation().accept(this));
		if (ifStatement.hasElse()){
			int elseRes = (Integer)ifStatement.getElseOperation().accept(this);
			res = Math.max(res, elseRes);
		}
		
		ifStatement.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * While visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(While whileStatement) {
		int res = Math.max((Integer)whileStatement.getCondition().accept(this),
							(Integer)whileStatement.getOperation().accept(this));
		whileStatement.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * Break visitor: always 0
	 */
	public Object visit(Break breakStatement) {
		breakStatement.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * Continue visitor: always 0
	 */
	public Object visit(Continue continueStatement) {
		continueStatement.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * StatementsBlock visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(StatementsBlock statementsBlock) {
		int maxRequired = 0;
		for (Statement s: statementsBlock.getStatements()){
			// update maximum required registers
			int statReq = (Integer) s.accept(this);
			maxRequired = Math.max(statReq, maxRequired);
		}
		statementsBlock.setRequiredRegs(maxRequired);
		return maxRequired;
	}

	
	/**
	 * LocalVariable visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(LocalVariable localVariable) {
		int res = localVariable.hasInitValue() ?
					(Integer)localVariable.getInitValue().accept(this) : 0;
		localVariable.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * VariableLocation visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(VariableLocation location) {
		int res = 0;
		if (location.isExternal()){
			res = (Integer)location.getLocation().accept(this);
		}
		location.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * ArrayLocation visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(ArrayLocation location) {
		int res = getSettiUlmanVal(location.getArray().accept(this),
									location.getIndex().accept(this));
		location.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * StaticCall visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(StaticCall call) {
		// returns MAX_VALUE always (to keep original order where called)
		
		// get all arguments required registers
		for (int i=0; i<call.getArguments().size(); i++){
			call.getArguments().get(i).accept(this);
		}
		int res = Integer.MAX_VALUE;
		
		call.setRequiredRegs(res);
		return res;
		
	}

	
	/**
	 * VirtualCall visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(VirtualCall call) {
		// get all arguments required registers
		for (int i=0; i<call.getArguments().size(); i++){
			call.getArguments().get(i).accept(this);
		}
		if (call.isExternal()) call.getLocation().accept(this);
		
		// set the value in the VirtualCall ASTNode as MAX_INT, for keeping the original
		// order of virtual calls
		call.setRequiredRegs(Integer.MAX_VALUE);
		return Integer.MAX_VALUE;
	}
	
	
	/**
	 * This visitor: always 0
	 */
	public Object visit(This thisExpression) {
		thisExpression.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * NewClass visitor: always 0
	 */
	public Object visit(NewClass newClass) {
		newClass.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * NewArray visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(NewArray newArray) {
		int res = (Integer)newArray.getSize().accept(this);
		newArray.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * Length visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(Length length) {
		int res = (Integer)length.getArray().accept(this);
		length.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * MathBinaryOp visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(MathBinaryOp binaryOp) {
		int res = getSettiUlmanVal(binaryOp.getFirstOperand().accept(this),
									binaryOp.getSecondOperand().accept(this));
		binaryOp.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * LogicalBinaryOp visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(LogicalBinaryOp binaryOp) {
		int res = getSettiUlmanVal(binaryOp.getFirstOperand().accept(this),
				binaryOp.getSecondOperand().accept(this));
		binaryOp.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * MathUnaryOp visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(MathUnaryOp unaryOp) {
		int res = (Integer)unaryOp.getOperand().accept(this);
		unaryOp.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * LogicalUnaryOp visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(LogicalUnaryOp unaryOp) {
		int res = (Integer)unaryOp.getOperand().accept(this);
		unaryOp.setRequiredRegs(res);
		return res;
	}

	
	/**
	 * Literal visitor: always 0
	 */
	public Object visit(Literal literal) {
		literal.setRequiredRegs(0);
		return 0;
	}

	
	/**
	 * ExpressionBlock visitor:
	 * - get the maximum number of required registers
	 * - return the result
	 */
	public Object visit(ExpressionBlock expressionBlock) {
		int res = (Integer)expressionBlock.getExpression().accept(this);
		expressionBlock.setRequiredRegs(res);
		return res;
	}
	
	/////////////
	// helpers //
	/////////////
	
	/**
	 * returns the number of registers used by the Setti-Ulman algorithm
	 * for the given 2 values
	 */
	public static int getSettiUlmanVal(Object node1, Object node2){
		int n1 = (Integer) node1;
		int n2 = (Integer) node2;
		if (n1 == n2) return (n1 == Integer.MAX_VALUE) ? Integer.MAX_VALUE : n1 + 1;
		else return Math.max(n1, n2);
	}
	

}
