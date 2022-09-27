package edu.washington.cs.dt.impact.util;

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
import com.github.javaparser.printer.XmlPrinter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;

public class MethodVisitor extends VoidVisitorAdapter
{
    private static void tryCreateDirectory(final File theDir) {
        try {
            Files.createDirectory(theDir.toPath());
        } catch (FileAlreadyExistsException ignored) {
            // The directory must have been created in between the check above and our attempt to create it.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void visit(MethodDeclaration n, Object arg)
    {
	File theDir = new File("xmlOutput");
	tryCreateDirectory(theDir);
	FileWriter output = null;
        BufferedWriter writer = null;
	try {
	    XmlPrinter printer =  new XmlPrinter(true);
	    output = new FileWriter("xmlOutput" + File.separator + arg.toString() + "."+ n.getName().asString());
            writer = new BufferedWriter(output);
	    writer.write(printer.output(n));
	} catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
        // extract method information here.                                                                                                                                       
        // put in to hashmap                                                                                                                                                      
    }
}
