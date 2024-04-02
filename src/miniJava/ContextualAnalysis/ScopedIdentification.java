package miniJava.ContextualAnalysis;


import miniJava.AbstractSyntaxTrees.*;

import java.util.HashMap;
import java.util.Stack;

import miniJava.ContextualAnalysis.Identification.IdentificationError;

public class ScopedIdentification {

    private Stack<HashMap<String,Declaration>> stack;

    public ScopedIdentification(){
        stack = new Stack<HashMap<String,Declaration>>();
    }


    public void openScope(){
        stack.push(new HashMap<String, Declaration>());
    }
    public void closeScope(){
        stack.pop();
    }
    public void addDeclaration(Declaration declaration) throws IdentificationError {
        String name = declaration.name;
        if(stack.size() > 2) {
            for (int i = 2; i < stack.size() - 1; i++) {
                if (stack.get(i).get(name) != null) {
                    throw new IdentificationError(declaration, "Identification Error - Declared Twice");
                }
            }
        }
        if(stack.peek().get(name) == null) {
            stack.peek().put(name, declaration);
        } else {
            throw new IdentificationError(declaration, "Identification Error - Declared Twice");
        }
    }

    public void addDeclaration(Declaration declaration, ClassDecl cd) throws IdentificationError {
        String name = cd.name + "." + declaration.name;
        if(stack.peek().get(name) == null) {
            stack.peek().put(name, declaration);
        } else {
            throw new IdentificationError(declaration, "Identification Error - Declared Twice");
        }
    }

    public void removeDeclaration(Declaration declaration, ClassDecl cd){
        stack.peek().remove(cd.name + "." + declaration.name);
    }

    public Declaration findDeclaration(Identifier id, ClassDecl cd){
        for(int i = stack.size() - 1; i >= 2; i--) {
            if(stack.get(i).get(id.spelling) != null) {
                id.decl = stack.get(i).get(id.spelling);
                return id.decl;
            }
        }
        if(stack.get(1).get(cd.name + "." + id.spelling) != null) {
            id.decl = stack.get(1).get(cd.name + "." + id.spelling);
            return id.decl;
        }
        return findClassDeclaration(id);
    }

    public ClassDecl findClassDeclaration(Identifier id){
        if(stack.get(0).get(id.spelling) != null) {
            id.decl = stack.get(0).get(id.spelling);
            return (ClassDecl) id.decl;
        }
        return null;
    }

    public void printStack(){
        for (int i = 0; i < stack.size(); i++) {
            System.out.println(i + ": " + stack.get(i).keySet());
        }
    }
}
