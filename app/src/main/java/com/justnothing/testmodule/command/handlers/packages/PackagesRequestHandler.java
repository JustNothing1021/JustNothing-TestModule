package com.justnothing.testmodule.command.handlers.packages;

import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.packages.PackagesRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.packages.PackagesResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;

import org.json.JSONObject;

import java.util.List;

public class PackagesRequestHandler implements RequestHandler<PackagesRequest, PackagesResult> {

    private static final Logger logger = Logger.getLoggerForName("PackagesReqHandler");

    @Override
    public String getCommandType() {
        return "Packages";
    }

    @Override
    public PackagesRequest parseRequest(JSONObject obj) {
        return new PackagesRequest().fromJson(obj);
    }

    @Override
    public PackagesResult createResult(String requestId) {
        return new PackagesResult(requestId);
    }

    @Override
    public PackagesResult handle(PackagesRequest request) {
        logger.debug("处理包列表请求");

        PackagesResult result = new PackagesResult(request.getRequestId());

        try {
            List<String> packages = ClassLoaderManager.getAllKnownPackages();
            result.setPackages(packages);
            logger.info("包列表查询成功, 共 " + packages.size() + " 个包");
        } catch (Exception e) {
            logger.error("获取包列表失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "获取包列表失败: " + e.getMessage()));
        }

        return result;
    }
}
