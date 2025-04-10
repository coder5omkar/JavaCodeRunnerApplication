package com.example.javacoderunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CodeExecutorController {

    private static final Logger logger = LoggerFactory.getLogger(CodeExecutorController.class);
    private static final String TEMP_CLASS_NAME = "TempCode";
    private static final String TEMP_FILE_NAME = TEMP_CLASS_NAME + ".java";
    private static final int MAX_PARAMS = 4;

    @PostMapping("/run")
    public ResponseEntity<String> runJavaCode(@RequestBody CodeRequest request) {
        logger.info("Received code execution request");

        String result;
        try {
            // Generate Java code file
            String codeWithInputs = injectVariables(request.getCode(), request.getVars());
            File sourceFile = new File(TEMP_FILE_NAME);

            logger.debug("Generated code with variables injected:\n{}", codeWithInputs);

            Files.write(sourceFile.toPath(), codeWithInputs.getBytes());
            logger.info("Temporary source file created: {}", sourceFile.getAbsolutePath());

            // Compile
            logger.info("Compiling temporary Java file...");
            Process compileProcess = Runtime.getRuntime().exec("javac " + TEMP_FILE_NAME);
            compileProcess.waitFor();

            if (compileProcess.exitValue() != 0) {
                String errors = new String(compileProcess.getErrorStream().readAllBytes());
                logger.error("Compilation failed with errors:\n{}", errors);
                return ResponseEntity.ok(errors);
            }

            logger.info("Compilation successful. Executing code...");

            // Run
            Process runProcess = Runtime.getRuntime().exec("java " + TEMP_CLASS_NAME);
            runProcess.waitFor();

            String output = new String(runProcess.getInputStream().readAllBytes());
            String errors = new String(runProcess.getErrorStream().readAllBytes());

            if (!errors.isEmpty()) {
                logger.warn("Runtime errors encountered:\n{}", errors);
            }

            result = output + errors;
            logger.info("Execution completed. Result length: {}", result.length());

        } catch (Exception e) {
            logger.error("Exception during code execution", e);
            result = "Error: " + e.getMessage();
        } finally {
            cleanUpTempFiles();
        }
        return ResponseEntity.ok(result);
    }

    private String injectVariables(String code, List<String> vars) {
        logger.debug("Injecting {} variables into code", vars.size());

        // If input already contains a complete class with main method, use it as-is
        if (code.contains("public class") && code.contains("main(String[] args)")) {
            logger.debug("Input contains complete class definition, using as-is");
            return code;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(TEMP_CLASS_NAME).append(" {\n");
        sb.append("    public static void main(String[] args) {\n");

        // Parse method signature to get parameters
        MethodInfo methodInfo = parseMethodSignature(code);

        // Generate method call with parameters
        if (methodInfo != null) {
            String methodCall = generateMethodCall(methodInfo, vars);
            sb.append("        ").append(methodCall).append("\n");
        }

        sb.append("    }\n\n"); // Close main method

        // Add the provided code
        sb.append(code).append("\n");
        sb.append("}\n");

        return sb.toString();
    }

    private MethodInfo parseMethodSignature(String code) {
        // Extract method signature using regex
        if (code.matches("(?s).*public\\s+(?:static\\s+)?[\\w<>]+\\s+(\\w+)\\(([^)]*)\\).*")) {
            String methodName = code.replaceAll("(?s).*public\\s+(?:static\\s+)?[\\w<>]+\\s+(\\w+)\\([^)]*\\).*", "$1");
            String paramsStr = code.replaceAll("(?s).*public\\s+(?:static\\s+)?[\\w<>]+\\s+\\w+\\(([^)]*)\\).*", "$1");

            // Count parameters
            int paramCount = paramsStr.trim().isEmpty() ? 0 : paramsStr.split("\\s*,\\s*").length;

            return new MethodInfo(
                    methodName,
                    paramCount,
                    code.contains("public static")
            );
        }
        return null;
    }

    private String generateMethodCall(MethodInfo methodInfo, List<String> vars) {
        StringBuilder call = new StringBuilder();

        if (methodInfo.isStatic) {
            call.append(TEMP_CLASS_NAME).append(".");
        } else {
            call.append("new ").append(TEMP_CLASS_NAME).append("().");
        }

        call.append(methodInfo.name).append("(");

        // Add parameters based on method signature and available variables
        for (int i = 0; i < Math.min(methodInfo.paramCount, MAX_PARAMS); i++) {
            if (i < vars.size() && !vars.get(i).isEmpty()) {
                call.append("\"").append(vars.get(i)).append("\"");
            } else {
                call.append("\"\""); // Default empty string if no input provided
            }

            if (i < methodInfo.paramCount - 1) {
                call.append(", ");
            }
        }

        call.append(");");
        return call.toString();
    }

    private void cleanUpTempFiles() {
        try {
            File javaFile = new File(TEMP_FILE_NAME);
            File classFile = new File(TEMP_CLASS_NAME + ".class");

            if (javaFile.exists()) {
                boolean javaDeleted = javaFile.delete();
                logger.debug("Temporary Java file deleted: {}", javaDeleted);
            }

            if (classFile.exists()) {
                boolean classDeleted = classFile.delete();
                logger.debug("Temporary class file deleted: {}", classDeleted);
            }
        } catch (Exception e) {
            logger.warn("Failed to clean up temporary files", e);
        }
    }

    private static class MethodInfo {
        String name;
        int paramCount;
        boolean isStatic;

        MethodInfo(String name, int paramCount, boolean isStatic) {
            this.name = name;
            this.paramCount = paramCount;
            this.isStatic = isStatic;
        }
    }
}