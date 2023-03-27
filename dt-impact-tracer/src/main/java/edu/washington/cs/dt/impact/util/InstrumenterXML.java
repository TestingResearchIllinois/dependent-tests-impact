package edu.washington.cs.dt.impact.util;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class InstrumenterXML  extends SceneTransformer {
    private static Constants.TECHNIQUE technique = Constants.DEFAULT_TECHNIQUE;
    private Document doc;
    private Element rootElement;
    private Set<String> visitedMethods = new HashSet<>();
    private int testMethodIdCounter = 1;
    private Map<String, Element> testClassElements = new HashMap<>();

    public InstrumenterXML (Constants.TECHNIQUE t) {
        this.technique = t;
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing Instumrnterxml", e);
        }
    }

    protected boolean isTestMethod(SootMethod method) {
        if (!method.isConcrete()) {
            return false;
        }

        for (Tag tag : method.getTags()) {
            if (tag instanceof VisibilityAnnotationTag) {
                VisibilityAnnotationTag vat = (VisibilityAnnotationTag) tag;
                for (AnnotationTag at : vat.getAnnotations()) {
                    if (at.getType().equals("Lorg/junit/Test;")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        // You need to iterate over all the classes and their methods here
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod method : sootClass.getMethods()) {
                if (method.hasActiveBody()) {
                    internalTransform(method.getActiveBody(), phaseName, options);
                }
            }
        }
    }

    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        SootMethod method = body.getMethod();
        if (isTestMethod(method)) {
            if (rootElement == null) {
                rootElement = doc.createElement("testList");
                doc.appendChild(rootElement);
            }
            Element testClassElement = getTestClassElement(method.getDeclaringClass().getName());
            processMethodCallTree(method, doc, testClassElement, testMethodIdCounter++); // Increment the testMethodIdCounter after processing
        }
    }

    private Element getTestClassElement(String className) {
        Element testClassElement = testClassElements.get(className);
        if (testClassElement == null) {
            testClassElement = doc.createElement("testClass");
            testClassElement.setAttribute("name", className);
            rootElement.appendChild(testClassElement);
            testClassElements.put(className, testClassElement);
        }
        return testClassElement;
    }

    private void processMethodCallTree(SootMethod method, Document doc, Element parentElement, int index) {
        visitedMethods.add(method.getSignature());
        if (method.hasActiveBody()) {
            processMethod(method, doc, parentElement, index);
        }
    }

    private void processMethod(SootMethod method, Document doc, Element parentElement, int index) {
        try {
            System.out.println("Processing method: " + method.getSignature());

            if (method.getName().equals("<init>")) {
                return;
            }

            if (method.hasActiveBody()) {
                Body body = method.getActiveBody();

                Element methodElement = parentElement;
                if (!parentElement.getTagName().equals("testMethod")) {
                    methodElement = doc.createElement(isTestMethod(method) ? "testMethod" : "method");
                    methodElement.setAttribute("id", Integer.toString(index));
                    methodElement.setAttribute("name", method.getDeclaringClass().getName() + "." + method.getName());
                    methodElement.setAttribute("testType", Boolean.toString(isTestMethod(method)));
                    methodElement.setAttribute("time", "");
                    methodElement.setAttribute("throwException", "false");
                    parentElement.appendChild(methodElement);
                }

                int invokedMethodIndex = 1;
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof Stmt) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            SootMethod invokedMethod = invokeExpr.getMethod();

                            if ((invokedMethod.getName().equals("<init>"))) {
                                continue;
                            }

                            String invokedMethodId = methodElement.getAttribute("id") + "." + invokedMethodIndex++;

                            Element invokedMethodElement = doc.createElement("invokedMethod");
                            invokedMethodElement.setAttribute("id", invokedMethodId);
                            invokedMethodElement.setAttribute("name", invokedMethod.getDeclaringClass().getName() + "." + invokedMethod.getName());
                            invokedMethodElement.setAttribute("testType", Boolean.toString(isTestMethod(invokedMethod)));
                            invokedMethodElement.setAttribute("time", "");
                            invokedMethodElement.setAttribute("throwException", "false");
                            methodElement.appendChild(invokedMethodElement);

                            if (isTestMethod(method)) {
                                // Recursively process the invoked method and its children
                                processMethod(invokedMethod, doc, invokedMethodElement, invokedMethodIndex - 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while processing method " + method.getSignature() + ": " + e.getMessage());
        }
    }


    public void generateXML(String outputFilePath) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            OutputStream outputStream = new FileOutputStream(outputFilePath);
            StreamResult result = new StreamResult(outputStream);

            transformer.transform(source, result);
            outputStream.close();

            System.out.println("XML saved to " + outputFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Error generating XML", e);
        }
    }
}