import edu.washington.cs.dt.impact.util.MethodVisitor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.io.*;
/**                                                                                                                                                                               
 * Some code that uses JavaParser.                                                                                                                                                
 */

public class GeneratingAST {
    public static void createAST(){
	try {
	    String filename = "src/main/java/com/github/kevinsawicki/http/HttpRequest.java";
	    File sourceFile = new File(filename);
	    CompilationUnit compilationUnit = JavaParser.parse(sourceFile);
	    String package_path = filename.replace("src/main/java/", " ");
	    package_path = package_path.replace(".java", " ");
	    String[] parts =  package_path.trim().split("/");
	    String package_name = String.join(".", parts);
	    MethodVisitor visitor = new MethodVisitor();
	    visitor.visit(compilationUnit, package_name);
	} catch (FileNotFoundException e) {
	}
    }
    public static void main(String[] args) throws FileNotFoundException{
	List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        int inputFileIndex = argsList.indexOf("-inputFile");
	String input_file = argsList.get(inputFileIndex);
        System.out.println(inputFileIndex);
	System.out.println(input_file);
	try {
	    createAST();
	} catch (Exception e){
	    
	}
	//System.out.println(path.toAbsolutePath());
	//System.out.println(path.toRealPath());
	//System.out.println(path);
    }
}
