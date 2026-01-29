package com.justnothing.testmodule.command.functions.bytecode;

import android.annotation.SuppressLint;

import com.justnothing.testmodule.utils.functions.Logger;

import dalvik.system.DexFile;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SystemBytecodeExtractor {
    private static final String TAG = "SystemBytecodeExtractor";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final Map<String, byte[]> BYTECODE_CACHE = new HashMap<>();
    
    /**
     * 增强版：获取类的字节码（支持系统类）
     */
    public static byte[] getEnhancedClassBytecode(Class<?> clazz) {
        if (clazz == null) return null;
        
        String className = clazz.getName();
        
        // 1. 检查缓存
        if (BYTECODE_CACHE.containsKey(className)) {
            return BYTECODE_CACHE.get(className);
        }
        
        // 2. 尝试标准方法（对于应用类）
        byte[] bytecode = getClassBytecodeStandard(clazz);
        if (bytecode != null) {
            BYTECODE_CACHE.put(className, bytecode);
            return bytecode;
        }
        
        // 3. 对于系统类，尝试从DexFile或Jar中获取
        bytecode = getSystemClassBytecode(clazz);
        if (bytecode != null) {
            BYTECODE_CACHE.put(className, bytecode);
        }
        
        return bytecode;
    }
    
    /**
     * 标准方法：通过ClassLoader获取字节码
     */
    private static byte[] getClassBytecodeStandard(Class<?> clazz) {
        try {
            String resourceName = clazz.getName().replace('.', '/') + ".class";
            ClassLoader classLoader = clazz.getClassLoader();
            
            if (classLoader == null) {
                return null; // 系统类，类加载器为null
            }
            
            InputStream is = classLoader.getResourceAsStream(resourceName);
            if (is != null) {
                return readAllBytes(is);
            }
        } catch (Exception e) {
            logger.warn("getClassBytecodeStandard出现错误", e);
        }
        return null;
    }
    
    /**
     * 系统类专用：从DexFile或Jar文件中获取字节码
     */
    private static byte[] getSystemClassBytecode(Class<?> clazz) {
        String className = clazz.getName();
        
        try {
            // 方法1：通过反射获取BootClassLoader的路径
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            byte[] bytecode = findClassInBootClassPath(className, bootClassLoader);
            if (bytecode != null) return bytecode;
            
            // 方法2：扫描/system/framework目录
            bytecode = findClassInSystemFramework(className);
            if (bytecode != null) return bytecode;
            
            // 方法3：使用DexFile API
            bytecode = findClassViaDexFile(className);
            return bytecode;
            
        } catch (Exception e) {
            logger.warn("getSystemClassByteCode出现错误", e);
        }
        return null;
    }
    
    /**
     * 在BootClassPath中查找类
     */
    private static byte[] findClassInBootClassPath(String className, ClassLoader classLoader) {
        try {
            // 获取PathClassLoader的DexPathList
            Field pathListField = ClassLoader.class.getDeclaredField("pathList");
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(classLoader);
            
            // 获取dexElements
            assert pathList != null;
            Field dexElementsField = pathList.getClass().getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            Object[] dexElements = (Object[]) dexElementsField.get(pathList);
            
            // 遍历所有dex元素
            assert dexElements != null;
            for (Object dexElement : dexElements) {
                try {
                    Field dexFileField = dexElement.getClass().getDeclaredField("dexFile");
                    dexFileField.setAccessible(true);
                    DexFile dexFile = (DexFile) dexFileField.get(dexElement);
                    
                    if (dexFile != null) {
                        // 尝试加载类
                        Class<?> loadedClass = dexFile.loadClass(className, classLoader);
                        if (loadedClass != null) {
                            // 通过反射获取mCookie，然后获取原始DEX数据
                            return getDexBytecode(dexFile, className);
                        }
                    }
                } catch (Exception e) {
                    // 继续尝试下一个
                }
            }
        } catch (Exception e) {
            logger.warn("findClassInBootClassPath出现错误", e);
        }
        return null;
    }
    
    /**
     * 在/system/framework目录中查找类
     */
    private static byte[] findClassInSystemFramework(String className) {
        File frameworkDir = new File("/system/framework/");
        if (!frameworkDir.exists()) return null;
        
        File[] jarFiles = frameworkDir.listFiles((dir, name) -> 
            name.endsWith(".jar") || name.endsWith(".apk"));
        
        if (jarFiles == null) return null;
        
        String classEntry = className.replace('.', '/') + ".class";
        
        for (File jarFile : jarFiles) {
            try {
                byte[] bytecode = extractClassFromJar(jarFile.getAbsolutePath(), classEntry);
                if (bytecode != null) return bytecode;
            } catch (Exception e) {
                // 继续尝试下一个文件
            }
        }
        return null;
    }
    
    /**
     * 从JAR文件中提取类
     */
    private static byte[] extractClassFromJar(String jarPath, String classEntry) {
        try {
            if (jarPath.endsWith(".jar")) {
                JarFile jarFile = new JarFile(jarPath);
                JarEntry entry = jarFile.getJarEntry(classEntry);
                if (entry != null) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        return readAllBytes(is);
                    }
                }
                jarFile.close();
            } else if (jarPath.endsWith(".apk")) {
                // APK文件实际上也是ZIP格式
                ZipFile zipFile = new ZipFile(jarPath);
                ZipEntry entry = zipFile.getEntry(classEntry);
                if (entry != null) {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        return readAllBytes(is);
                    }
                }
                zipFile.close();
            }
        } catch (Exception e) {
            logger.warn("extractClassFromJar出现错误", e);
        }
        return null;
    }
    
    /**
     * 使用DexFile API查找类
     */
    private static byte[] findClassViaDexFile(String className) {
        // 注意：这个方法可能需要系统权限
        try {
            // 加载系统jar文件
            String[] systemJars = {
                "/system/framework/framework.jar",
                "/system/framework/services.jar",
                "/system/framework/core.jar",
                "/system/framework/ext.jar",
                "/system/framework/telephony-common.jar"
            };
            
            for (String jarPath : systemJars) {
                try {
                    DexFile dexFile = new DexFile(jarPath);
                    Enumeration<String> entries = dexFile.entries();
                    
                    while (entries.hasMoreElements()) {
                        String entry = entries.nextElement();
                        if (entry.equals(className)) {
                            // 尝试获取类的DEX字节码
                            return getDexClassData(dexFile, className);
                        }
                    }
                    dexFile.close();
                } catch (Exception e) {
                    // 文件可能不存在或无法访问
                }
            }
        } catch (Exception e) {
            logger.warn("findClassViaDexFile出现错误", e);
        }
        return null;
    }
    
    /**
     * 获取DEX文件的原始数据
     */
    private static byte[] getDexBytecode(DexFile dexFile, String className) {
        try {
            // 通过反射获取mCookie
            @SuppressLint("DiscouragedPrivateApi") Field mCookieField = DexFile.class.getDeclaredField("mCookie");
            mCookieField.setAccessible(true);
            Object mCookie = mCookieField.get(dexFile);
            
            // 通过反射获取mFileName
            @SuppressLint("DiscouragedPrivateApi") Field mFileNameField = DexFile.class.getDeclaredField("mFileName");
            mFileNameField.setAccessible(true);
            String fileName = (String) mFileNameField.get(dexFile);
            
            if (fileName != null) {
                // 从文件中读取
                RandomAccessFile raf = new RandomAccessFile(fileName, "r");
                byte[] data = new byte[(int) raf.length()];
                raf.readFully(data);
                raf.close();
                return data;
            }
        } catch (Exception e) {
            logger.warn("getDexBytecode出现错误", e);
        }
        return null;
    }
    
    /**
     * 获取DEX类的数据
     */
    private static byte[] getDexClassData(DexFile dexFile, String className) {
        // 注意：这个方法比较复杂，因为DEX格式与JVM字节码不同
        // 这里我们尝试获取整个DEX文件
        return getDexBytecode(dexFile, className);
    }
    
    /**
     * 读取所有字节
     */
    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }
    
    /**
     * 获取当前ClassLoader加载的所有类的字节码
     */
    public static Map<String, byte[]> getAllClassesBytecode(ClassLoader classLoader) {
        Map<String, byte[]> result = new HashMap<>();
        
        try {
            // 获取类加载器的所有已加载类
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            assert classes != null;
            for (Class<?> clazz : classes) {
                try {
                    byte[] bytecode = getEnhancedClassBytecode(clazz);
                    if (bytecode != null) {
                        result.put(clazz.getName(), bytecode);
                    }
                } catch (Exception e) {
                    // 跳过无法处理的类
                }
            }
        } catch (Exception e) {
            logger.warn("getAllClassesBytecode出现错误", e);
        }
        
        return result;
    }
    
    /**
     * 批量导出所有类到文件
     */
    public static void exportAllClasses(ClassLoader classLoader, String outputDir) {
        Map<String, byte[]> classes = getAllClassesBytecode(classLoader);
        
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        int success = 0;
        int failed = 0;
        
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            String className = entry.getKey();
            byte[] bytecode = entry.getValue();
            
            if (bytecode == null || bytecode.length == 0) {
                failed++;
                continue;
            }
            
            try {
                // 创建安全的文件名
                String safeName = className
                    .replace('.', '_')
                    .replace('$', '_')
                    .replace('/', '_')
                    + ".class";
                
                File outputFile = new File(dir, safeName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(bytecode);
                    success++;
                }
                
                // 每100个文件记录一次
                if (success % 100 == 0) {
                    logger.info("已导出 " + success + " 个类文件");
                }
                
            } catch (Exception e) {
                failed++;
                logger.error("导出 " + className + " 失败: " + e.getMessage());
            }
        }
        
        logger.info("导出完成: 成功=" + success + ", 失败=" + failed);
        
        // 生成索引文件
        createIndexFile(classes, new File(dir, "index.txt"));
    }
    
    /**
     * 创建索引文件
     */
    private static void createIndexFile(Map<String, byte[]> classes, File indexFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(indexFile))) {
            writer.println("Class Index - Total: " + classes.size());
            writer.println("Generated: " + new Date());
            writer.println("========================================");
            
            List<String> classNames = new ArrayList<>(classes.keySet());
            Collections.sort(classNames);
            
            for (String className : classNames) {
                byte[] bytecode = classes.get(className);
                writer.println(String.format(
                    Locale.getDefault(),
                    "%-80s %10d bytes",
                    className, 
                    bytecode != null ? bytecode.length : 0));
            }
        } catch (Exception e) {
            logger.warn("createIndexFile出现错误", e);
        }
    }
}