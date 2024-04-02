package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import static miniJava.AbstractSyntaxTrees.TypeKind.*;

public class TypeChecking implements Visitor<Object, Object> {
	private ErrorReporter _errors;

	public TypeChecking(ErrorReporter errors) {
		this._errors = errors;
	}

	public void parse(Package prog) {
		prog.visit(this, null);
	}

	public Object visitPackage(Package prog, Object unused) {
		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}
	// Declarations
	public Object visitClassDecl(ClassDecl cd, Object unused) {
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}
	public Object visitFieldDecl(FieldDecl fd, Object unused) {
		fd.type.visit(this, null);
		return null;
	}
	public Object visitMethodDecl(MethodDecl md, Object unused) { return null; }
	public Object visitParameterDecl(ParameterDecl pd, Object unused) { return null; }
	public Object visitVarDecl(VarDecl decl, Object unused) { return null; }
	// Types
	public Object visitBaseType(BaseType type, Object unused) { return null; }
	public Object visitClassType(ClassType type, Object unused) { return null; }
	public Object visitArrayType(ArrayType type, Object unused) { return null; }
	// Statements
	public Object visitBlockStmt(BlockStmt stmt, Object unused) { return null; }
	public Object visitVarDeclStmt(VarDeclStmt stmt, Object unused) { return null; }
	public Object visitAssignStmt(AssignStmt stmt, Object unused) { return null; }
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object unused) { return null; }
	public Object visitCallStmt(CallStmt stmt, Object unused) { return null; }
	public Object visitReturnStmt(ReturnStmt stmt, Object unused) { return null; }
	public Object visitIfStmt(IfStmt stmt, Object unused) { return null; }
	public Object visitWhileStmt(WhileStmt stmt, Object unused) { return null; }

	// Expressions
	public Object visitUnaryExpr(UnaryExpr expr, Object unused) { return null; }
	public Object visitBinaryExpr(BinaryExpr expr, Object unused) { return null; }
	public Object visitRefExpr(RefExpr expr, Object unused) { return null; }
	public Object visitIxExpr(IxExpr expr, Object unused) { return null; }
	public Object visitCallExpr(CallExpr expr, Object unused) { return null; }
	public Object visitLiteralExpr(LiteralExpr expr, Object unused) { return null; }
	public Object visitNewObjectExpr(NewObjectExpr expr, Object unused) { return null; }
	public Object visitNewArrayExpr(NewArrayExpr expr, Object unused) { return null; }

	// References
	public Object visitThisRef(ThisRef ref, Object unused) { return null; }
	public Object visitIdRef(IdRef ref, Object unused) {
		return ref.id.visit(this, null);
	}

	public Object visitQRef(QualRef ref, Object unused) {
		return ref.id.visit(this, null);
	}

	// Terminals
	public Object visitIdentifier(Identifier id, Object unused) {
		return id.decl.type;
	}
	public Object visitOperator(Operator op, Object unused) {
		return null;
	}
	public Object visitIntLiteral(IntLiteral num, Object unused) {
		return new BaseType(INT, null);
	}
	public Object visitBooleanLiteral(BooleanLiteral bool, Object unused) {
		return new BaseType(BOOLEAN, null);
	}
	public Object visitNullReference(NullReference nl, Object unused) { return null; }


	private void reportTypeError(AST ast, String errMsg) {
		_errors.reportError( ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg );
	}
}