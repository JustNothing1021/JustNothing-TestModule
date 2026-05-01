package com.justnothing.testmodule.command.functions.network;

import com.justnothing.testmodule.hooks.HookAPI;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;

public class NetworkInterceptor {

    private static final String TAG = "NetworkInterceptor";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static final String OKHTTP_CLIENT = "okhttp3.OkHttpClient";
    private static final String OKHTTP_CALL = "okhttp3.Call";
    private static final String OKHTTP_REQUEST = "okhttp3.Request";
    private static final String OKHTTP_RESPONSE = "okhttp3.Response";
    private static final String OKHTTP_REQUEST_BODY = "okhttp3.RequestBody";
    private static final String OKHTTP_RESPONSE_BODY = "okhttp3.ResponseBody";
    private static final String OKHTTP_NEW_CALL = "newCall";
    private static final String OKHTTP_CALLBACK = "okhttp3.Callback";

    private static final String RETROFIT_CALL = "retrofit2.Call";
    private static final String RETROFIT_RESPONSE = "retrofit2.Response";

    private static volatile boolean okHttpHooked = false;
    private static volatile boolean httpUrlHooked = false;
    private static volatile boolean retrofitHooked = false;

    private static ClassLoader cachedClassLoader;

    private NetworkInterceptor() {
    }

