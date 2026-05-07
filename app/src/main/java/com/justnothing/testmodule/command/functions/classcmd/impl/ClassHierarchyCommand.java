package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.DirectCommand;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassHierarchyRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassHierarchyResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ClassHierarchyCommand extends DirectCommand<ClassHierarchyRequest, ClassHierarchyResult> {

    private static final Logger logger = Logger.getLoggerForName("ClassHierarchyCommand");

    public ClassHierarchyCommand() {
        super("hierarchy",
            ClassHierarchyRequest.class,
            ClassHierarchyResult.class,
                ClassHierarchyCommand::executeHierarchy);
    }

    private static ClassHierarchyResult executeHierarchy(ClassCommandContext<ClassHierarchyRequest> context,
                                                         ClassHierarchyRequest request) {
        String className = request.getClassName();
        logger.debug("查询类层次结构: " + className);

        ClassHierarchyResult result = new ClassHierarchyResult();

        if (className == null || className.isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_REQUEST", "类名不能为空"));
            result.setSuccess(false);
            return result;
        }

        try {
            Class<?> clazz = ClassResolver.findClassOrFail(className);

            List<ClassHierarchyResult.HierarchyClassInfo> classChain = new ArrayList<>();
            List<List<String>> interfacesPerLevel = new ArrayList<>();

            while (clazz != null) {
                ClassHierarchyResult.HierarchyClassInfo info =
                    new ClassHierarchyResult.HierarchyClassInfo(
                        clazz.getName(),
                        clazz.isInterface(),
                        clazz.isAnnotation(),
                        clazz.isEnum(),
                        Modifier.isAbstract(clazz.getModifiers()),
                        Modifier.isFinal(clazz.getModifiers())
                    );
                classChain.add(info);

                List<String> interfaces = new ArrayList<>();
                Collections.addAll(interfaces, getClassNames(clazz.getInterfaces()));
                interfacesPerLevel.add(interfaces);

                clazz = clazz.getSuperclass();
            }

            Collections.reverse(classChain);
            Collections.reverse(interfacesPerLevel);

            result.setClassChain(classChain);
            result.setInterfacesPerLevel(interfacesPerLevel);
            result.setSuccess(true);

            context.execContext().println("=== 类层次结构 ===", Colors.CYAN);
            for (int i = 0; i < classChain.size(); i++) {
                ClassHierarchyResult.HierarchyClassInfo info = classChain.get(i);
                String prefix = "  ".repeat(i);
                context.execContext().print(prefix + "├─ ", Colors.CYAN);
                context.execContext().println(info.getName(), Colors.GREEN);

                List<String> ifaces = interfacesPerLevel.get(i);
                if (!ifaces.isEmpty()) {
                    for (String iface : ifaces) {
                        context.execContext().print(prefix + "│  └─ implements ", Colors.GRAY);
                        context.execContext().println(iface, Colors.YELLOW);
                    }
                }
            }

            logger.info("类层次结构查询成功: " + className);

        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            result.setError(new CommandResult.ErrorInfo("CLASS_NOT_FOUND", "类未找到: " + className));
            result.setSuccess(false);
        } catch (Exception e) {
            logger.error("处理类层次结构失败: " + className, e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "处理失败: " + e.getMessage()));
            result.setSuccess(false);
        }

        return result;
    }

    private static String[] getClassNames(Class<?>[] classes) {
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }
}
