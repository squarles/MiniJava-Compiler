/*
ModRMSIB - is about the register's operands, not the instruction itself
Constructs opcode's parameters
Passed into an instruction that will use it
Instruction turns into bytes

Visit is more independent of each other
Push result onto the stack - can use whatever register it wants
 */


package miniJava.CodeGeneration;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.x64.*;
import miniJava.CodeGeneration.x64.ISA.*;

/*
put static variables on the stack first
top of stack = start of bss
static access = top stack offset
save the top of the stack in a register = R15

methods - if it's not static, pass this as a parameter
static -
 */

public class CodeGenerator implements Visitor<Object, Object> {
	private ErrorReporter _errors;
	private InstructionList _asm; // our list of instructions that are used to make the code section

	private int EntryPoint;
	private int CurrentRBPOffsetV = -8;
	private int CurrentRBPOffsetP = 16;
	private int NumStaticFields = 0;
	private FieldDeclList Statics = new FieldDeclList();

	private MethodDecl CurrentMethod = null;

	public CodeGenerator(ErrorReporter errors) {
		this._errors = errors;
	}
	
	public void parse(Package prog) {
		_asm = new InstructionList();
		
		// If you haven't refactored the name "ModRMSIB" to something like "R",
		//  go ahead and do that now. You'll be needing that object a lot.
		// Here is some example code.
		
		// Simple operations:
		// _asm.add( new Push(0) ); // push the value zero onto the stack
		// _asm.add( new Pop(Reg64.RCX) ); // pop the top of the stack into RCX
		
		// Fancier operations:
		// _asm.add( new Cmp(new R(Reg64.RCX,Reg64.RDI)) ); // cmp rcx,rdi
		// _asm.add( new Cmp(new R(Reg64.RCX,0x10,Reg64.RDI)) ); // cmp [rcx+0x10],rdi
		// _asm.add( new Add(new R(Reg64.RSI,Reg64.RCX,4,0x1000,Reg64.RDX)) ); // add [rsi+rcx*4+0x1000],rdx
		
		// Thus:
		// new R( ... ) where the "..." can be:
		//  RegRM, RegR						== rm, r
		//  RegRM, int, RegR				== [rm+int], r
		//  RegRD, RegRI, intM, intD, RegR	== [rd+ ri*intM + intD], r
		// Where RegRM/RD/RI are just Reg64 or Reg32 or even Reg8
		//
		// Note there are constructors for ModRMSIB where RegR is skipped.
		// This is usually used by instructions that only need one register operand, and often have an immediate
		//   So they actually will set RegR for us when we create the instruction. An example is:
		// _asm.add( new Mov_rmi(new R(Reg64.RDX,true), 3) ); // mov rdx,3
		//   In that last example, we had to pass in a "true" to indicate whether the passed register
		//    is the operand RM or R, in this case, true means RM
		//  Similarly:
		// _asm.add( new Push(new R(Reg64.RBP,16)) );
		//   This one doesn't specify RegR because it is: push [rbp+16] and there is no second operand register needed
		
		// Patching example:
		// Instruction someJump = new Jmp((int)0); // 32-bit offset jump to nowhere
		// _asm.add( someJump ); // populate listIdx and startAddress for the instruction
		// ...
		// ... visit some code that probably uses _asm.add
		// ...
		// patch method 1: calculate the offset yourself
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size() - someJump.startAddress - 5) );
		// -=-=-=-
		// patch method 2: let the jmp calculate the offset
		//  Note the false means that it is a 32-bit immediate for jumping (an int)
		//     _asm.patch( someJump.listIdx, new Jmp(asm.size(), someJump.startAddress, false) );
		
		prog.visit(this,null);
		
		// Output the file "a.out" if no errors
		if( !_errors.hasErrors() )
			makeElf("a.out");
	}

	@Override //TODO: static fields
	public Object visitPackage(Package prog, Object arg) {
		for(ClassDecl cd : prog.classDeclList) {
			int CurrentOffset = 0;
			for (FieldDecl fd : cd.fieldDeclList) {
				if(!fd.isStatic) {
					fd.HeapOffset = CurrentOffset;
					CurrentOffset += 8;
				} else {
					NumStaticFields++;
					Statics.add(fd);
				}
			}
		}
		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, Object arg){
		for(MethodDecl md : cd.methodDeclList) {
			md.visit(this, null);
		}
		return null;
	}
	@Override
	public Object visitFieldDecl(FieldDecl fd, Object arg){ return null; }
	@Override
	public Object visitMethodDecl(MethodDecl md, Object arg){
		CurrentRBPOffsetP = 16;
		if(!md.isStatic) {
			CurrentRBPOffsetP += 8;
		}
		CurrentRBPOffsetV = -8;
		md.StartAddress = _asm.getSize();
		for (Instruction call: md.IncompleteCallList) {
			_asm.patch(call.listIdx, new Call(call.startAddress, md.StartAddress));
		}
		if(md.isMain) {
			EntryPoint = md.StartAddress;
			int R15Offset = 0;
			for (FieldDecl fd : Statics) {
				fd.HeapOffset = R15Offset;
				R15Offset += 8;
				_asm.add(new Push(0));
			}
			_asm.add(new Mov_rmr(new R(Reg64.R15, Reg64.RSP)));
		}
		_asm.add(new Push(Reg64.RBP));
		_asm.add(new Mov_rmr(new R(Reg64.RBP,Reg64.RSP)));
		CurrentMethod = md;
		for(ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, null);
		}
		for(Statement s : md.statementList) {
			s.visit(this, null);
		}
		CurrentMethod = null;
		_asm.add(new Mov_rmr(new R(Reg64.RSP,Reg64.RBP)));
		_asm.add(new Pop(Reg64.RBP));
		if(md.isMain) {
			_asm.add(new Mov_rmi(new R(Reg64.RAX,true),60)); // exit
			_asm.add(new Xor(new R(Reg64.RDI,Reg64.RDI))); // error code is 0
			_asm.add(new Syscall());
		} else if(md.type.typeKind == TypeKind.VOID) {
			int numParams = md.parameterDeclList.size();
			if(!md.isStatic) {
				numParams++;
			}
			_asm.add(new Ret((short) numParams, (short) 8));
		}
		return null;
	}
	@Override
	public Object visitParameterDecl(ParameterDecl pd, Object arg){
		pd.RBPOffset = CurrentRBPOffsetP;
		CurrentRBPOffsetP += 8;
		return null;
	}
	@Override
	public Object visitVarDecl(VarDecl decl, Object arg) {
		decl.RBPOffset = CurrentRBPOffsetV;
		CurrentRBPOffsetV -= 8;
		_asm.add(new Push(0));
		return null;
	}

	// Types
	@Override
	public Object visitBaseType(BaseType type, Object arg){return null;}
	@Override
	public Object visitClassType(ClassType type, Object arg){return null;}
	@Override
	public Object visitArrayType(ArrayType type, Object arg){return null;}

	// Statements
	@Override
	public Object visitBlockStmt(BlockStmt stmt, Object arg){
		int StartingOffset = CurrentRBPOffsetV;
		for(Statement s : stmt.sl) {
			s.visit(this, null);
		}
		if(CurrentRBPOffsetV != StartingOffset) {
			int difference = CurrentRBPOffsetV - StartingOffset;
			CurrentRBPOffsetV = StartingOffset;
			_asm.add(new Add(new R(Reg64.RSP, true), difference));
		}
		return null;
	}
	@Override
	public Object visitVarDeclStmt(VarDeclStmt stmt, Object arg){
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		_asm.add(new Pop(new R(Reg64.RBP, stmt.varDecl.RBPOffset)));
		return null;
	}
	@Override
	public Object visitAssignStmt(AssignStmt stmt, Object arg){
		stmt.ref.visit(this, (Object) Boolean.TRUE);
		stmt.val.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Mov_rmr(new R(Reg64.RCX, 0, Reg64.RAX)));
		return null;
	}
	@Override
	public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg){
		stmt.exp.visit(this, null);
		stmt.ref.visit(this, null);
		stmt.ix.visit(this, null);
		_asm.add(new Pop(Reg64.RAX)); //[RCX+RAX*8+0] = RDX;
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RDX));
		_asm.add(new Mov_rmr(new R(Reg64.RCX, Reg64.RAX, 8, 0, Reg64.RDX)));
		return null;
	}
	@Override
	public Object visitCallStmt(CallStmt stmt, Object arg){
		boolean DeclIsPrintLn = false;
		MethodDecl md;
		if(stmt.methodRef instanceof QualRef) {
			md = (MethodDecl) ((QualRef) stmt.methodRef).id.decl;
			if(md.isPrintln) {
				DeclIsPrintLn = true;
			}
		} else {
			md = (MethodDecl) ((IdRef) stmt.methodRef).id.decl;
			if(md.isPrintln) {
				DeclIsPrintLn = true;
			}
		}
		if(DeclIsPrintLn) {
			stmt.argList.get(0).visit(this, null);
			makePrintln();
			return null;
		}

		for (int i = stmt.argList.size() - 1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, null);
		}
		if(
				!CurrentMethod.isStatic &&
				stmt.methodRef instanceof IdRef &&
				!md.isStatic
		) {
			_asm.add(new Push(new R(Reg64.RBP, 16)));
		} else if (
				stmt.methodRef instanceof QualRef &&
						!md.isStatic
		) {
			QualRef qr = (QualRef) stmt.methodRef;
			qr.ref.visit(this, null);
		}
		Instruction call = new Call(_asm.getSize(),md.StartAddress);
		if(md.StartAddress == -1) {
			md.IncompleteCallList.add(call);
		}
		_asm.add(call);
		return null;
	}
	@Override
	public Object visitReturnStmt(ReturnStmt stmt, Object arg){
		if(stmt.returnExpr != null){
			stmt.returnExpr.visit(this, null);
			_asm.add(new Pop(Reg64.RAX));
		}
		_asm.add(new Mov_rmr(new R(Reg64.RSP,Reg64.RBP)));
		_asm.add(new Pop(Reg64.RBP));
		int numParams = CurrentMethod.parameterDeclList.size();
		if(!CurrentMethod.isStatic) {
			numParams++;
		}
		_asm.add(new Ret((short) numParams, (short) 8));
		return null;
	}
	@Override
	public Object visitIfStmt(IfStmt stmt, Object arg){

		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Cmp(new R(Reg64.RAX, true), 0));
		Instruction ifj = new CondJmp(Condition.E, 0);
		_asm.add(ifj);
		stmt.thenStmt.visit(this, null);
		if(stmt.elseStmt == null) {
			_asm.patch(ifj.listIdx, new CondJmp(Condition.E, ifj.startAddress, _asm.getSize(), false));
			return null;
		}

		Instruction thenj = new Jmp(0);
		_asm.add(thenj);
		_asm.patch(ifj.listIdx, new CondJmp(Condition.E, ifj.startAddress, _asm.getSize(), false));
		stmt.elseStmt.visit(this, null);
		_asm.patch(thenj.listIdx, new Jmp(thenj.startAddress, _asm.getSize(), false));
		return null;
	}
	@Override
	public Object visitWhileStmt(WhileStmt stmt, Object arg){
		int start = _asm.getSize();
		stmt.cond.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Cmp(new R(Reg64.RAX, true), 0));
		Instruction whilej = new CondJmp(Condition.E, 0);
		_asm.add(whilej);

		stmt.body.visit(this, null);
		_asm.add(new Jmp(_asm.getSize(), start, false));
		_asm.patch(whilej.listIdx, new CondJmp(Condition.E, whilej.startAddress, _asm.getSize(), false));
		return null;
	}

	// Expressions
	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Object arg){
		expr.expr.visit(this, null);
		if(expr.operator.spelling.equals('-')) {
			_asm.add(new Neg(new R(Reg64.RSP, 0)));
		} else {
			_asm.add(new Not(new R(Reg64.RSP, 0)));
			_asm.add(new And(new R(Reg64.RSP, 0), 1));
		}
		return null;
	}
	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Object arg){
		expr.left.visit(this, null);
		expr.right.visit(this, null);
		Condition cond = Condition.getCond(expr.operator);
		if(cond != null) {
			_asm.add(new Pop(Reg64.RCX));
			_asm.add(new Pop(Reg64.RDX));
			_asm.add(new Xor(new R(Reg64.RAX, Reg64.RAX)));
			_asm.add(new Cmp(new R(Reg64.RDX, Reg64.RCX)));
			_asm.add(new SetCond(cond,Reg8.AL));
			_asm.add(new Push(Reg64.RAX));
			return null;
		}
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Pop(Reg64.RAX));
		if(expr.operator.spelling.equals("+")) {
			_asm.add(new Add(new R(Reg64.RAX, Reg64.RCX)));
		} else if(expr.operator.spelling.equals("-")) {
			_asm.add(new Sub(new R(Reg64.RAX, Reg64.RCX)));
		} else if(expr.operator.spelling.equals("*")) {
			_asm.add(new Imul(new R(Reg64.RCX, true)));
		} else if(expr.operator.spelling.equals("/")) {
			_asm.add(new Xor(new R(Reg64.RDX, Reg64.RDX)));
			_asm.add(new Idiv(new R(Reg64.RCX, true)));
		} else if(expr.operator.spelling.equals("||")) {
			_asm.add(new Or(new R(Reg64.RAX, Reg64.RCX)));
		} else if(expr.operator.spelling.equals("&&")) {
			_asm.add(new And(new R(Reg64.RAX, Reg64.RCX)));
		}
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitRefExpr(RefExpr expr, Object arg){
		expr.ref.visit(this, null);
		return null;
	}
	@Override
	public Object visitIxExpr(IxExpr expr, Object arg){
		expr.ref.visit(this, null);
		expr.ixExpr.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		_asm.add(new Pop(Reg64.RCX));
		_asm.add(new Push(new R(Reg64.RCX, Reg64.RAX, 8, 0)));
		return null;
	}
	@Override
	public Object visitCallExpr(CallExpr expr, Object arg){
		boolean DeclIsPrintLn = false;
		MethodDecl md;
		if(expr.functionRef instanceof QualRef) {
			md = (MethodDecl) ((QualRef) expr.functionRef).id.decl;
			if(md.isPrintln) {
				DeclIsPrintLn = true;
			}
		} else {
			md = (MethodDecl) ((IdRef) expr.functionRef).id.decl;
			if(md.isPrintln) {
				DeclIsPrintLn = true;
			}
		}
		if(DeclIsPrintLn) {
			expr.argList.get(0).visit(this, null);
			makePrintln();
			return null;
		}

		for (int i = expr.argList.size() - 1; i >= 0; i--) {
			expr.argList.get(i).visit(this, null);
		}
		if(
				!CurrentMethod.isStatic &&
						expr.functionRef instanceof IdRef &&
						!md.isStatic
		) {
			_asm.add(new Push(new R(Reg64.RBP, 16)));
		} else if (
				expr.functionRef instanceof QualRef &&
				!md.isStatic
		) {
			QualRef qr = (QualRef) expr.functionRef;
			qr.ref.visit(this, null);
		}
		Instruction call = new Call(_asm.getSize(),md.StartAddress);
		if(md.StartAddress == -1) {
			md.IncompleteCallList.add(call);
		}
		_asm.add(call);
		_asm.add(new Push(Reg64.RAX)); // return value
		return null;
	}
	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Object arg){
		expr.lit.visit(this, null);
		return null;
	}
	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Object arg){
		makeMalloc();
		_asm.add(new Push(Reg64.RAX));
		return null;
	}
	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Object arg){
		makeMalloc();
		_asm.add(new Push(Reg64.RAX));
		return null;
	}

	// References
	@Override
	public Object visitThisRef(ThisRef ref, Object arg){
		_asm.add(new Push(new R(Reg64.RBP, 16)));
		return null;
	}
	@Override
	public Object visitIdRef(IdRef ref, Object arg){
		if (ref.id.decl instanceof LocalDecl) {
			LocalDecl ld = (LocalDecl) ref.id.decl;
			if(arg != null && (Boolean) arg == Boolean.TRUE) {
				_asm.add(new Lea(new R(Reg64.RBP, ld.RBPOffset,Reg64.RAX)));
			} else {
				_asm.add(new Mov_rrm(new R(Reg64.RBP, ld.RBPOffset, Reg64.RAX)));
			}
			_asm.add(new Push(Reg64.RAX));
		} else { //ref.id.decl instanceof FieldDecl
			FieldDecl fd = (FieldDecl) ref.id.decl;
			if (fd.isStatic) {
				if(arg != null && (Boolean) arg == Boolean.TRUE) {
					_asm.add(new Lea(new R(Reg64.R15, fd.HeapOffset,Reg64.RCX)));
				} else {
					_asm.add(new Mov_rrm(new R(Reg64.R15, fd.HeapOffset, Reg64.RCX)));
				}
			} else {
				_asm.add(new Mov_rrm(new R(Reg64.RBP, 16, Reg64.RAX)));
				if (arg != null && (Boolean) arg == Boolean.TRUE) {
					_asm.add(new Lea(new R(Reg64.RAX, fd.HeapOffset, Reg64.RCX)));
				} else {
					_asm.add(new Mov_rrm(new R(Reg64.RAX, fd.HeapOffset, Reg64.RCX)));
				}
			}
			_asm.add(new Push(Reg64.RCX));

		}
		return null;
	}

	@Override
	public Object visitQRef(QualRef ref, Object arg){
		/*
		LHS will be an object of some kind
		RHS is some identifier in the object
		Static will need to be fixed
		LHS can't be ClassDecl
		Value of LHS will be a heap address

		 */
		ref.ref.visit(this, null);
		_asm.add(new Pop(Reg64.RAX));
		FieldDecl fd = (FieldDecl) ref.id.decl;
		if(fd.isStatic) {
			if (arg != null && (Boolean) arg == Boolean.TRUE) {
				_asm.add(new Lea(new R(Reg64.R15, fd.HeapOffset, Reg64.RCX)));
			} else {
				_asm.add(new Mov_rrm(new R(Reg64.R15, fd.HeapOffset, Reg64.RCX)));
			}
		} else {
			if (arg != null && (Boolean) arg == Boolean.TRUE) {
				_asm.add(new Lea(new R(Reg64.RAX, fd.HeapOffset, Reg64.RCX)));
			} else {
				_asm.add(new Mov_rrm(new R(Reg64.RAX, fd.HeapOffset, Reg64.RCX)));
			}
		}
		_asm.add(new Push(Reg64.RCX));
		return null;
	}

	// Terminals
	@Override
	public Object visitIdentifier(Identifier id, Object arg){
		if (id.decl instanceof LocalDecl) {
			_asm.add(new Push(new R(Reg64.RBP, ((LocalDecl) id.decl).RBPOffset)));
		}
		return null;
	}
	@Override
	public Object visitOperator(Operator op, Object arg){return null;}
	@Override
	public Object visitIntLiteral(IntLiteral num, Object arg){
		_asm.add(new Push(Integer.parseInt(num.spelling)));
		return null;
	}
	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Object arg){
		if(Boolean.parseBoolean(bool.spelling)) {
			_asm.add(new Push(1));
		} else {
			_asm.add(new Push(0));
		}
		return null;
	}
	@Override
	public Object visitNullReference(NullReference nl, Object arg){
		_asm.add(new Push(0));
		return null;
	}


	public void makeElf(String fname) {
		ELFMaker elf = new ELFMaker(_errors, _asm.getSize(), 8); // bss ignored until PA5, set to 8
		elf.outputELF(fname, _asm.getBytes(), EntryPoint);
	}
	
	private int makeMalloc() {
		int idxStart = _asm.add( new Mov_rmi(new R(Reg64.RAX,true),0x09) ); // mmap
		
		_asm.add( new Xor(		new R(Reg64.RDI,Reg64.RDI)) 	); // addr=0
		_asm.add( new Mov_rmi(	new R(Reg64.RSI,true),0x1000) ); // 4kb alloc
		_asm.add( new Mov_rmi(	new R(Reg64.RDX,true),0x03) 	); // prot read|write
		_asm.add( new Mov_rmi(	new R(Reg64.R10,true),0x22) 	); // flags= private, anonymous
		_asm.add( new Mov_rmi(	new R(Reg64.R8, true),-1) 	); // fd= -1
		_asm.add( new Xor(		new R(Reg64.R9,Reg64.R9)) 	); // offset=0
		_asm.add( new Syscall() );
		
		// pointer to newly allocated memory is in RAX
		// return the index of the first instruction in this method, if needed
		return idxStart;
	}
	
	private int makePrintln() { //print char in RSP to the standard out
		_asm.add( new Mov_ri64( Reg64.RAX,0x01) 	);
		_asm.add( new Mov_ri64(	Reg64.RDI,0x01) 	);
		_asm.add( new Mov_rmr(	new R(Reg64.RSI,Reg64.RSP)) 	);
		_asm.add( new Mov_ri64(	Reg64.RDX,0x01) 	);
		_asm.add( new Syscall() );
		_asm.add( new Pop(Reg64.RAX));
		return -1;
	}
}
