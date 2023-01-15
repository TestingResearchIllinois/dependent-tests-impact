/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 * 
 * Tool used to instrument class and test files.
 */

package edu.washington.cs.dt.impact.util;

import edu.washington.cs.dt.impact.util.Constants.TECHNIQUE;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.*;
import soot.tagkit.AnnotationTag;
import soot.tagkit.LineNumberTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Instrumenter extends BodyTransformer{

    private static SootClass tracerClass;
    private static SootMethod trace, output, reset, selectionOutput, selectionTrace, timerOutput, startTimer, endTimer, startExecution, endExecution, exceptionMessage;
    private static final String JUNIT4_TAG = "VisibilityAnnotationTag";
    private static final String JUNIT4_TYPE = "Lorg/junit/Test;";
    private static final String JUNIT4_BEFORE = "Lorg/junit/Before;";
    private static final String JUNIT4_BEFORE_CLASS = "Lorg/junit/BeforeClass;";
    private static final String JUNIT4_AFTER = "Lorg/junit/After;";
    private static final String JUNIT4_AFTER_CLASS = "Lorg/junit/AfterClass;";
    private static final String JUNIT3_CLASS = "junit.framework.TestCase";
    private static final String JUNIT3_RETURN = "void";
    private static final String JUNIT3_METHOD_PREFIX = "test";
    private static TECHNIQUE technique = Constants.DEFAULT_TECHNIQUE;

    public Instrumenter() {
	Scene.v().setSootClassPath(System.getProperty("java.class.path"));

        tracerClass = Scene.v().loadClassAndSupport("edu.washington.cs.dt.impact.util.Tracer");
        trace = tracerClass.getMethodByName("trace");
        selectionTrace = tracerClass.getMethodByName("selectionTrace");
        selectionOutput = tracerClass.getMethodByName("selectionOutput");
        output = tracerClass.getMethodByName("output");
        reset = tracerClass.getMethodByName("reset");
        timerOutput = tracerClass.getMethodByName("timerOutput");
        startExecution = tracerClass.getMethodByName("startExecution");
        endExecution = tracerClass.getMethodByName("endExecution");
        startTimer = tracerClass.getMethodByName("startTimer");
        endTimer = tracerClass.getMethodByName("endTimer");
        exceptionMessage = tracerClass.getMethodByName("exceptionMessage");
    }

    public Instrumenter(TECHNIQUE t) {
        this();
        technique = t;
    }

    /* internalTransform goes through a method body and inserts
     * counter instructions before an INVOKESTATIC instruction
     */
    @Override
    protected void internalTransform(Body body, String phase,
            @SuppressWarnings("rawtypes") Map options) {
        SootMethod method = body.getMethod();

        if (method.getName().equals("<init>") || method.getName().equals("<clinit>")) {
            return;
        }

        boolean isSetupOrTeardown = false;
        boolean isBefore = false;
        boolean isAfter = false;
        boolean isBeforeClass = false;
        boolean isAfterClass = false;

        boolean isJUnit4 = false;
        VisibilityAnnotationTag vat = (VisibilityAnnotationTag) method.getTag(JUNIT4_TAG);
        if (vat != null) {
            List<AnnotationTag> tags = vat.getAnnotations();
            for (AnnotationTag at : tags) {
                if (!isJUnit4) {
                    isJUnit4 = at.getType().equals(JUNIT4_TYPE);
                }
                if (!isSetupOrTeardown) {
                    isBefore = at.getType().equals(JUNIT4_BEFORE);
                    isAfter = at.getType().equals(JUNIT4_AFTER);
                    isSetupOrTeardown = isBefore || isAfter;
                }
                if (!isBeforeClass) {
                    isBeforeClass = at.getType().equals(JUNIT4_BEFORE_CLASS);
                }
                if (!isAfterClass) {
                    isAfterClass  = at.getType().equals(JUNIT4_AFTER_CLASS);
                }
            }
        }

        boolean isJUnit3 = false;
        boolean extendsJUnit = false;
        if (!isJUnit4 && method.getName().length() > 3) {
            String retType = method.getReturnType().toString();
            String prefix = method.getName().substring(0, 4);
            SootClass superClass = method.getDeclaringClass().getSuperclass();
            while(superClass.hasSuperclass()) {
                if (superClass.getName().equals(JUNIT3_CLASS)) {
                    extendsJUnit = true;
                    break;
                }
                superClass = superClass.getSuperclass();
            }
            isJUnit3 = method.isPublic() && retType.equals(JUNIT3_RETURN)
                    && extendsJUnit && prefix.equals(JUNIT3_METHOD_PREFIX);
        }

        // exclude the instrumentation of setups/teardowns
        // and JUnit3 methods that are not tests
        if ((extendsJUnit && !isJUnit3)) {
            return;
        }

        Chain<Unit> units = body.getUnits();
        // get a snapshot iterator of the unit since we are going to
        // mutate the chain when iterating over it.
        Iterator<Unit> stmtIt = units.snapshotIterator();

        Iterator<Unit> stmtItpre = units.snapshotIterator();


        String packageMethodName = method.getDeclaringClass().getName() + "." + method.getName();
        SootClass thrwCls = Scene.v().getSootClass("java.lang.Throwable");
        List<Stmt> probe = new ArrayList<Stmt>();
        PatchingChain<Unit> pchain = body.getUnits();

        Stmt sFirstNonId = getFirstNonIdStmt(pchain);
        Stmt sLast = (Stmt) pchain.getLast();
        Stmt sFirst = (Stmt) pchain.getFirst();
        StringBuffer testOutputBuffer = new StringBuffer();
        String part = "body";
        if (isBefore) {
            part = "before";
        } else if (isAfter){
            part = "after";
        }

        if (isJUnit4 || isJUnit3 || isSetupOrTeardown) {
            // instrumentation of JUnit files

            // get access to Throwable class and toString method
            InvokeExpr firstExpr = Jimple.v().newStaticInvokeExpr(startExecution.makeRef(),StringConstant.v(packageMethodName), StringConstant.v(part));
            Stmt firstStmt = Jimple.v().newInvokeStmt(firstExpr);
            units.insertAfter(firstStmt, sFirst);



            InvokeExpr lastExpr = Jimple.v().newStaticInvokeExpr(endExecution.makeRef(),StringConstant.v(packageMethodName), StringConstant.v(part), StringConstant.v(" "));
            Stmt lastStmt = Jimple.v().newInvokeStmt(lastExpr);
            units.insertBefore(lastStmt, sLast);
            // Don't instrument empty methods
            if (sFirstNonId == sLast) {
                return;
            }

            // FOR NOW, no other returns allowed apart from last stmt
            for (Unit u : pchain) {
                assert (!(u instanceof ReturnStmt) && !(u instanceof RetStmt)) || u == sLast;
            }

            Stmt sGotoLast = Jimple.v().newGotoStmt(sLast);
            probe.add(sGotoLast);
            Local lException1 = getCreateLocal(body, "<ex1>", RefType.v(thrwCls));
            Stmt sCatch = Jimple.v().newIdentityStmt(lException1, Jimple.v().newCaughtExceptionRef());
            probe.add(sCatch);

            // TODO after catching an exception in a test, throw the exception back
            // Type throwType = thrwCls.getType();
            // Local lSysOut = getCreateLocal(body, "<throw>", throwType);
            // Stmt callThrow = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(lException1, initThrow.makeRef(),
            //         lSysOut));
            // probe.add(callThrow);

            insertRightBeforeNoRedirect(pchain, probe, sLast);

            // insert trap (catch)
            body.getTraps().add(Jimple.v().newTrap(thrwCls, sFirstNonId, sGotoLast, sCatch));

            // Do not forget to insert instructions to report the counter

        } else {
            if (technique == TECHNIQUE.SELECTION) {
                // instrumentation of class files for test selection
                // creates selectionOutput directory containing what statements
                // each test executed without duplicates
                Set<Unit> duplicates = new HashSet<Unit>();
                StringBuffer sb = new StringBuffer();
                UnitGraph ug = new ExceptionalUnitGraph(body);
                Stack<Unit> remaining = new Stack<Unit>();
                StringBuffer functionBodyBuffer = new StringBuffer();
                remaining.addAll(ug.getHeads());

                List<Stmt> incStmts = new LinkedList<Stmt>();
                List<Stmt> stmts = new LinkedList<Stmt>();

                while(!remaining.empty()) {
                    Unit current = remaining.pop();
                    // cast back to a statement.
                    Stmt stmt = (Stmt)current;

                    if (stmt instanceof ReturnVoidStmt) {
                        continue;
                    }
                    if (internalOrInitStatementNotInvoked(stmt)) {
                        functionBodyBuffer.append(stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName() + "." + stmt.getInvokeExpr().getMethod().getName() + "\n");
                    }
                    if (!duplicates.contains(current)) {
                        List<Unit> children = ug.getSuccsOf(current);
                        // don't check identity statements (parameters)
                        if (!(stmt instanceof IdentityStmt)) {

                            for (Unit u : children) {
                                String s = packageMethodName + " : "
                                        + current.toString().split(" goto")[0] + " >>>>>>>> "
                                        + packageMethodName + " : "
                                        + u.toString().split(" goto")[0] + "\n";
                                sb.append(s);
                            }

                            InvokeExpr incExpr= Jimple.v().newStaticInvokeExpr(
                                    selectionTrace.makeRef(), StringConstant.v(stmt.toString()),
                                    StringConstant.v(packageMethodName));
                            Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
                            incStmts.add(incStmt);
                            stmts.add(stmt);
                        }
                        remaining.addAll(children);
                        duplicates.add(current);
                    }
                }

                for (int i = 0; i < incStmts.size(); i++) {
                    units.insertBefore(incStmts.get(i), stmts.get(i));
                }

                selectionOutput(packageMethodName, sb, "selectionOutput");
                selectionOutput(packageMethodName, functionBodyBuffer, "methodOutput");
            } else {
                // instrumentation of class files for test prioritization and parallelization
                Set<Integer> lines = new HashSet<Integer>();
                StringBuffer functionBodyBuffer = new StringBuffer();
                while (stmtIt.hasNext()) {
                    // cast back to a statement.
                    Stmt stmt = (Stmt)stmtIt.next();

                    if (stmt instanceof ReturnVoidStmt) {
                        continue;
                    }

                    if (stmt instanceof IdentityStmt) {
                        continue;
                    }
                    if (internalOrInitStatementNotInvoked(stmt)) {
                        functionBodyBuffer.append(stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName() + "." + stmt.getInvokeExpr().getMethod().getName() + "\n");
                    }
                    LineNumberTag t = (LineNumberTag) stmt.getTag("LineNumberTag");
                    if (stmt.hasTag("LineNumberTag") && !lines.contains(t.getLineNumber())) {
                        lines.add(t.getLineNumber());

                        InvokeExpr incExpr= Jimple.v().newStaticInvokeExpr(trace.makeRef(),
                                StringConstant.v(stmt.toString()),
                                StringConstant.v(packageMethodName));
                        Stmt incStmt = Jimple.v().newInvokeStmt(incExpr);
                        units.insertBefore(incStmt, stmt);
                    }
                }
                selectionOutput(packageMethodName, functionBodyBuffer, "methodOutput");
            }
        }
        //stmtIt = units.snapshotIterator();
        //stmtIt=stmtItpre;

        while (stmtItpre.hasNext()) {
            Stmt stmt = (Stmt)stmtItpre.next();

            if (internalOrInitStatementNotInvoked(stmt)) {
                testOutputBuffer.append(stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName() + "." + stmt.getInvokeExpr().getMethod().getName() + "\n");

                InvokeExpr resetExpr = Jimple.v().newStaticInvokeExpr(timerOutput.makeRef(),
                        StringConstant.v(packageMethodName) ,
                        StringConstant.v(stmt.getInvokeExpr().getMethod().getName()),
                        StringConstant.v(stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName()));


                Stmt resetStmt = Jimple.v().newInvokeStmt(resetExpr);

                units.insertBefore(resetStmt, stmt);
            }


            if ((stmt instanceof ReturnStmt)
                    ||(stmt instanceof ReturnVoidStmt)) {


                if (technique != TECHNIQUE.SELECTION) {

                    InvokeExpr reportExpr= Jimple.v().newStaticInvokeExpr(output.makeRef(),
                            StringConstant.v(packageMethodName));
                    Stmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
                    units.insertBefore(reportStmt, stmt);

                    InvokeExpr resetExpr= Jimple.v().newStaticInvokeExpr(reset.makeRef());
                    Stmt resetStmt = Jimple.v().newInvokeStmt(resetExpr);
                    units.insertAfter(resetStmt, reportStmt);
                } else {

                    InvokeExpr reportExpr= Jimple.v().newStaticInvokeExpr(
                            selectionOutput.makeRef(), StringConstant.v(packageMethodName));
                    Stmt reportStmt = Jimple.v().newInvokeStmt(reportExpr);
                    units.insertBefore(reportStmt, stmt);

                    // reset the tracer
                    InvokeExpr resetExpr= Jimple.v().newStaticInvokeExpr(reset.makeRef());
                    Stmt resetStmt = Jimple.v().newInvokeStmt(resetExpr);
                    units.insertAfter(resetStmt, reportStmt);
                }
            }

        }


        Chain<Unit> new_units = body.getUnits();
        Iterator<Unit> new_stmtIt = new_units.snapshotIterator();
        while (new_stmtIt.hasNext()) {
            Stmt stmt = (Stmt) new_stmtIt.next();
            InvokeExpr lExceptionMessage = Jimple.v().newStaticInvokeExpr(exceptionMessage.makeRef(), StringConstant.v(packageMethodName), StringConstant.v(part));
            Stmt exceptionStmt = Jimple.v().newInvokeStmt(lExceptionMessage);
            if(stmt.toString().contains("@caughtexception")){
                units.insertAfter(exceptionStmt, stmt);
            }
        }
        selectionOutput(packageMethodName, testOutputBuffer, "testOutput");
    }

    private static boolean internalOrInitStatementNotInvoked(Stmt stmt) {
        return stmt.containsInvokeExpr() /*&& !(stmt.toString().contains("org.junit"))*/ &&
//                !(stmt.getInvokeExpr().getMethod().getName().equals("<init>")) &&
//                !(stmt.getInvokeExpr().getMethod().getName().equals("<clinit>")) &&
                !(stmt.getInvokeExpr().getMethodRef().getDeclaringClass().getName().contains("edu.washington.cs.dt.impact.util.Tracer"));
    }

    // used for the instrumentation of test selection class files
    private static void selectionOutput(String packageMethodName, StringBuffer sb, String fileName) {
        File theDir = new File(fileName);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            try {
                Files.createDirectory(theDir.toPath());
            } catch (FileAlreadyExistsException ignored) {
                // The directory must have been created in between the check above and our attempt to create it.
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            output = new FileWriter(fileName + File.separator + packageMethodName);
            writer = new BufferedWriter(output);
            writer.write(sb.toString());
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
    }

    private void insertRightBeforeNoRedirect(PatchingChain<Unit> pchain,
            List<Stmt> instrumCode, Stmt s) {
        assert !(s instanceof IdentityStmt);
        for (Object stmt : instrumCode) {
            pchain.insertBeforeNoRedirect((Unit) stmt, s);
        }
    }

    private static Stmt getFirstNonIdStmt(PatchingChain<Unit> pchain) {
        Stmt sFirstNonId = null;
        for (Iterator<Unit> it = pchain.iterator(); it.hasNext(); ) {
            sFirstNonId = (Stmt) it.next();
            if (!(sFirstNonId instanceof IdentityStmt)) {
                break;
            }
        }
        return sFirstNonId;
    }

    private static Local getCreateLocal(Body b, String localName, Type t) {
        // try getting it
        Local l = getLocal(b, localName);
        if (l != null) {
            assert l.getType().equals(t); // ensure type is correct
            return l;
        }
        // no luck; create it
        Chain<Local> locals = b.getLocals();
        l = Jimple.v().newLocal(localName, t);
        locals.add(l);
        return l;
    }

    private static Local getLocal(Body b, String localName) {
        // look for existing bs local, and return it if found
        Chain<Local> locals = b.getLocals();
        for (Iterator<Local> itLoc = locals.iterator(); itLoc.hasNext(); ) {
            Local l = itLoc.next();
            if (l.getName().equals(localName)) {
                return l;
            }
        }
        return null;
    }
}
