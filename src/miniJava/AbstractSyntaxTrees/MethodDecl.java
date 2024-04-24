/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.SyntacticAnalyzer.SourcePosition;

import java.util.ArrayList;
import java.util.List;

public class MethodDecl extends MemberDecl {
    public List<Instruction> IncompleteCallList = new ArrayList<Instruction>();
    public int StartAddress = -1;
    public boolean isMain = false;
    public boolean isPrintln = false;
	
	public MethodDecl(MemberDecl md, ParameterDeclList pl, StatementList sl, SourcePosition posn){
    super(md,posn);
    parameterDeclList = pl;
    statementList = sl;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }
	
	public ParameterDeclList parameterDeclList;
	public StatementList statementList;
}
