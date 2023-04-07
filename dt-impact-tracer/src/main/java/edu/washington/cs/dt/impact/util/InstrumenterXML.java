package edu.washington.cs.dt.impact.util;

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

public class InstrumenterXML  extends SceneTransformer {
    private static Constants.TECHNIQUE technique = Constants.DEFAULT_TECHNIQUE;
    private Document doc;
    private Element rootElement;
    private Set<String> visitedMethods = new HashSet<>();
    private int testMethodIdCounter = 1;
    private Map<String, Element> testClassElements = new HashMap<>();
    private List<String> targetTestMethodNames;

    public InstrumenterXML(Constants.TECHNIQUE t, List<String> targetTestMethodNames) {
        this.technique = t;
        this.targetTestMethodNames = targetTestMethodNames;
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

                String fullyQualifiedName = method.getDeclaringClass().getName() + "." + method.getName();
                boolean containsTargetTestMethod = targetTestMethodNames.stream().anyMatch(fullyQualifiedName::equals);
                targetTestMethodNames.removeIf(fullyQualifiedName::equals);
                if (method.hasActiveBody() && isTestMethod(method) && containsTargetTestMethod) {
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
    private boolean hasAncestorWithTag(Element element, String tagName) {
        Node parentNode = element.getParentNode();
        while (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
            if (((Element) parentNode).getTagName().equals(tagName)) {
                return true;
            }
            parentNode = parentNode.getParentNode();
        }
        return false;
    }

    private void processMethod(SootMethod method, Document doc, Element parentElement, int index) {
        try {
            //System.out.println("Processing method: " + method.getSignature());

            if (method.getName().equals("<init>")) {
                return;
            }

            if (method.hasActiveBody()) {
                Body body = method.getActiveBody();

                Element methodElement = parentElement;
                if (!hasAncestorWithTag(parentElement, "testMethod")) {
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


    public void generateXML(String dirpath){
        String outputFilePath=dirpath+"output.xml";
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Check if the document is empty
            if (doc.getDocumentElement() == null || doc.getDocumentElement().getChildNodes().getLength() == 0) {
                // Create a new XML document with the desired root element
                DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
                Document newDoc = documentBuilder.newDocument();
                Element rootElement = newDoc.createElement("testList");
                newDoc.appendChild(rootElement);

                // Update the doc reference
                doc = newDoc;
            }

            DOMSource source = new DOMSource(doc);

            OutputStream outputStream = new FileOutputStream(outputFilePath);
            StreamResult result = new StreamResult(outputStream);

            transformer.transform(source, result);
            outputStream.close();
            System.out.println("XML saved to " + outputFilePath);
            String xmlFile1 = dirpath+"sootXML-firstVers/firstVers-runtime.xml";
            String xmlFile2 = outputFilePath;
            String outputFile = dirpath+"secondVers-merged.xml";
            //System.out.println("---targeted-----"+targetTestMethodNames);
            try {
                compareAndGenerateXML(xmlFile1, xmlFile2, outputFile, targetTestMethodNames);
            } catch (IOException | ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating XML", e);
        }
    }
    private static Element createOutputTestClassWithoutTargetMethods(Document output, Node inputTestClass, List<String> targetTestMethodNames) {
        NodeList inputTestMethods = inputTestClass.getChildNodes();
        Element outputTestClass = output.createElement("testClass");
        outputTestClass.setAttribute("name", inputTestClass.getAttributes().getNamedItem("name").getNodeValue());

        boolean hasTestMethod = false;
        for (int i = 0; i < inputTestMethods.getLength(); i++) {
            Node inputTestMethod = inputTestMethods.item(i);
            if (inputTestMethod.getNodeType() == Node.ELEMENT_NODE && !targetTestMethodNames.contains(inputTestMethod.getAttributes().getNamedItem("name").getNodeValue())) {
                outputTestClass.appendChild(output.importNode(inputTestMethod, true));
                hasTestMethod = true;
            }
        }

        return hasTestMethod ? outputTestClass : null;
    }
    public static void compareAndGenerateXML(String inputFile1, String inputFile2, String outputFile, List<String> targetTestMethodNames) throws IOException, ParserConfigurationException, TransformerException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document input1 = dBuilder.parse(new File(inputFile1));
        Document input2 = dBuilder.parse(new File(inputFile2));
        Document output = dBuilder.newDocument();

        input1.getDocumentElement().normalize();
        input2.getDocumentElement().normalize();

        Element testList = output.createElement("testList");
        output.appendChild(testList);

        Map<String, Node> input1TestClassesMap = new HashMap<>();
        Map<String, Node> input2TestClassesMap = new HashMap<>();

        NodeList input1TestClasses = input1.getElementsByTagName("testClass");
        NodeList input2TestClasses = input2.getElementsByTagName("testClass");

        for (int i = 0; i < input1TestClasses.getLength(); i++) {
            Node input1TestClass = input1TestClasses.item(i);
            input1TestClassesMap.put(input1TestClass.getAttributes().getNamedItem("name").getNodeValue(), input1TestClass);
        }

        for (int i = 0; i < input2TestClasses.getLength(); i++) {
            Node input2TestClass = input2TestClasses.item(i);
            input2TestClassesMap.put(input2TestClass.getAttributes().getNamedItem("name").getNodeValue(), input2TestClass);
        }

        Set<String> allTestClassNames = new HashSet<>(input1TestClassesMap.keySet());
        allTestClassNames.addAll(input2TestClassesMap.keySet());

        for (String testClassName : allTestClassNames) {
            Node input1TestClass = input1TestClassesMap.get(testClassName);
            Node input2TestClass = input2TestClassesMap.get(testClassName);

            if (input1TestClass == null && input2TestClass != null) {
                Element outputTestClass = createOutputTestClassWithoutTargetMethods(output, input2TestClass, targetTestMethodNames);
                if (outputTestClass != null) {
                    testList.appendChild(outputTestClass);
                }
                continue;
            } else if (input1TestClass != null && input2TestClass == null) {
                Element outputTestClass = createOutputTestClassWithoutTargetMethods(output, input1TestClass, targetTestMethodNames);
                if (outputTestClass != null) {
                    testList.appendChild(outputTestClass);
                }
                continue;
            }

            Element outputTestClass = output.createElement("testClass");
            outputTestClass.setAttribute("name", testClassName);
            testList.appendChild(outputTestClass);

            Map<String, Node> input1TestMethodsMap = new HashMap<>();
            Map<String, Node> input2TestMethodsMap = new HashMap<>();

            NodeList input1TestMethods = input1TestClass.getChildNodes();
            NodeList input2TestMethods = input2TestClass.getChildNodes();

            for (int j = 0; j < input1TestMethods.getLength(); j++) {
                Node input1TestMethod = input1TestMethods.item(j);
                if (input1TestMethod.getNodeType() == Node.ELEMENT_NODE) {
                    input1TestMethodsMap.put(input1TestMethod.getAttributes().getNamedItem("name").getNodeValue(), input1TestMethod);
                }
            }

            for (int j = 0; j < input2TestMethods.getLength(); j++) {
                Node input2TestMethod = input2TestMethods.item(j);
                if (input2TestMethod.getNodeType() == Node.ELEMENT_NODE) {
                    input2TestMethodsMap.put(input2TestMethod.getAttributes().getNamedItem("name").getNodeValue(), input2TestMethod);
                }
            }

            Set<String> allTestMethodNames = new HashSet<>(input1TestMethodsMap.keySet());
            allTestMethodNames.addAll(input2TestMethodsMap.keySet());

            for (String testMethodName : allTestMethodNames) {
                int deletedflag=0;
                for (String testName : targetTestMethodNames) {

                    if (testName.equals(testMethodName)) {
                        deletedflag=1;
                        break;
                    }
                }
                if(deletedflag==1)
                {
                    continue;
                }
                Node input1TestMethod = input1TestMethodsMap.get(testMethodName);
                Node input2TestMethod = input2TestMethodsMap.get(testMethodName);

                if (input1TestMethod == null && input2TestMethod != null) {
                    outputTestClass.appendChild(output.importNode(input2TestMethod, true));
                } else if (input1TestMethod != null && input2TestMethod == null) {
                    outputTestClass.appendChild(output.importNode(input1TestMethod, true));
                } else {
                    Element mergedTestMethod = mergeTestMethods(output, input1TestMethod, input2TestMethod);
                    outputTestClass.appendChild(mergedTestMethod);
                }
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(output);
        StreamResult result = new StreamResult(new File(outputFile));
        transformer.transform(source, result);
    }
    private static Element mergeTestMethods(Document output, Node input1TestMethod, Node input2TestMethod) {
        Element mergedTestMethod = (Element) output.importNode(input2TestMethod, true);
        NodeList input1Methods = input1TestMethod.getChildNodes();
        NodeList mergedMethods = mergedTestMethod.getChildNodes();

        Map<String, List<Element>> input1NestedMethodsMap = new HashMap<>();
        buildNestedMethodsList(input1Methods, input1NestedMethodsMap, 0); // Include level parameter

        mergeChildMethods(mergedMethods, input1NestedMethodsMap, 0);

        // Update mergedTestMethod attributes
        mergedTestMethod.setAttribute("id", input1TestMethod.getAttributes().getNamedItem("id").getNodeValue());
        mergedTestMethod.setAttribute("name", input2TestMethod.getAttributes().getNamedItem("name").getNodeValue());
        mergedTestMethod.setAttribute("testType", input2TestMethod.getAttributes().getNamedItem("testType").getNodeValue());
        mergedTestMethod.setAttribute("throwException", input2TestMethod.getAttributes().getNamedItem("throwException").getNodeValue());
        mergedTestMethod.setAttribute("time", input2TestMethod.getAttributes().getNamedItem("time").getNodeValue());

        return mergedTestMethod;
    }


    private static void buildNestedMethodsList(NodeList nodes, Map<String, List<Element>> map, int level) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String key = level + "-" + element.getTagName();
                List<Element> elements = map.getOrDefault(key, new ArrayList<>());
                elements.add(element);
                map.put(key, elements);

                buildNestedMethodsList(node.getChildNodes(), map, level + 1);
            }
        }
    }
    private static boolean mergeChildMethods(NodeList mergedMethods, Map<String, List<Element>> input1NestedMethodsMap, int level) {
        boolean notMatchedFlag = false;
        for (int i = 0; i < mergedMethods.getLength(); i++) {
            Node mergedMethod = mergedMethods.item(i);
            if (mergedMethod.getNodeType() == Node.ELEMENT_NODE) {
                String key = level + "-" + mergedMethod.getNodeName();
                List<Element> input1Methods = input1NestedMethodsMap.getOrDefault(key, new ArrayList<>());
                boolean currentMethodMatched = false;
                if (!input1Methods.isEmpty()) {
                    Element input1Method = input1Methods.get(0);
                    if (mergedMethod.getAttributes().getNamedItem("name").getNodeValue().equals(input1Method.getAttribute("name"))) {
                        if(!notMatchedFlag)
                        {
                            mergedMethod.getAttributes().getNamedItem("time").setNodeValue(input1Method.getAttribute("time"));
                        }
                        input1Methods.remove(0);
                        currentMethodMatched = true;
                    }
                }

                boolean childNotMatched = mergeChildMethods(mergedMethod.getChildNodes(), input1NestedMethodsMap, level + 1);

                // If current method and its children didn't match, set the time attribute to an empty string
                if (!currentMethodMatched || childNotMatched) {
                    mergedMethod.getAttributes().getNamedItem("time").setNodeValue("");
                    notMatchedFlag = true;
                }
            }
        }
        return notMatchedFlag;
    }
}