    public static boolean hookOkHttp(ClassLoader classLoader) {
        if (okHttpHooked) {
            logger.info("OkHttp 已经被 Hook");
            return true;
        }

        cachedClassLoader = classLoader;

        try {
            Class<?> okHttpClientClass = ClassResolver.findClass(OKHTTP_CLIENT, classLoader);
            if (okHttpClientClass == null) {
                logger.info("未找到 OkHttp 类，跳过 Hook");
                return false;
            }

            Class<?> requestClass = ClassResolver.findClass(OKHTTP_REQUEST, classLoader);
            if (requestClass == null) {
                logger.warn("未找到 Request 类");
                return false;
            }

            XC_MethodHook.Unhook unhook = HookAPI.findAndHookMethod(
                    okHttpClientClass,
                    OKHTTP_NEW_CALL,
                    requestClass,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!NetworkManager.getInstance().isInterceptEnabled()) {
                                return;
                            }

                            try {
                                Object request = param.args[0];
                                NetworkRequestInfo requestInfo = extractOkHttpRequest(request);

                                NetworkManager.MockRule mockRule = NetworkManager.getInstance()
                                        .findMockRule(requestInfo.getUrl());

                                if (mockRule != null) {
                                    logger.info("Mock 匹配: " + requestInfo.getUrl());
                                    requestInfo.setResponseCode(mockRule.statusCode);
                                    requestInfo.setResponseBody(mockRule.response);
                                    for (Map.Entry<String, String> header : mockRule.headers.entrySet()) {
                                        requestInfo.addResponseHeader(header.getKey(), header.getValue());
                                    }
                                    requestInfo.setResponseTime(System.currentTimeMillis());
                                    requestInfo.setCompleted(true);

                                    Object mockResponse = createMockResponse(request, mockRule, classLoader);
                                    Object mockCall = createMockCall(mockResponse, request, classLoader);
                                    param.setResult(mockCall);
                                }
                            } catch (Exception e) {
                                logger.error("OkHttp Hook 处理失败", e);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!NetworkManager.getInstance().isRecordEnabled()) {
                                return;
                            }

                            try {
                                Object call = param.getResult();
                                if (call != null && !isMockCall(call)) {
                                    hookCallMethods(call, classLoader);
                                }
                            } catch (Exception e) {
                                logger.error("OkHttp Call Hook 处理失败", e);
                            }
                        }
                    }
            );

            NetworkManager.getInstance().addHook("okhttp_new_call", unhook);
            okHttpHooked = true;
            logger.info("OkHttp Hook 成功");
            return true;

        } catch (Throwable e) {
            logger.error("OkHttp Hook 失败", e);
            return false;
        }
    }

    private static boolean isMockCall(Object call) {
        try {
            return call.getClass().getSimpleName().contains("Mock");
        } catch (Exception e) {
            return false;
        }
    }

    private static void hookCallMethods(Object call, ClassLoader classLoader) {
        try {
            Class<?> callClass = call.getClass();

            HookAPI.findAndHookMethod(callClass, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Exception {
                    Object thisObject = param.thisObject;
                    Object request = ReflectionUtils.callMethod(thisObject, "request");

                    NetworkRequestInfo requestInfo = extractOkHttpRequest(request);
                    param.setObjectExtra("network_request", requestInfo);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    NetworkRequestInfo requestInfo = (NetworkRequestInfo) param.getObjectExtra("network_request");
                    if (requestInfo == null) return;

                    Object response = param.getResult();
                    if (response != null) {
                        extractOkHttpResponse(response, requestInfo);
                    }

                    Throwable error = param.getThrowable();
                    if (error != null) {
                        NetworkManager.getInstance().failRequest(requestInfo.getId(), error);
                    } else {
                        NetworkManager.getInstance().completeRequest(requestInfo.getId());
                    }
                }
            });

            Class<?> callbackClass = ClassResolver.findClass(OKHTTP_CALLBACK, classLoader);
            if (callbackClass != null) {
                HookAPI.findAndHookMethod(callClass, "enqueue", callbackClass, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Exception {
                        Object thisObject = param.thisObject;
                        Object request = ReflectionUtils.callMethod(thisObject, "request");
                        Object originalCallback = param.args[0];

                        NetworkRequestInfo requestInfo = extractOkHttpRequest(request);

                        Object wrappedCallback = createWrappedCallback(originalCallback, requestInfo, classLoader);
                        param.args[0] = wrappedCallback;
                    }
                });
            }

        } catch (Exception e) {
            logger.error("Hook Call 方法失败", e);
        }
    }

    private static Object createWrappedCallback(Object originalCallback, NetworkRequestInfo requestInfo, ClassLoader cl) {
        try {
            Class<?> callbackClass = ClassResolver.findClass(OKHTTP_CALLBACK, cl);

            return Proxy.newProxyInstance(
                    cl,
                    new Class<?>[]{callbackClass},
                    (proxy, method, args) -> {
                        String methodName = method.getName();

                        if (methodName.equals("onResponse")) {
                            Object response = args[1];

                            extractOkHttpResponse(response, requestInfo);
                            NetworkManager.getInstance().completeRequest(requestInfo.getId());

                            return method.invoke(originalCallback, args);
                        } else if (methodName.equals("onFailure")) {
                            Throwable error = (Throwable) args[1];

                            NetworkManager.getInstance().failRequest(requestInfo.getId(), error);

                            return method.invoke(originalCallback, args);
                        }

                        return method.invoke(originalCallback, args);
                    }
            );
        } catch (Exception e) {
            logger.error("创建包装回调失败", e);
            return originalCallback;
        }
    }

    private static NetworkRequestInfo extractOkHttpRequest(Object request) {
        return extractOkHttpRequest(request, "OkHttp");
    }

    private static NetworkRequestInfo extractOkHttpRequest(Object request, String clientType) {
        try {
            Object url = ReflectionUtils.callMethod(request, "url");
            String urlStr = (String) ReflectionUtils.callMethod(url, "toString");

            String method = (String) ReflectionUtils.callMethod(request, "method");

            NetworkRequestInfo info = NetworkManager.getInstance()
                    .createRequest(urlStr, method, clientType);

            Object headers = ReflectionUtils.callMethod(request, "headers");
            if (headers != null) {
                try {
                    Method namesMethod = headers.getClass().getMethod("names");
                    Iterable<?> names = (Iterable<?>) namesMethod.invoke(headers);
                    for (Object name : names) {
                        String nameStr = name.toString();
                        String value = (String) ReflectionUtils.callMethod(headers, "get", nameStr);
                        info.addHeader(nameStr, value);
                    }
                } catch (Exception ignored) {
                }
            }

            Object body = ReflectionUtils.callMethod(request, "body");
            if (body != null) {
                try {
                    Class<?> bufferClass = ClassResolver.findClass("okio.Buffer", cachedClassLoader);
                    Object buffer = bufferClass.newInstance();
                    ReflectionUtils.callMethod(body, "writeTo", buffer);
                    String bodyStr = buffer.toString();
                    info.setRequestBody(bodyStr);
                } catch (Exception e) {
                    logger.debug("无法读取请求体: " + e.getMessage());
                }
            }

            return info;

        } catch (Exception e) {
            logger.error("提取 OkHttp 请求信息失败", e);
            return NetworkManager.getInstance().createRequest("unknown", "UNKNOWN", clientType);
        }
    }

    private static void extractOkHttpResponse(Object response, NetworkRequestInfo requestInfo) {
        try {
            int code = (int) ReflectionUtils.callMethod(response, "code");
            String message = (String) ReflectionUtils.callMethod(response, "message");

            requestInfo.setResponseCode(code);
            requestInfo.setResponseMessage(message);

            Object headers = ReflectionUtils.callMethod(response, "headers");
            if (headers != null) {
                try {
                    Method namesMethod = headers.getClass().getMethod("names");
                    Iterable<?> names = (Iterable<?>) namesMethod.invoke(headers);
                    for (Object name : names) {
                        String nameStr = name.toString();
                        String value = (String) ReflectionUtils.callMethod(headers, "get", nameStr);
                        requestInfo.addResponseHeader(nameStr, value);
                    }
                } catch (Exception ignored) {
                }
            }

            Object body = ReflectionUtils.callMethod(response, "body");
            if (body != null) {
                try {
                    Object source = ReflectionUtils.callMethod(body, "source");
                    ReflectionUtils.callMethod(source, "request", Long.MAX_VALUE);
                    Object buffer = ReflectionUtils.callMethod(source, "buffer");
                    String bodyStr = ReflectionUtils.callMethod(buffer, "clone").toString();

                    if (bodyStr == null || bodyStr.isEmpty()) {
                        bodyStr = buffer.toString();
                    }
                    requestInfo.setResponseBody(bodyStr);
                } catch (Exception e) {
                    logger.debug("无法读取响应体: " + e.getMessage());
                }
            }

            requestInfo.setResponseTime(System.currentTimeMillis());

        } catch (Exception e) {
            logger.error("提取 OkHttp 响应信息失败", e);
        }
    }

    private static Object createMockResponse(Object request, NetworkManager.MockRule rule, ClassLoader cl) {
        try {
            Class<?> responseBuilderClass = ClassResolver.findClass("okhttp3.Response$Builder", cl);
            Object builder = responseBuilderClass.newInstance();

            ReflectionUtils.callMethod(builder, "request", request);
            ReflectionUtils.callMethod(builder, "protocol",
                    ClassResolver.findClass("okhttp3.Protocol", cl).getField("HTTP_1_1").get(null));
            ReflectionUtils.callMethod(builder, "code", rule.statusCode);
            ReflectionUtils.callMethod(builder, "message", "Mocked");

            Object responseBody = createResponseBody(rule.response, cl);
            ReflectionUtils.callMethod(builder, "body", responseBody);

            return ReflectionUtils.callMethod(builder, "build");

        } catch (Exception e) {
            logger.error("创建 Mock 响应失败", e);
            return null;
        }
    }

    private static Object createRequestBody(String content, String contentType, ClassLoader cl) {
        try {
            Class<?> mediaTypeClass = ClassResolver.findClass("okhttp3.MediaType", cl);
            Object mediaType = ReflectionUtils.callStaticMethod(mediaTypeClass, "parse", contentType);

            Class<?> requestBodyClass = ClassResolver.findClass(OKHTTP_REQUEST_BODY, cl);

            try {
                return ReflectionUtils.callStaticMethod(requestBodyClass, "create", mediaType, content);
            } catch (Exception e1) {
                return ReflectionUtils.callStaticMethod(requestBodyClass, "create", content, mediaType);
            }

        } catch (Exception e) {
            logger.error("创建请求体失败", e);
            return null;
        }
    }

    private static Object createResponseBody(String content, ClassLoader cl) {
        try {
            Class<?> mediaTypeClass = ClassResolver.findClass("okhttp3.MediaType", cl);
            Object mediaType = ReflectionUtils.callStaticMethod(mediaTypeClass, "parse", "application/json; charset=utf-8");

            Class<?> responseBodyClass = ClassResolver.findClass(OKHTTP_RESPONSE_BODY, cl);

            try {
                return ReflectionUtils.callStaticMethod(responseBodyClass, "create", mediaType, content);
            } catch (Exception e1) {
                try {
                    return ReflectionUtils.callStaticMethod(responseBodyClass, "create", content, mediaType);
                } catch (Exception e2) {
                    return ReflectionUtils.callStaticMethod(responseBodyClass, "create",
                            ClassResolver.findClassOrFail("okio.ByteString", cl)
                                    .getMethod("encodeUtf8", String.class).invoke(null, content),
                            mediaType);
                }
            }

        } catch (Exception e) {
            logger.error("创建响应体失败", e);
            return null;
        }
    }

    private static Object createMockCall(Object response, Object request, ClassLoader cl) {
        try {
            Class<?> callClass = ClassResolver.findClass(OKHTTP_CALL, cl);
            Class<?> responseClass = ClassResolver.findClass(OKHTTP_RESPONSE, cl);

            return Proxy.newProxyInstance(
                    cl,
                    new Class<?>[]{callClass},
                    (proxy, method, args) -> {
                        String methodName = method.getName();

                        switch (methodName) {
                            case "execute" -> {
                                return response;
                            }
                            case "enqueue" -> {
                                Object callback = args[0];
                                try {
                                    Method onResponse = callback.getClass().getMethod("onResponse", callClass, responseClass);
                                    onResponse.invoke(callback, proxy, response);
                                } catch (Exception e) {
                                    Method onFailure = callback.getClass().getMethod("onFailure", callClass, IOException.class);
                                    onFailure.invoke(callback, proxy, new IOException("Mock call failed", e));
                                }
                                return null;
                            }
                            case "isExecuted" -> {
                                return true;
                            }
                            case "isCanceled" -> {
                                return false;
                            }
                            case "equals" -> {
                                return Objects.equals(proxy, args[0]);
                            }
                            case "request" -> {
                                return request;
                            }
                            case "hashCode" -> {
                                return System.identityHashCode(proxy);
                            }
                            case "toString" -> {
                                return "MockCall";
                            }
                            default -> {
                            }
                        }
                        return null;
                    }
            );
        } catch (Exception e) {
            logger.error("创建 Mock Call 失败", e);
            return null;
        }
    }

    public static boolean hookHttpURLConnection(ClassLoader classLoader) {
        if (httpUrlHooked) {
            logger.info("HttpURLConnection 已经被 Hook");
            return true;
        }

        try {
            HookAPI.findAndHookMethod(URL.class, "openConnection", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object connection = param.getResult();
                    if (connection instanceof HttpURLConnection) {
                        hookHttpUrlConnectionInstance((HttpURLConnection) connection);
                    }
                }
            });

            httpUrlHooked = true;
            logger.info("HttpURLConnection Hook 成功");
            return true;

        } catch (Throwable e) {
            logger.error("HttpURLConnection Hook 失败", e);
            return false;
        }
    }

    private static void hookHttpUrlConnectionInstance(HttpURLConnection connection) {
        try {
            HookAPI.findAndHookMethod(connection.getClass(), "getInputStream", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!NetworkManager.getInstance().isRecordEnabled()) {
                        return;
                    }

                    try {
                        HttpURLConnection thisConn = (HttpURLConnection) param.thisObject;
                        URL url = thisConn.getURL();
                        String method = thisConn.getRequestMethod();

                        NetworkRequestInfo info = NetworkManager.getInstance()
                                .createRequest(url.toString(), method, "HttpURLConnection");

                        param.setObjectExtra("network_request", info);

                    } catch (Exception e) {
                        logger.error("记录 HttpURLConnection 请求失败", e);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    NetworkRequestInfo info = (NetworkRequestInfo) param.getObjectExtra("network_request");
                    if (info == null) return;

                    try {
                        HttpURLConnection thisConn = (HttpURLConnection) param.thisObject;
                        int responseCode = thisConn.getResponseCode();
                        String responseMessage = thisConn.getResponseMessage();

                        info.setResponseCode(responseCode);
                        info.setResponseMessage(responseMessage);
                        info.setResponseTime(System.currentTimeMillis());

                        InputStream is = (InputStream) param.getResult();
                        if (is != null) {
                            StringBuilder sb = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line);
                            }
                            info.setResponseBody(sb.toString());
                        }

                        NetworkManager.getInstance().completeRequest(info.getId());

                    } catch (Exception e) {
                        logger.error("记录 HttpURLConnection 响应失败", e);
                        NetworkManager.getInstance().failRequest(info.getId(), e);
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Hook HttpURLConnection 实例失败", e);
        }
    }

    public static boolean hookRetrofit(ClassLoader classLoader) {
        if (retrofitHooked) {
            logger.info("Retrofit 已经被 Hook");
            return true;
        }

        try {
            Class<?> retrofitCallClass = ClassResolver.findClass(RETROFIT_CALL, classLoader);
            if (retrofitCallClass == null) {
                logger.info("未找到 Retrofit 类，跳过 Hook");
                return false;
            }

            HookAPI.findAndHookMethod(retrofitCallClass, "execute", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!NetworkManager.getInstance().isRecordEnabled()) {
                        return;
                    }

                    try {
                        Object retrofitCall = param.thisObject;
                        Object okHttpCall = ReflectionUtils.callMethod(retrofitCall, "raw");
                        Object request = ReflectionUtils.callMethod(okHttpCall, "request");

                        NetworkRequestInfo info = extractOkHttpRequest(request, "Retrofit");

                        param.setObjectExtra("network_request", info);

                    } catch (Exception e) {
                        logger.error("记录 Retrofit 请求失败", e);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    NetworkRequestInfo info = (NetworkRequestInfo) param.getObjectExtra("network_request");
                    if (info == null) return;

                    try {
                        Object retrofitResponse = param.getResult();
                        if (retrofitResponse != null) {
                            int code = (int) ReflectionUtils.callMethod(retrofitResponse, "code");
                            String message = (String) ReflectionUtils.callMethod(retrofitResponse, "message");

                            info.setResponseCode(code);
                            info.setResponseMessage(message);

                            Object body = ReflectionUtils.callMethod(retrofitResponse, "body");
                            if (body != null) {
                                info.setResponseBody(body.toString());
                            }

                            Object errorBody = ReflectionUtils.callMethod(retrofitResponse, "errorBody");
                            if (errorBody != null) {
                                String errorStr = (String) ReflectionUtils.callMethod(errorBody, "string");
                                if (info.getResponseBody() == null || info.getResponseBody().isEmpty()) {
                                    info.setResponseBody(errorStr);
                                }
                            }
                        }

                        info.setResponseTime(System.currentTimeMillis());
                        NetworkManager.getInstance().completeRequest(info.getId());

                    } catch (Exception e) {
                        logger.error("记录 Retrofit 响应失败", e);
                        NetworkManager.getInstance().failRequest(info.getId(), e);
                    }
                }
            });

            retrofitHooked = true;
            logger.info("Retrofit Hook 成功");
            return true;

        } catch (Throwable e) {
            logger.error("Retrofit Hook 失败", e);
            return false;
        }
    }

    public static boolean isOkHttpHooked() {
        return okHttpHooked;
    }

    public static boolean isHttpUrlHooked() {
        return httpUrlHooked;
    }

    public static boolean isRetrofitHooked() {
        return retrofitHooked;
    }

    public static void unhookAll() {
        NetworkManager.getInstance().clearHooks();
        okHttpHooked = false;
        httpUrlHooked = false;
        retrofitHooked = false;
        logger.info("所有网络 Hook 已移除");
    }
}
