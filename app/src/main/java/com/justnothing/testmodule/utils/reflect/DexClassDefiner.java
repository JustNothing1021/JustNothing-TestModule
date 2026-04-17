package com.justnothing.testmodule.utils.reflect;

import com.justnothing.javainterpreter.evaluator.ClassDefiner;
import com.justnothing.testmodule.constants.FileDirectory;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DexClassDefiner implements ClassDefiner {

    private static final AtomicLong COUNTER = new AtomicLong(0);
    private static final ConcurrentHashMap<String, ClassLoader> LOADER_CACHE = new ConcurrentHashMap<>();

    @Override
    public Class<?> defineClass(String name, byte[] bytecode, ClassLoader parent) throws Exception {
        byte[] dexBytes = classToDex(bytecode);

        ClassLoader effectiveParent = resolveEffectiveParent(parent);

        String loaderKey = "dcg_" + COUNTER.getAndIncrement();
        File dexFile = writeDexToTempFile(dexBytes, loaderKey);

        try {
            ClassLoader dexClassLoader = createDexClassLoader(dexFile, effectiveParent);
            LOADER_CACHE.put(loaderKey, dexClassLoader);

            return dexClassLoader.loadClass(name);
        } catch (Exception e) {
            cleanupDexFile(dexFile);
            throw e;
        }
    }

    private static ClassLoader resolveEffectiveParent(ClassLoader parent) {
        if (parent != null) {
            try {
                parent.loadClass("com.justnothing.javainterpreter.evaluator.MethodBodyExecutor");
                return parent;
            } catch (ClassNotFoundException ignored) {}
        }

        ClassLoader fallback = DexClassDefiner.class.getClassLoader();
        try {
            fallback.loadClass("com.justnothing.javainterpreter.evaluator.MethodBodyExecutor");
            return fallback;
        } catch (ClassNotFoundException ignored) {}

        return parent != null ? parent : ClassLoader.getSystemClassLoader();
    }

    private static ClassLoader createDexClassLoader(File dexFile, ClassLoader parent) throws Exception {
        File optimizedDir = createOptimizedDir("dcg_opt");

        String dexPath = dexFile.getAbsolutePath();
        String optimizedDirPath = optimizedDir.getAbsolutePath();

        return (ClassLoader) Class
                .forName("dalvik.system.DexClassLoader")
                .getConstructor(String.class, String.class, String.class, ClassLoader.class)
                .newInstance(dexPath, optimizedDirPath, null, parent);
    }

    private static File writeDexToTempFile(byte[] dexBytes, String prefix) throws IOException {
        File tempDir = createTempDir("dcg_dex");
        File dexFile = new File(tempDir, prefix + ".dex");
        try (FileOutputStream fos = new FileOutputStream(dexFile)) {
            fos.write(dexBytes);
        }
        dexFile.deleteOnExit();
        return dexFile;
    }

    private static void cleanupDexFile(File dexFile) {
        if (dexFile != null && dexFile.exists()) {
            dexFile.delete();
        }
    }

    private static File createOptimizedDir(String prefix) throws IOException {
        String[] candidateDirs = {
            FileDirectory.METHODS_DATA_DIR + "/opt",
            FileDirectory.SDCARD_PATH + "/dcg_opt",
            System.getProperty("java.io.tmpdir"),
            "/data/data/com.justnothing.testmodule/cache",
            "/data/local/tmp"
        };

        for (String dir : candidateDirs) {
            if (dir == null || dir.isEmpty()) continue;
            File f = new File(dir, prefix + "_" + System.nanoTime());
            if (!f.exists()) {
                try {
                    if (f.mkdirs() || f.isDirectory()) {
                        f.deleteOnExit();
                        return f;
                    }
                } catch (SecurityException ignored) {}
            }
        }

        File fallback = File.createTempFile(prefix, "");
        fallback.delete();
        fallback.mkdirs();
        fallback.deleteOnExit();
        return fallback;
    }

    private static File createTempDir(String prefix) throws IOException {
        String[] candidateDirs = {
            FileDirectory.METHODS_DATA_DIR,
            FileDirectory.SDCARD_PATH + "/dcg_temp",
            System.getProperty("java.io.tmpdir"),
            "/data/data/com.justnothing.testmodule/cache",
            "/data/local/tmp"
        };

        for (String dir : candidateDirs) {
            if (dir == null || dir.isEmpty()) continue;
            File f = new File(dir, prefix + "_" + System.nanoTime());
            if (!f.exists()) {
                try {
                    if (f.mkdirs() || f.isDirectory()) {
                        f.deleteOnExit();
                        return f;
                    }
                } catch (SecurityException ignored) {}
            }
        }

        File fallback = File.createTempFile(prefix, "");
        fallback.delete();
        fallback.mkdirs();
        fallback.deleteOnExit();
        return fallback;
    }

    private static byte[] classToDex(byte[] classBytes) throws Exception {
        File tempDir = createTempDir("dcg_dex");

        File classFile = new File(tempDir, "GeneratedClass" + System.nanoTime() + ".class");
        try (FileOutputStream fos = new FileOutputStream(classFile)) {
            fos.write(classBytes);
        }

        try {
            D8Command.Builder builder = D8Command.builder();
            builder.addProgramFiles(classFile.toPath());
            builder.setMinApiLevel(26);
            builder.setOutput(tempDir.toPath(), OutputMode.DexIndexed);
            
            D8.run(builder.build());

            File dexFile = new File(tempDir, "classes.dex");
            if (!dexFile.exists()) {
                File[] files = tempDir.listFiles((dir, fname) -> fname.endsWith(".dex"));
                if (files != null && files.length > 0) { 
                    dexFile = files[0]; 
                }
            }

            if (dexFile.exists()) {
                return Files.readAllBytes(dexFile.toPath());
            }

            throw new RuntimeException("D8 did not produce a .dex output file");
        } finally {
            if (classFile.exists()) classFile.delete();
            File[] dexFiles = tempDir.listFiles((dir, fname) -> fname.endsWith(".dex"));
            if (dexFiles != null) { 
                for (File f : dexFiles) f.delete(); 
            }
            tempDir.delete();
        }
    }
}
