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
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class InstrumenterXML  extends SceneTransformer {
    private static Constants.TECHNIQUE technique = Constants.DEFAULT_TECHNIQUE;
    private Document doc;
    private Element rootElement;
    private Set<String> visitedMethods = new HashSet<>();
    private int testMethodIdCounter = 1;
    private Map<String, Element> testClassElements = new HashMap<>();
    private String targetTestMethodName;

    public InstrumenterXML(Constants.TECHNIQUE t, String targetTestMethodName) {
        this.technique = t;
        this.targetTestMethodName = targetTestMethodName;
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
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod method : sootClass.getMethods()) {

                String fullyQualifiedName=method.getDeclaringClass().getName()+"."+method.getName();
                if (method.hasActiveBody() && isTestMethod(method) && fullyQualifiedName.contains(targetTestMethodName)) {
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

                            Element invokedMethodElement = doc.createElement("method");
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
            try {
                String xmlFile1 = "/home/pious/Documents/work/dependent-tests-impact/lib-results/sootXML-firstVers/firstVers-runtime.xml";
                String xmlFile2 = "/home/pious/Documents/work/dependent-tests-impact/lib-results/output.xml";
                String outputFile = "/home/pious/Documents/work/dependent-tests-impact/lib-results/resulted.xml";
                mergeXMLFiles(xmlFile1, xmlFile2, outputFile);
                System.out.println("Merged XML files successfully.");
            } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating XML", e);
        }
    }
    public static void mergeXMLFiles(String xmlFile1, String xmlFile2, String outputFile) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc1 = builder.parse(new File(xmlFile1));
        Document doc2 = builder.parse(new File(xmlFile2));

        doc1.getDocumentElement().normalize();
        doc2.getDocumentElement().normalize();

        NodeList testClasses1 = doc1.getElementsByTagName("testClass");
        NodeList testClasses2 = doc2.getElementsByTagName("testClass");

        // Iterate over testClasses1
        for (int i = 0; i < testClasses1.getLength(); i++) {
            Element testClass1 = (Element) testClasses1.item(i);
            String testClassName1 = testClass1.getAttribute("name");

            // Find a matching testClass in testClasses2
            Element testClass2 = null;
            for (int j = 0; j < testClasses2.getLength(); j++) {
                Element candidateTestClass2 = (Element) testClasses2.item(j);
                if (candidateTestClass2.getAttribute("name").equals(testClassName1)) {
                    testClass2 = candidateTestClass2;
                    break;
                }
            }

            if (testClass2 == null) {
                continue;
            }

            NodeList testMethods1 = testClass1.getElementsByTagName("testMethod");
            NodeList testMethods2 = testClass2.getElementsByTagName("testMethod");

            // Iterate over testMethods1
            for (int j = 0; j < testMethods1.getLength(); j++) {
                Element testMethod1 = (Element) testMethods1.item(j);
                String testMethodName1 = testMethod1.getAttribute("name");

                // Find a matching testMethod in testMethods2
                Element testMethod2 = null;
                for (int k = 0; k < testMethods2.getLength(); k++) {
                    Element candidateTestMethod2 = (Element) testMethods2.item(k);
                    if (candidateTestMethod2.getAttribute("name").equals(testMethodName1)) {
                        testMethod2 = candidateTestMethod2;
                        break;
                    }
                }

                if (testMethod2 == null) {
                    continue;
                }

                NodeList methods1 = testMethod1.getElementsByTagName("method");
                NodeList methods2 = testMethod2.getElementsByTagName("method");

                int m1Index = 0;
                int m2Index = 0;
                while (m1Index < methods1.getLength() && m2Index < methods2.getLength()) {
                    Element method1 = (Element) methods1.item(m1Index);
                    Element method2 = (Element) methods2.item(m2Index);

                    if (method1.getAttribute("name").equals(method2.getAttribute("name"))) {
                        m1Index++;
                        m2Index++;
                    } else {
                        Node nextSibling = m1Index + 1 < methods1.getLength() ? methods1.item(m1Index + 1) : null;
                        if (nextSibling != null) {
                            testMethod1.insertBefore(doc1.importNode(method2, true), nextSibling);
                        } else {
                            testMethod1.appendChild(doc1.importNode(method2, true));
                        }
                        m2Index++;
                        methods1 = testMethod1.getElementsByTagName("method");
                    }
                }

                while (m2Index < methods2.getLength()) {
                    Element method2 = (Element) methods2.item(m2Index);
                    testMethod1.appendChild(doc1.importNode(method2, true));
                    m2Index++;
                }
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc1);
        StreamResult result = new StreamResult(new FileOutputStream(new File(outputFile)));

        transformer.transform(source, result);
    }

}