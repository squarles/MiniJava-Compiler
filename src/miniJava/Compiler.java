package miniJava;

import miniJava.AbstractSyntaxTrees.Package;
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
		Scanner scanner = new Scanner(in, reporter);
		Parser parser = new Parser(scanner, reporter);
		Identification identification = new Identification(reporter);
		AST tree = parser.parse();
		identification.parse((Package) tree);
		if(reporter.hasErrors()) {
			System.out.println("Error");
			reporter.outputErrors();
		}
		else {
			System.out.println("Success");
			//ASTDisplay display = new ASTDisplay();
			//display.showTree(tree);
		}
	}
}