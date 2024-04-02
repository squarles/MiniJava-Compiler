package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NullReference extends BaseRef {
    public NullReference(SourcePosition posn){
        super(posn);
    }

    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitNullReference(this, o);
    }
}
