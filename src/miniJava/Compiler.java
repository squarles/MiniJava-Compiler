package miniJava;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.SyntacticAnalyzer.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ContextualAnalysis.*;

import java.io.*;

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws FileNotFoundException {
		ErrorReporter reporter = new ErrorReporter();
		String path = args[0];
		InputStream in = new FileInputStream(new File(path));
		Parser parser = new Parser(new Scanner(in, reporter), reporter);
		Package pkg = (Package) parser.parse();
		if(reporter.hasErrors()) {
			System.out.println("Error");
			reporter.outputErrors();
		}
		else {
			Identification identifier = new Identification(reporter);
			identifier.parse(pkg);
			if(reporter.hasErrors()) {
				System.out.println("Error");
				reporter.outputErrors();
			}
			else {
				TypeChecking typeChecker = new TypeChecking(reporter);
				typeChecker.parse(pkg);
				if(reporter.hasErrors()) {
					System.out.println("Error");
					reporter.outputErrors();
				}
				else {
					CodeGenerator generator = new CodeGenerator(reporter);
					generator.parse(pkg);
					if(reporter.hasErrors()) {
						System.out.println("Error");
						reporter.outputErrors();
					}
					else {
						System.out.println("Success");
					}
					ASTDisplay display = new ASTDisplay();
					display.showTree(pkg);
				}
			}
		}
	}
}