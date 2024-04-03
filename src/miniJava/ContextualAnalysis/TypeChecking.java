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

    private ClassDecl currentClass;
    private MethodDecl currentMethod;

	public Object visitPackage(Package prog, Object unused) {
		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}
	// Declarations
	public Object visitClassDecl(ClassDecl cd, Object unused) {
        currentClass = cd;
		for (FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, null);
		}
		for (MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
        currentClass = null;
		return null;
	}
	public Object visitFieldDecl(FieldDecl fd, Object unused) {
		return fd.type.visit(this, null);
	}
	public Object visitMethodDecl(MethodDecl md, Object unused) {
        currentMethod = md;
		md.type.visit(this, null);
		for (ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}
		for (Statement s : md.statementList) {
			s.visit(this, null);
		}
        currentMethod = null;
		return null;
	}
	public Object visitParameterDecl(ParameterDecl pd, Object unused) {
		return pd.type.visit(this, null);
	}
	public Object visitVarDecl(VarDecl decl, Object unused) {
		return decl.type.visit(this, null);
	}
	// Types
	public Object visitBaseType(BaseType type, Object unused) {
		return type;
	}
	public Object visitClassType(ClassType type, Object unused) {
		type.className.visit(this, null);
		return type;
	}
	public Object visitArrayType(ArrayType type, Object unused) {
		type.eltType.visit(this, null);
		return type;
	}
	// Statements
	public Object visitBlockStmt(BlockStmt stmt, Object unused) {
		for(Statement s : stmt.sl) {
			s.visit(this, null);
		}
		return null;
	}
	public Object visitVarDeclStmt(VarDeclStmt stmt, Object unused) {
        if (!areSameType((TypeDenoter) stmt.initExp.visit(this, null),
                (TypeDenoter) stmt.varDecl.visit(this, null))) {
            reportTypeError(stmt, "Initial expression is not this variable's type");
        }
		return null;
	}
	public Object visitAssignStmt(AssignStmt stmt, Object unused) {
        if (!areSameType((TypeDenoter) stmt.val.visit(this, null),
                (TypeDenoter) stmt.ref.visit(this, null))) {
            reportTypeError(stmt, "Assigning incorrect type");
        }
        return null;
    }
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object unused) {
        TypeDenoter refType = (TypeDenoter) stmt.ref.visit(this, null);
        if (!(refType instanceof ArrayType) && refType.typeKind != ERROR) {
            reportTypeError(stmt, "Brackets on something that is not an array");
        }
        TypeDenoter sizeType = (TypeDenoter) stmt.ix.visit(this, null);
        if (sizeType.typeKind != INT && sizeType.typeKind != ERROR) {
            reportTypeError(stmt, "Array size must be an integer");
        }
        if (!areSameType(((ArrayType) refType).eltType,
                (TypeDenoter) stmt.exp.visit(this, null))) {
            reportTypeError(stmt, "Assigning array element as wrong type");
        }
        return null;
    }
	public Object visitCallStmt(CallStmt stmt, Object unused) {
        TypeDenoter funcType = (TypeDenoter) stmt.methodRef.visit(this, null);
        if (funcType.typeKind != METHOD && funcType.typeKind != ERROR) {
            reportTypeError(stmt, "Tried to call something that isn't a function");
            return null;
        }
        MethodDecl md = null;
        if (stmt.methodRef instanceof IdRef) {
            md = (MethodDecl) ((IdRef) stmt.methodRef).id.decl;
        } else if (stmt.methodRef instanceof QualRef) {
            md = (MethodDecl) ((QualRef) stmt.methodRef).id.decl;
        }
        if (md.parameterDeclList.size() != stmt.argList.size()) {
            reportTypeError(stmt, "Too many or too few parameters");
            return null;
        }
        for(int i = 0; i < stmt.argList.size(); i++) {
            TypeDenoter et = md.parameterDeclList.get(i).type;
            TypeDenoter at = (TypeDenoter) stmt.argList.get(i).visit(this, null);
            if (!areSameType(et, at)) {
                reportTypeError(stmt, "A parameter is the wrong type");
                return null;
            }
        }
        if (stmt.methodRef instanceof ThisRef) {
            reportTypeError(stmt, "'this' isn't a function");
        }
        return null;
    }
	public Object visitReturnStmt(ReturnStmt stmt, Object unused) {
        TypeDenoter returned = (TypeDenoter) stmt.returnExpr.visit(this, null);
        if (!areSameType(returned, currentMethod.type)) {
            reportTypeError(stmt, "Wrong return type");
        }
        return null;
    }
    public Object visitIfStmt(IfStmt stmt, Object unused) {
        TypeDenoter cond = (TypeDenoter) stmt.cond.visit(this, null);
        if (cond.typeKind != BOOLEAN && cond.typeKind != ERROR) {
            reportTypeError(stmt, "If statement condition not a boolean");
        } else {
            stmt.thenStmt.visit(this, null);
            if (stmt.elseStmt != null) {
                stmt.elseStmt.visit(this, null);
            }
        }
        return null;
    }
	public Object visitWhileStmt(WhileStmt stmt, Object unused) {
        TypeDenoter cond = (TypeDenoter) stmt.cond.visit(this, null);
        if (cond.typeKind != BOOLEAN && cond.typeKind != ERROR) {
            reportTypeError(stmt, "While statement condition not a boolean");
        } else {
            stmt.body.visit(this, null);
        }
        return null;
    }

	// Expressions
	public Object visitUnaryExpr(UnaryExpr expr, Object unused) {
        TypeDenoter e = (TypeDenoter) expr.operator.visit(this, null);
        TypeDenoter a = (TypeDenoter) expr.expr.visit(this, null);
        if (!areSameType(e, a)) {
            reportTypeError(expr, "Object types do not match what is expected by UNOP");
            return new BaseType(ERROR, null);
        } else {
            return e;
        }
    }
	public Object visitBinaryExpr(BinaryExpr expr, Object unused) {
        TypeDenoter expected = (TypeDenoter) expr.operator.visit(this, null);
        TypeDenoter left = (TypeDenoter) expr.left.visit(this, null);
        TypeDenoter right = (TypeDenoter) expr.right.visit(this, null);
        if (!areSameType(left, right)) {
            reportTypeError(expr, "Binary expression on object types that do not match");
            return new BaseType(ERROR, null);
        }
        if(
                !(expected == null) &&
                !(expected.typeKind == BOOLEAN && left.typeKind == BOOLEAN) &&
                !(expected.typeKind == INT && left.typeKind == INT)
        ) {
            reportTypeError(expr, "Object types do not match what is expected by BINOP");
            return new BaseType(ERROR, null);
        } else {
            TypeDenoter out;
            if(
                    expr.operator.spelling.equals("&&") || expr.operator.spelling.equals("||") ||
                    expr.operator.spelling.equals(">") || expr.operator.spelling.equals(">=") ||
                    expr.operator.spelling.equals("<") || expr.operator.spelling.equals("<=") ||
                    expr.operator.spelling.equals("==") || expr.operator.spelling.equals("!=")
            ) {
                return new BaseType(BOOLEAN, null);
            }
            else if(
                    expr.operator.spelling.equals("+") || expr.operator.spelling.equals("-") ||
                    expr.operator.spelling.equals("*") || expr.operator.spelling.equals("/")
            ) {
                return new BaseType(INT, null);
            }
            else {
                reportTypeError(expr, "Unexpected Binary Expression Error");
                return new BaseType(ERROR, null);
            }
        }
    }
	public Object visitRefExpr(RefExpr expr, Object unused) {
        return expr.ref.visit(this, null);
    }
	public Object visitIxExpr(IxExpr expr, Object unused) {
        TypeDenoter sizeType = (TypeDenoter) expr.ixExpr.visit(this, null);
        if (sizeType.typeKind != INT && sizeType.typeKind != ERROR) {
            reportTypeError(expr, "Array size must be an integer");
            return new BaseType(ERROR, null);
        }
        expr.ref.visit(this, null);
        if (expr.ref instanceof QualRef) {
            return ((ArrayType) ((QualRef) expr.ref).id.decl.type).eltType;
        } else if (expr.ref instanceof IdRef) {
            return ((ArrayType) ((IdRef) expr.ref).id.decl.type).eltType;
        } else {
            return ((ArrayType) currentClass.type).eltType;
        }
    }
	public Object visitCallExpr(CallExpr expr, Object unused) {
        TypeDenoter funcType = (TypeDenoter) expr.functionRef.visit(this, null);
        if (funcType.typeKind != METHOD && funcType.typeKind != ERROR) {
            reportTypeError(expr, "Tried to call something that isn't a function");
            return new BaseType(ERROR, null);
        }
        MethodDecl md = null;
        if (expr.functionRef instanceof IdRef) {
            md = (MethodDecl) ((IdRef) expr.functionRef).id.decl;
        } else if (expr.functionRef instanceof QualRef) {
            md = (MethodDecl) ((QualRef) expr.functionRef).id.decl;
        }
        if (md.parameterDeclList.size() != expr.argList.size()) {
            reportTypeError(expr, "Too many or too few parameters");
            return new BaseType(ERROR, null);
        }
        for(int i = 0; i < expr.argList.size(); i++) {
            TypeDenoter et = md.parameterDeclList.get(i).type;
            TypeDenoter at = (TypeDenoter) expr.argList.get(i).visit(this, null);
            if (!areSameType(et, at)) {
                reportTypeError(expr, "A parameter is the wrong type");
                return new BaseType(ERROR, null);
            }
        }
        if (expr.functionRef instanceof IdRef) {
            return ((IdRef) expr.functionRef).id.decl.type;
        } else if (expr.functionRef instanceof QualRef) {
            return ((QualRef) expr.functionRef).id.decl.type;
        } else {
            reportTypeError(expr, "'this' isn't a function");
            return new BaseType(ERROR, null);
        }
    }
	public Object visitLiteralExpr(LiteralExpr expr, Object unused) {
        return expr.lit.visit(this, null);
    }
	public Object visitNewObjectExpr(NewObjectExpr expr, Object unused) {
        expr.classtype.visit(this, null);
        if (expr.classtype.typeKind != CLASS) {
            reportTypeError(expr, "Object must be a Class Type");
            return new BaseType(ERROR, null);
        }
        return expr.classtype;
    }
	public Object visitNewArrayExpr(NewArrayExpr expr, Object unused) {
        TypeDenoter eltType = (TypeDenoter) expr.eltType.visit(this, null);
        TypeDenoter sizeType = (TypeDenoter) expr.sizeExpr.visit(this, null);
        if (sizeType.typeKind != INT && sizeType.typeKind != ERROR) {
            reportTypeError(expr, "Array size must be an integer");
            return new BaseType(ERROR, null);
        }
        if (eltType.typeKind != INT && eltType.typeKind != CLASS && eltType.typeKind != ERROR) {
            reportTypeError(expr, "Array element type must be int or a Class Type");
            return new BaseType(ERROR, null);
        }
        return new ArrayType(eltType, null);
    }

	// References
	public Object visitThisRef(ThisRef ref, Object unused) {
        return currentClass.type;
    }
	public Object visitIdRef(IdRef ref, Object unused) {
        return ref.id.visit(this, null);
	}

	public Object visitQRef(QualRef ref, Object unused) {
        return ref.id.visit(this, null);
	}

	// Terminals
	public Object visitIdentifier(Identifier id, Object unused) {
        if (id.decl instanceof MethodDecl) {
            return new BaseType(METHOD, null);
        } else if (id.decl instanceof ClassDecl) {
            return new BaseType(CLASSNAME, null);
        } else {
            return id.decl.type;
        }
	}
	public Object visitOperator(Operator op, Object unused) {
        if(
                op.spelling.equals("+") || op.spelling.equals("-") ||
                op.spelling.equals("*") || op.spelling.equals("/") ||
                op.spelling.equals(">") || op.spelling.equals(">=") ||
                op.spelling.equals("<") || op.spelling.equals("<=")
        ) { return new BaseType(INT, null); }
        else if(
                op.spelling.equals("&&") || op.spelling.equals("||") ||
                op.spelling.equals("!")
        ) { return new BaseType(BOOLEAN, null); }
        else { return null; }	//any, but still has to match
	}
	public Object visitIntLiteral(IntLiteral num, Object unused) {
		return new BaseType(INT, null);
	}
	public Object visitBooleanLiteral(BooleanLiteral bool, Object unused) {
		return new BaseType(BOOLEAN, null);
	}
	public Object visitNullReference(NullReference nl, Object unused) {
        return new BaseType(NULL, null);
    }


	private void reportTypeError(AST ast, String errMsg) {
		_errors.reportError( ast.posn == null
				? "*** " + errMsg
				: "*** " + ast.posn.toString() + ": " + errMsg );
	}

    private boolean areSameType(TypeDenoter x, TypeDenoter y) {
        if (x.typeKind == ERROR || y.typeKind == ERROR) {
            return true;
        } else if (x.typeKind == UNSUPPORTED || y.typeKind == UNSUPPORTED) {
            return false;
        } else if (x.typeKind == CLASS && y.typeKind == CLASS) {
            return ((ClassType) x).className.spelling.equals(((ClassType) y).className.spelling);
        } else if (x.typeKind == ARRAY && y.typeKind == ARRAY) {
            return areSameType(((ArrayType) x).eltType, ((ArrayType) y).eltType);
        } else {
            return x.typeKind == y.typeKind;
        }
    }
}