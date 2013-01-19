package IC.LIR;

import IC.AST.*;

/** An interface for a propagating AST visitor.
 * The visitor passes down objects of type <code>DownType</code>
 * and propagates up objects of type <code>UpType</code>.
 */
public interface PropagatingVisitor<DownType,UpType> {
	
	public UpType visit(Program program, DownType d);

	public UpType visit(ICClass icClass, DownType d);

	public UpType visit(Field field, DownType d);

	public UpType visit(VirtualMethod method, DownType d);

	public UpType visit(StaticMethod method, DownType d);

	public UpType visit(LibraryMethod method, DownType d);

	public UpType visit(Formal formal, DownType d);

	public UpType visit(PrimitiveType type, DownType d);

	public UpType visit(UserType type, DownType d);

	public UpType visit(Assignment assignment, DownType d);

	public UpType visit(CallStatement callStatement, DownType d);

	public UpType visit(Return returnStatement, DownType d);

	public UpType visit(If ifStatement, DownType d);

	public UpType visit(While whileStatement, DownType d);

	public UpType visit(Break breakStatement, DownType d);

	public UpType visit(Continue continueStatement, DownType d);

	public UpType visit(StatementsBlock statementsBlock, DownType d);

	public UpType visit(LocalVariable localVariable, DownType d);

	public UpType visit(VariableLocation location, DownType d);

	public UpType visit(ArrayLocation location, DownType d);

	public UpType visit(StaticCall call, DownType d);

	public UpType visit(VirtualCall call, DownType d);

	public UpType visit(This thisExpression, DownType d);

	public UpType visit(NewClass newClass, DownType d);

	public UpType visit(NewArray newArray, DownType d);

	public UpType visit(Length length, DownType d);

	public UpType visit(MathBinaryOp binaryOp, DownType d);

	public UpType visit(LogicalBinaryOp binaryOp, DownType d);

	public UpType visit(MathUnaryOp unaryOp, DownType d);

	public UpType visit(LogicalUnaryOp unaryOp, DownType d);

	public UpType visit(Literal literal, DownType d);

	public UpType visit(ExpressionBlock expressionBlock, DownType d);
}

