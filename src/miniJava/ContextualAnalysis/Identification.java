package miniJava.ContextualAnalysis;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import static miniJava.AbstractSyntaxTrees.TypeKind.*;

public class Identification implements Visitor<Object,Object> {
	private ErrorReporter _errors;
	private ScopedIdentification SI;

	private boolean inStatic = false;
	private String varDeclared = null;
	
	public Identification(ErrorReporter errors) {
		this.SI = new ScopedIdentification();
		this._errors = errors;
	}

	public void parse( Package prog ) {
		try {
			prog.visit(this,null);
		} catch( IdentificationError e ) {
			_errors.reportError(e.toString());
		}
	}

	// Package
	public Object visitPackage(Package prog, Object unused) {
		FieldDeclList SystemList = new FieldDeclList();
		FieldDecl out = new FieldDecl(
				false, true,
				new ClassType(
						new Identifier(new Token(TokenType.IDENTIFIER, "_PrintStream")),
						null),
				"out",
				null);
		SystemList.add(out);
		ClassDecl classSystem = new ClassDecl("System", SystemList, new MethodDeclList(), null);

		MethodDeclList _PrintStreamList = new MethodDeclList();
		ParameterDeclList printlnList = new ParameterDeclList();
		ParameterDecl n = new ParameterDecl(new BaseType(INT, null), "n", null);
		printlnList.add(n);
		FieldDecl printlnField = new FieldDecl(
				false, false, new BaseType(VOID, null), "println", null);
		MethodDecl println = new MethodDecl(
				printlnField, printlnList, new StatementList(), null);
		_PrintStreamList.add(println);
		ClassDecl class_PrintStream = new ClassDecl("_PrintStream", new FieldDeclList(), new MethodDeclList(), null);

		ClassDecl classString = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);

		SI.openScope();
		for(ClassDecl cd : prog.classDeclList) {
			SI.addDeclaration(cd);
		}
		SI.addDeclaration(classSystem);
		SI.addDeclaration(class_PrintStream);
		SI.addDeclaration(classString);

