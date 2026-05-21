package com.se.bds.core.rtm;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RtmReportExtension implements TestWatcher, AfterAllCallback {

    private static final String RTM_PATH = findRtmPath();
    private static final String REPORT_DIR = "target/reports/";
    private static final Map<String, RtmParser.RtmInfo> rtmMap = RtmParser.parseRtm(RTM_PATH);

    private static String findRtmPath() {
        String[] paths = {
            "docs/REQUIREMENT_TRACEABILITY_MATRIX.csv",
            "../docs/REQUIREMENT_TRACEABILITY_MATRIX.csv"
        };
        for (String p : paths) {
            if (new File(p).exists()) return p;
        }
        return paths[0]; // fallback
    }
    private static final AtomicInteger defectCounter = new AtomicInteger(0);
    private static final List<TestResult> results = Collections.synchronizedList(new ArrayList<>());

    private static class TestResult {
        String tcId;
        String methodName;
        String className;
        String status;
        String defectId;
        String errorMessage;
        String stackTrace;
        long duration;
        LocalDateTime executedAt;

        RtmParser.RtmInfo rtmInfo;
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        recordResult(context, "Pass", null, null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String defectId = "BUG-" + String.format("%03d", defectCounter.incrementAndGet());
        recordResult(context, "Fail", defectId, cause);
    }

    private void recordResult(ExtensionContext context, String status, String defectId, Throwable cause) {
        TestResult res = new TestResult();
        res.tcId = context.getRequiredTestMethod().getAnnotation(RtmTestCase.class) != null 
                    ? context.getRequiredTestMethod().getAnnotation(RtmTestCase.class).id() 
                    : "UNKNOWN";
        res.methodName = context.getRequiredTestMethod().getName();
        res.className = context.getRequiredTestClass().getName();
        res.status = status;
        res.defectId = defectId;
        res.executedAt = LocalDateTime.now();
        res.duration = 0; // Simplified for this implementation
        res.rtmInfo = rtmMap.getOrDefault(res.tcId, new RtmParser.RtmInfo("N/A", "Unknown Feature", "No description"));

        if (cause != null) {
            res.errorMessage = cause.getMessage() != null ? cause.getMessage().split("\n")[0] : cause.getClass().getName();
            java.io.StringWriter sw = new java.io.StringWriter();
            cause.printStackTrace(new PrintWriter(sw));
            res.stackTrace = sw.toString();
        }

        results.add(res);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Generate reports when all tests in a class finish
        // Note: For a global suite report, a TestExecutionListener is preferred, 
        // but this Extension approach satisfies the requirement for an Extension.
        generateReports();
    }

    private synchronized void generateReports() {
        new File(REPORT_DIR).mkdirs();
        generateTestResultsCsv();
        generateDefectRegistryCsv();
        generateBugReportTxt();
    }

    private void generateTestResultsCsv() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(REPORT_DIR + "TEST_RESULTS_OUTPUT.csv"))) {
            pw.println("Category,Test Case ID,Test Case Description,PreRequisites,Steps to Perform,Step Expected Result,Test Case Expected Result,Actual Result,Status,Defect ID,Note");
            for (TestResult res : results) {
                String actualResult = res.status.equals("Pass") ? "Test passed successfully." : escapeCsv(res.errorMessage);
                String note = String.format("Executed: %s; Duration: %dms", res.executedAt.format(DateTimeFormatter.ISO_DATE_TIME), res.duration);
                pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%s%n",
                        escapeCsv(res.rtmInfo.category),
                        escapeCsv(res.tcId),
                        escapeCsv(res.rtmInfo.tcDesc),
                        "", "", "", "Test assertion should pass.",
                        actualResult,
                        res.status,
                        res.defectId != null ? res.defectId : "",
                        escapeCsv(note),
                        ",,,,,,,,,,,"); // Final 11 commas
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateDefectRegistryCsv() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(REPORT_DIR + "DEFECT_REGISTRY.csv"))) {
            pw.println("Defect ID,Test Case ID,Test Case Description,Category,Defect Status,Severity,Priority,Assigned To,Reported Date,Summary,Actual Result (Evidence),Test File Path,Duration (ms),,,,,,,,,,,,");
            for (TestResult res : results) {
                if (res.status.equals("Fail")) {
                    pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d%s%n",
                            res.defectId,
                            escapeCsv(res.tcId),
                            escapeCsv(res.rtmInfo.tcDesc),
                            escapeCsv(res.rtmInfo.category),
                            "Open", "Critical (S1)", "High (P1)", "Developer-Backend",
                            res.executedAt.toLocalDate().toString(),
                            escapeCsv(res.errorMessage),
                            escapeCsv(res.stackTrace),
                            escapeCsv(res.className),
                            res.duration,
                            ",,,,,,,,,,,,"); // Final 12 commas to match 25 total
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateBugReportTxt() {
        long totalDefects = results.stream().filter(r -> r.status.equals("Fail")).count();
        try (PrintWriter pw = new PrintWriter(new FileWriter(REPORT_DIR + "BUG_REPORT_GENERATED.txt"))) {
            pw.println("Project: BatDongScam Backend");
            pw.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pw.println("Total Defects Found: " + totalDefects);
            pw.println();
            pw.println("Quick Reference Table:");
            pw.println("Defect ID | Test Case ID | Status | Severity");
            pw.println("-------------------------------------------");
            for (TestResult res : results) {
                if (res.status.equals("Fail")) {
                    pw.printf("%-10s | %-12s | %-6s | Critical (S1)%n", res.defectId, res.tcId, "Open");
                }
            }
            pw.println();

            for (TestResult res : results) {
                if (res.status.equals("Fail")) {
                    pw.println("--------------------------------------------------");
                    pw.println("  Bug Name: [" + res.tcId + "]: " + res.rtmInfo.reqDesc);
                    pw.println("  Bug ID: [" + res.defectId + "]");
                    pw.println("  Test Case ID: [" + res.tcId + "]");
                    pw.println("  Date: [" + res.executedAt.toLocalDate() + "]");
                    pw.println("  Assigned to: Developer-Backend");
                    pw.println("  Status: Open");
                    pw.println("  Summary/Description:");
                    pw.println("    " + res.errorMessage);
                    pw.println("  Environments (OS/Browser): " + System.getProperty("java.version") + " / " + System.getProperty("os.name"));
                    pw.println("  Step to reproduce:");
                    pw.println("    1. Run test class: " + res.className);
                    pw.println("    2. Test method failed: " + res.methodName);
                    pw.println("  Actual results:");
                    pw.println("    " + res.stackTrace.replace("\n", "\n    "));
                    pw.println("  Expected results:");
                    pw.println("    Test assertion should pass.");
                    pw.println("  Severity: Critical (S1)");
                    pw.println("  Priority: High (P1)");
                    pw.println("--------------------------------------------------");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
