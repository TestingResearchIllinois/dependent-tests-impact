package edu.washington.cs.dt.impact.util;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "testClass")
public class TestClass {
    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String name;
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testMethod")
    private ArrayList<Method> method;
    private static List<TestClass> allTestClasses = new ArrayList<>();

    public TestClass(String name) {
        this.name = name;
        this.method = new ArrayList<Method>();
        allTestClasses.add(this);
    }

    public void addMethod(Method m) {
        this.method.add(m);
    }

    public ArrayList<Method> getMethod() {
        return method;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public static List<TestClass> getAllTestClasses() {
        return allTestClasses;
    }
    public TestClass getTestClassByName(String name) {
        for (TestClass testClass : allTestClasses) {
            if (testClass.getName().equals(name)) {
                return testClass;
            }
        }
        return null;
    }
}
