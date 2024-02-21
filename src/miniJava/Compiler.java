package miniJava;

import miniJava.SyntacticAnalyzer.*;
import miniJava.AbstractSyntaxTrees.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

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
			ASTDisplay display = new ASTDisplay();
			display.showTree(tree);
		}
	}
}