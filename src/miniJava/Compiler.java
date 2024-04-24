package miniJava;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.SyntacticAnalyzer.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ContextualAnalysis.*;

import java.io.*;

// 335, 336, 340, 350, 351, 390
// 322, 327, 355

public class Compiler {
	// Main function, the file to compile will be an argument.
	public static void main(String[] args) throws FileNotFoundException {
		ErrorReporter reporter = new ErrorReporter();
		String path = args[0];
		InputStream in = new FileInputStream(new File(path));
		Scanner scanner = new Scanner(in, reporter);
		Parser parser = new Parser(scanner, reporter);
		AST tree = parser.parse();
		if(reporter.hasErrors()) {
			System.out.println("Error");
			reporter.outputErrors();
		}
		else {
			Identification identification = new Identification(reporter);
			identification.parse((Package) tree);
			if(reporter.hasErrors()) {
				System.out.println("Error");
				reporter.outputErrors();
			}
			else {
				TypeChecking typeChecking = new TypeChecking(reporter);
				typeChecking.parse((Package) tree);
				if(reporter.hasErrors()) {
					System.out.println("Error");
					reporter.outputErrors();
				}
				else {
					CodeGenerator codeGen = new CodeGenerator(reporter);
					codeGen.parse((Package) tree);
					if(reporter.hasErrors()) {
						System.out.println("Error");
						reporter.outputErrors();
					}
					else {
						System.out.println("Success");
					}
					//ASTDisplay display = new ASTDisplay();
					//display.showTree(tree);
				}
			}
		}
	}
}