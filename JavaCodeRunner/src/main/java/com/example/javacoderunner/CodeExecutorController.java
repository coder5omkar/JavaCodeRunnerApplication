package com.example.javacoderunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private JavaClassGenerator classGenerator;


    @PostMapping("/run")
    public ResponseEntity<String> runJavaCode(@RequestBody CodeRequest request) {
        logger.info("Received code execution request");

        String result;
        try {
            // Generate Java code file
            String fullClassCode = classGenerator.generateFullClass(request.getCode());
            File sourceFile = new File(TEMP_FILE_NAME);  // Moved declaration before use
            Files.write(sourceFile.toPath(), fullClassCode.getBytes());

            logger.info("Generated code:\n{}", fullClassCode);

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

    // Removed injectVariables() as it wasn't being used in the main flow
    // Removed parseMethodSignature() as it wasn't being used in the main flow
    // Removed generateMethodCall() as it wasn't being used in the main flow

    private void cleanUpTempFiles() {
        try {
            File javaFile = new File(TEMP_FILE_NAME);
            File classFile = new File(TEMP_CLASS_NAME + ".class");

//            if (javaFile.exists()) {
//                boolean javaDeleted = javaFile.delete();
//                logger.debug("Temporary Java file deleted: {}", javaDeleted);
//            }
//
//            if (classFile.exists()) {
//                boolean classDeleted = classFile.delete();
//                logger.debug("Temporary class file deleted: {}", classDeleted);
//            }
        } catch (Exception e) {
            logger.warn("Failed to clean up temporary files", e);
        }
    }
}