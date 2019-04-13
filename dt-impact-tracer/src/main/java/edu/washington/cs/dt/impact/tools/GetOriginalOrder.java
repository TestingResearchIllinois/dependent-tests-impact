package edu.washington.cs.dt.impact.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.washington.cs.dt.impact.tools.mavenTestOrder.GetMavenTestOrder;
import edu.washington.cs.dt.impact.tools.mavenTestOrder.TestClassData;

/**
 * The following is created using edu.illinois.cs.dt.tools.detection.getOriginalOrder
 * from the version https://github.com/idflakies/iDFlakies/commit/b74d016aafcce12e22bc88ee4e3903ee0b01f2b4
 * Created by winglam on 4/12/19.
 */
public class GetOriginalOrder {

    public static void main(String[] args) {
        Path outputFileName = Paths.get(args[0]);
        Path inputTargetDir = Paths.get(args[1]);
        Path mvnTestLog = Paths.get(args[2]);

        if (!Files.exists(outputFileName)) {
            try {
                final Path surefireReportsPath = inputTargetDir.resolve("surefire-reports");

                if (Files.exists(mvnTestLog) && Files.exists(surefireReportsPath)) {
                    final List<TestClassData> testClassData = new GetMavenTestOrder(surefireReportsPath, mvnTestLog).testClassDataList();

                    final List<String> tests = new ArrayList<>();

                    for (final TestClassData classData : testClassData) {
                        for (final String testName : classData.testNames) {
                            tests.add(classData.className + "." + testName);
                        }
                    }

                    FileTools.printListToFile(tests, outputFileName.toFile());
                } else {
                    throw new RuntimeException("Missing mvn test log or surefire-reports path.");
                }
            } catch (Exception ignored) {
                throw new RuntimeException("Error parsing surefire-reports.", ignored);
            }
        } else {
            System.out.println("Original order already exists. Skipping generation of original order.");
        }
    }
}