		SI.openScope();
		for(ClassDecl cd : prog.classDeclList) {
			for(FieldDecl fd : cd.fieldDeclList) {
				if(!fd.isPrivate) {
				SI.addDeclaration(fd, cd);
				}
			}
			for(MethodDecl md : cd.methodDeclList) {
				if(!md.isPrivate) {
					SI.addDeclaration(md, cd);
				}
			}
		}
		SI.addDeclaration(out, classSystem);
		SI.addDeclaration(println, class_PrintStream);

		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}
	// Declarations
	public Object visitClassDecl(ClassDecl cd, Object unused) {
		for(FieldDecl fd : cd.fieldDeclList) {
			if(fd.isPrivate) {
				SI.addDeclaration(fd, cd);
			}
		}
		for(MethodDecl md : cd.methodDeclList) {
			if(md.isPrivate) {
				SI.addDeclaration(md, cd);
			}
		}

		for(FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, cd);
		}
		for(MethodDecl md : cd.methodDeclList) {
			md.visit(this, cd);
		}

		for(FieldDecl fd : cd.fieldDeclList) {
			if(fd.isPrivate) {
				SI.removeDeclaration(fd, cd);
			}
		}
		for(MethodDecl md : cd.methodDeclList) {
			if(md.isPrivate) {
				SI.removeDeclaration(md, cd);
			}
		}
		return null;
	}
	public Object visitFieldDecl(FieldDecl fd, Object cd) {
		fd.type.visit(this, null);
		return null;
	}
	public Object visitMethodDecl(MethodDecl md, Object cd) {
		md.type.visit(this, null);
		inStatic = md.isStatic;
		SI.openScope();
		for (int i = 0; i < md.parameterDeclList.size(); i++) {
			md.parameterDeclList.get(i).visit(this, cd);
		}
		for (int i = 0; i < md.statementList.size(); i++) {
			md.statementList.get(i).visit(this, cd);
		}
		SI.closeScope();
		return null;
	}
	public Object visitParameterDecl(ParameterDecl pd, Object cd) {
		SI.addDeclaration(pd);
		pd.type.visit(this, cd);
		return null;
	}
	public Object visitVarDecl(VarDecl decl, Object cd) {
		decl.type.visit(this, cd);
		SI.addDeclaration(decl);
		return null;
	}
	// Types
	public Object visitBaseType(BaseType type, Object cd) { return null; }
	public Object visitClassType(ClassType type, Object cd) {
		ClassDecl TypeDeclaration = SI.findClassDeclaration(type.className);
		if (TypeDeclaration == null) {
			throw new IdentificationError(type,"Undefined class type: " + type.className.spelling);
		}
		return null;
	}
	public Object visitArrayType(ArrayType type, Object cd) {
		type.eltType.visit(this, null);
		return null;
	}
	// Statements
	public Object visitBlockStmt(BlockStmt stmt, Object cd) {
		SI.openScope();
		for (int i = 0; i < stmt.sl.size(); i++) {
			stmt.sl.get(i).visit(this, cd);
		}
		SI.closeScope();
		return null;
	}
	public Object visitVarDeclStmt(VarDeclStmt stmt, Object cd) {
		varDeclared = stmt.varDecl.name;
		stmt.initExp.visit(this, cd);
		stmt.varDecl.visit(this, cd);
		varDeclared = null;
		return null;
	}
	public Object visitAssignStmt(AssignStmt stmt, Object cd) {
		stmt.ref.visit(this, cd);
		stmt.val.visit(this, cd);
		return null;
	}
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object cd) {
		stmt.ix.visit(this, cd);
		stmt.exp.visit(this, cd);
		stmt.ref.visit(this, cd);
		return null;
	}
	public Object visitCallStmt(CallStmt stmt, Object cd) {
		stmt.methodRef.visit(this, cd);
		if(stmt.methodRef instanceof ThisRef) {
			throw new IdentificationError(stmt, "Identification Error: not a method");
		} else if (stmt.methodRef instanceof QualRef) {
			if (!(((QualRef) stmt.methodRef).id.decl instanceof MethodDecl)) {
				throw new IdentificationError(stmt, "Identification Error: not a method");
			}
		} else {
			if (!(((IdRef) stmt.methodRef).id.decl instanceof MethodDecl)) {
				throw new IdentificationError(stmt, "Identification Error: not a method");
			}
		}
		for(int i = 0; i < stmt.argList.size(); i++) {
			stmt.argList.get(i).visit(this, cd);
		}
		return null;
	}
	public Object visitReturnStmt(ReturnStmt stmt, Object cd) {
		stmt.returnExpr.visit(this, cd);
		return null;
	}
	public Object visitIfStmt(IfStmt stmt, Object cd) {
		stmt.cond.visit(this, cd);
		stmt.thenStmt.visit(this, cd);
		if(stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, cd);
		}
		if (stmt.thenStmt instanceof VarDeclStmt || stmt.elseStmt instanceof VarDeclStmt) {
			throw new IdentificationError(stmt, "solitary variable declaration statement not permitted here");
		}
		return null;
	}
	public Object visitWhileStmt(WhileStmt stmt, Object cd) {
		stmt.cond.visit(this, cd);
		stmt.body.visit(this, cd);
		if (stmt.body instanceof VarDeclStmt) {
			throw new IdentificationError(stmt, "solitary variable declaration statement not permitted here");
		}
		return null;
	}

	// Expressions
	public Object visitUnaryExpr(UnaryExpr expr, Object cd) {
		expr.operator.visit(this, cd);
		expr.expr.visit(this, cd);
		return null;
	}
	public Object visitBinaryExpr(BinaryExpr expr, Object cd) {
		expr.left.visit(this, cd);
		expr.operator.visit(this, cd);
		expr.right.visit(this, cd);
		return null;
	}
	public Object visitRefExpr(RefExpr expr, Object cd) {
		expr.ref.visit(this, cd);
		return null;
	}
	public Object visitIxExpr(IxExpr expr, Object cd) {
		expr.ref.visit(this, cd);
		expr.ixExpr.visit(this, cd);
		return null;
	}
	public Object visitCallExpr(CallExpr expr, Object cd) {
		expr.functionRef.visit(this, cd);
		for(int i = 0; i < expr.argList.size(); i++) {
			expr.argList.get(i).visit(this, cd);
		}
		return null;
	}
	public Object visitLiteralExpr(LiteralExpr expr, Object cd) {
		expr.lit.visit(this, cd);
		return null;
	}
	public Object visitNewObjectExpr(NewObjectExpr expr, Object cd) {
		expr.classtype.visit(this, cd);
		return null;
	}
	public Object visitNewArrayExpr(NewArrayExpr expr, Object cd) {
		expr.eltType.visit(this, cd);
		expr.sizeExpr.visit(this, cd);
		return null;
	}

	// References
	public Object visitThisRef(ThisRef ref, Object cd) {
		if(inStatic) {
			throw new IdentificationError(ref, "Cannot use this in static");
		}
		return cd;
	}

	public Object visitIdRef(IdRef ref, Object cd) {
		return ref.id.visit(this, cd);
	}

	public Object visitQRef(QualRef ref, Object cd) throws IdentificationError {
		ClassDecl left = (ClassDecl) ref.ref.visit(this, cd);
		if (left == null) {
			throw new IdentificationError(ref, "Identification Error: " + ref.id.spelling);
		}
		ClassDecl right = (ClassDecl) visitForeignIdentifier(ref.id, left);
		if (ref.id.decl == null) {
			throw new IdentificationError(ref, "Identification Error: " + ref.id.spelling);
		} else if (ref.id.decl instanceof ClassDecl || ref.id.decl instanceof MethodDecl) {
			return null;
		}
		if (ref.ref instanceof IdRef) {
			if (((IdRef) ref.ref).id.decl instanceof ClassDecl) {
				if(ref.id.decl instanceof MemberDecl) {
					if(!((MemberDecl) ref.id.decl).isStatic) {
						throw new IdentificationError(ref, "Identification Error: " + ref.id.spelling);
					}
				} else {
					throw new IdentificationError(ref, "Identification Error: " + ref.id.spelling);
				}
			}
		}
		return right;
	}

	// Terminals
	public Object visitIdentifier(Identifier id, Object cd) throws IdentificationError {
		if (id.spelling.equals(varDeclared)) {
			throw new IdentificationError(id, "Can't use var in its declaration");
		}
		SI.findDeclaration(id, (ClassDecl) cd);
		if(id.decl == null) {
			throw new IdentificationError(id, "Identification Error: " + id.spelling);
		} else if (id.decl instanceof ClassDecl) {
			return id.decl;
		} else if (id.decl.type instanceof ClassType) {
			return SI.findDeclaration(((ClassType) id.decl.type).className, (ClassDecl) cd);
		} else {
			return null;
		}
	}
	public Object visitOperator(Operator op, Object cd) { return null; }
	public Object visitIntLiteral(IntLiteral num, Object cd) { return null; }
	public Object visitBooleanLiteral(BooleanLiteral bool, Object cd) { return null; }
	public Object visitNullReference(NullReference nl, Object cd) { return null; }

	private Object visitForeignIdentifier(Identifier id, Object cd) throws IdentificationError {
		SI.findMethodDeclaration(id, (ClassDecl) cd);
		if(id.decl == null) {
			throw new IdentificationError(id, "Identification Error: " + id.spelling);
		} else if (id.decl instanceof ClassDecl) {
			return id.decl;
		} else if (id.decl.type instanceof ClassType) {
			return SI.findDeclaration(((ClassType) id.decl.type).className, (ClassDecl) cd);
		} else {
			return null;
		}
	}


	static class IdentificationError extends Error {
		private static final long serialVersionUID = -441346906191470192L;
		private String _errMsg;
		
		public IdentificationError(AST ast, String errMsg) {
			super();
			this._errMsg = ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg;
		}
		
		@Override
		public String toString() {
			return _errMsg;
		}
	}
}