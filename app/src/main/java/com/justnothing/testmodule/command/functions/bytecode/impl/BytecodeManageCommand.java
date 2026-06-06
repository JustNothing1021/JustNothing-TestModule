package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDumpRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeBatchExportRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeListClassesRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.bytecode.util.SystemBytecodeExtractor;
import com.justnothing.testmodule.utils.io.IOManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BytecodeManageCommand extends AbstractBytecodeCommand<CommandRequest> {

    public BytecodeManageCommand() {
        super("bytecode manage", CommandRequest.class);
    }

    @Override
    protected BytecodeResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();
        ClassLoader classLoader = context.classLoader();

        if (request instanceof BytecodeDumpRequest req) {
            return handleDump(req, classLoader, context);
        } else if (request instanceof BytecodeBatchExportRequest req) {
            return handleBatchExport(req, classLoader, context);
        } else if (request instanceof BytecodeListClassesRequest req) {
            return handleListClasses(req, classLoader, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private BytecodeResult handleDump(BytecodeDumpRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String className = request.getClassName();
        String outputPath = request.getOutputPath();

        if (outputPath == null) {
            outputPath = "/sdcard/" + className.replace('.', '_') + ".class";
        }

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            byte[] bytecode = getClassBytecode(targetClass);

            if (bytecode == null) {
                return buildErrorResult("无法获取类字节码");
            }

            IOManager.writeFile(outputPath, bytecode);

            logger.info("字节码已导出到: " + outputPath);
            return buildSuccessResult("dump", className,
                    "字节码已导出到: " + outputPath + "\n大小: " + bytecode.length + " 字节");

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "导出字节码失败");
            return buildErrorResult(errorMsg);
        }
    }

    private BytecodeResult handleBatchExport(BytecodeBatchExportRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        String outputPath = request.getOutputPath();

        if (outputPath == null) {
            outputPath = "/sdcard/class_dump_" + System.currentTimeMillis() + "/";
        }

        try {
            SystemBytecodeExtractor.exportAllClasses(classLoader, outputPath);
            out(context, "批量导出完成，保存到: " + outputPath, Colors.LIGHT_GREEN);
            return buildSuccessResult("batch_export", null, "批量导出完成，保存到: " + outputPath);
        } catch (Exception e) {
            logger.error("批量导出失败", e);
            out(context, "错误: " + e.getMessage(), Colors.RED);
            return buildErrorResult("批量导出失败: " + e.getMessage());
        }
    }

    private BytecodeResult handleListClasses(BytecodeListClassesRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest> context) {
        try {
            Map<String, byte[]> classes = SystemBytecodeExtractor.getAllClassesBytecode(classLoader);

            StringBuilder sb = new StringBuilder();
            sb.append("类加载器: ").append(classLoader.getClass().getName()).append("\n");
            sb.append("已加载类数: ").append(classes.size()).append("\n\n");

            List<String> classNames = new ArrayList<>(classes.keySet());
            Collections.sort(classNames);

            int count = 0;
            for (String className : classNames) {
                sb.append(className).append("\n");
                count++;
                if (count >= 100) {
                    sb.append("... (总共 ").append(classes.size()).append(" 个类)\n");
                    break;
                }
            }

            out(context, sb.toString(), Colors.DEFAULT);
            return buildSuccessResult("list_classes", null, sb.toString());

        } catch (Exception e) {
            logger.error("获取类列表失败", e);
            out(context, "错误: " + e.getMessage(), Colors.RED);
            return buildErrorResult("获取类列表失败: " + e.getMessage());
        }
    }
}
