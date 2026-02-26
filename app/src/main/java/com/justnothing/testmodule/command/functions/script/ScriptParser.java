package com.justnothing.testmodule.command.functions.script;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


import com.justnothing.testmodule.command.functions.script.ScriptModels.*;
import com.justnothing.testmodule.command.functions.script.ASTNodes.*;

import static com.justnothing.testmodule.command.functions.script.ScriptUtils.*;
import static com.justnothing.testmodule.command.functions.script.ScriptLogger.*;

/**
 * 代码解析器。
 */
public class ScriptParser {

    private final String input;
    private int position;
    private final ExecutionContext context;
    private final Stack<Integer> savedPositions;
    private ASTNode lastUncompleted = null;

    public ScriptParser(String input, ExecutionContext context) {
        this.input = input.trim();
        this.position = 0;
        this.savedPositions = new Stack<>();
        this.context = context;
    }

    /**
     * 查看当前字符但不移动位置。
     *
     * @return 当前字符，或者如果到达末尾则返回'\0'
     */
    private char peek() {
        return position < input.length() ? input.charAt(position) : '\0';
    }

    /**
     * 保存当前位置，以便以后恢复。
     *
     * @return 保存的位置
     */
    private int savePosition() {
        savedPositions.push(position);
        return position;
    }

    /**
     * 恢复到上次保存的位置。
     *
     * @return 返回到的位置
     */
    private int restorePosition() {
        position = savedPositions.pop();
        return position;
    }

    /**
     * 释放上一次保存的位置，不返回。
     *
     * @return 上一次保存的位置
     */
    private int releasePosition() {
        return savedPositions.pop();
    }

    /**
     * 查看当前字符但不移动位置。
     *
     * @param size 查看多少个字符
     * @return 当前字符，或者如果到达末尾则返回'\0'
     */
    private String peek(int size) {
        return input.substring(position, Math.min(input.length(), position + size));
    }

    /**
     * 预读下一个单词（标识符）。
     * 跳过空白字符后，读取连续的字母数字下划线字符。
     *
     * @return 下一个单词，如果到达末尾则返回空字符串
     */
    private String peekWord() {
        int savedPosition = position;
        int start;
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
        start = position;
        while (position < input.length() && (Character.isJavaIdentifierPart(input.charAt(position)))) {
            position++;
        }
        String word = position > start ? input.substring(start, position) : "";
        position = savedPosition;
        return word;
    }

    private String peekWord(int length) {
        if (length <= 0)
            return "";
        int savedPosition = position;
        int start;
        while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
            position++;
        }
        start = position;
        int end = Math.min(position + length, input.length());
        while (position < end && Character.isJavaIdentifierPart(input.charAt(position))) {
            position++;
        }
        String word = position > start ? input.substring(start, position) : "";
        position = savedPosition;
        return word;
    }

    /**
     * 跳过空白字符和注释。
     *
     */
    private void advanceToNextMeaningful() {
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) {
                position++;
            } else if (c == '/' && position + 1 < input.length()) {
                char next = input.charAt(position + 1);
                if (next == '/') {
                    while (position < input.length() && input.charAt(position) != '\n'
                            && input.charAt(position) != '\r') {
                        position++;
                    }
                } else if (next == '*') {
                    position += 2;
                    while (position + 1 < input.length()
                            && !(input.charAt(position) == '*' && input.charAt(position + 1) == '/')) {
                        position++;
                    }
                    if (position + 1 < input.length()) {
                        position += 2;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * 移动到下一个字符。
     *
     * @return 下一个字符，或者如果到达末尾则返回'\0'
     */
    private char advance() {
        return isAtEnd() ? '\0' : input.charAt(position++);
    }

    /**
     * 移动指定数量的字符。
     *
     * @param count 移动的字符数量
     */
    private String advance(int count) {
        StringBuilder sb = new StringBuilder();
        while (count-- > 0 && !isAtEnd()) {
            sb.append(advance());
        }
        return sb.toString();
    }

    /**
     * 匹配当前字符是否为预期字符，如果是则移动位置并返回true。否则返回false。
     *
     * @param expected 预期字符
     * @return 如果匹配则返回true，否则返回false
     */
    private boolean match(char expected) {
        if (peek() == expected) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchExact(char expected) {
        return matchExact(expected, "");
    }

    private boolean matchExact(char expected, String whiteList) {
        savePosition();
        if (!match(expected)) {
            restorePosition();
            return false;
        }
        if (!Character.isWhitespace(peek()) && !isAtEnd() && !whiteList.contains(peek() + "")) {
            restorePosition();
            return false;
        }
        releasePosition();
        return true;
    }

    private boolean matchKeywordExact(String expected) {
        return matchKeywordExact(expected, "");
    }

    /**
     * 通过全字匹配关键字。
     *
     * @param expected 关键字
     * @return 结果
     */
    private boolean matchKeywordExact(String expected, String whiteList) {
        savePosition();
        if (!matchWord(expected)) {
            restorePosition();
            return false;
        }
        if (!Character.isWhitespace(peek()) && !isAtEnd() && !whiteList.contains(peek() + "")) {
            restorePosition();
            return false;
        }
        releasePosition();
        return true;
    }

    /**
     * 判断接下来的字符是不是指定的字符。
     *
     * @param expected 关键字
     * @return 结果
     */
    private boolean isTargetWord(String expected) {
        return input.substring(position).startsWith(expected);
    }

    /**
     * 判断接下来的字符是不是指定的关键字。
     * 当关键字后面跟着一个非标识符字符时，才被认为是关键字。
     * （不然double会被识别成do，别问我怎么知道的，大草）
     *
     * @param expected 关键字
     * @return 结果
     */
    private boolean isTargetKeyWord(String expected) {
        return input.substring(position).startsWith(expected) &&
                (position + expected.length() >= input.length()
                        || (!Character.isJavaIdentifierPart(input.charAt(position + expected.length()))));
    }

    // Class<?> c = System.class;
    // java.lang.reflect.Field[] fields = c.getDeclaredFields();
    // System.out.println("Fields: " + fields.length);
    // for (java.lang.reflect.Field f : fields) if(f.getName().contains("out"))
    // System.out.println(f.getName());

    /**
     * 判断接下来的字符是不是指定的关键字，如果是，则返回true并且向前移动。
     *
     * @param expected 关键字
     * @return 结果
     */
    private boolean matchWord(String expected) {
        if (input.substring(position).startsWith(expected)) {
            position += expected.length();
            return true;
        }
        return false;
    }

    /**
     * 跳过接下来的所有空白字符，直到遇到不是空白的字符。
     */
    private void skipWhitespace() {
        while (position < input.length()) {
            char ch = peek();
            if (Character.isWhitespace(ch)) {
                advance();
            } else if (isTargetWord("//")) {
                skipLineComment();
            } else if (isTargetWord("/*")) {
                skipBlockComment();
            } else {
                break;
            }
        }
    }

    private void skipLineComment() {
        if (!input.substring(position).startsWith("//")) {
            throw new RuntimeException("Expected '//' at position " + position);
        }
        position += 2;
        while (position < input.length() && peek() != '\n' && peek() != '\r')
            advance();
    }

    private void skipBlockComment() {
        if (!input.substring(position).startsWith("/*")) {
            throw new RuntimeException("Expected '/*' at position " + position);
        }
        position += 2;
        while (position < input.length() && !isTargetWord("*/"))
            advance();
        if (position >= input.length()) {
            logger.error("多行注释未闭合");
            throw new RuntimeException("Unterminated multi-line comment");
        }
        if (!input.substring(position).startsWith("*/")) {
            throw new RuntimeException("Expected '*/' at position " + position);
        }
        position += 2;
    }

    /**
     * 检查是否到达输入的末尾。
     *
     * @return 如果到达末尾则返回true，否则返回false
     */
    private boolean isAtEnd() {
        return position >= input.length();
    }

    /**
     * 断定预期遇到指定字符，否则抛出异常。
     *
     * @param expected 预期字符
     */
    private char expect(char expected) throws RuntimeException {
        skipWhitespace();
        if (peek() != expected) {
            throw new RuntimeException("Expected '" + expected + "' but found '" + peek() + "'");
        }
        return peek();
    }

    private String expectWord(String expected) throws RuntimeException {
        skipWhitespace();
        if (input.substring(position).startsWith(expected)) {
            return expected;
        }
        throw new RuntimeException(
                "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
    }

    /**
     * 断定预期遇到指定字符并移动到下一个字符，否则抛出异常。
     *
     * @param expected 预期字符
     * @return 遇到的实际字符
     * @throws RuntimeException 如果失配
     */
    private char expectToMove(char expected) throws RuntimeException {
        skipWhitespace();
        if (peek() != expected) {
            throw new RuntimeException(
                    "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
        }
        return advance();
    }

    private String expectWordToMove(String expected) throws RuntimeException {
        skipWhitespace();
        if (input.substring(position).startsWith(expected)) {
            position += expected.length();
            return expected;
        }
        throw new RuntimeException(
                "Expected '" + expected + "' at position " + position + " but found '" + peek() + "'");
    }

    /**
     * 断定预期遇到指定字符，否则抛出异常。
     *
     * @param expected 预期字符
     */
    private char expect(char... expected) throws RuntimeException {
        skipWhitespace();
        for (char c : expected)
            if (peek() == c)
                return peek();
        throw new RuntimeException("Expected one of '" + String.valueOf(expected) +
                "' at position " + position + " but found '" + peek() + "'");
    }

    /**
     * 断定预期遇到指定字符并移动到下一个字符，否则抛出异常。
     *
     * @param expected 预期字符
     * @return 遇到的实际字符
     * @throws RuntimeException 如果失配
     */
    private char expectToMove(char... expected) throws RuntimeException {
        skipWhitespace();
        for (char c : expected)
            if (peek() == c) {
                advance();
                return c;
            }
        throw new RuntimeException("Expected one of '" + String.valueOf(expected) +
                "' at position " + position + " but found '" + peek() + "'");
    }

    /**
     * 设置最后一个没有被完成的语句。
     *
     * @param node 语句节点
     */
    private void setLastUncompletedStmt(ASTNode node) {
        this.lastUncompleted = node;
    }

    /**
     * 获取最后一个没有被完成的语句。
     *
     * @return 语句节点
     */
    private ASTNode getLastUncompletedStmt() {
        return this.lastUncompleted;
    }

    /**
     * 清除最后一个没有被完成的语句。
     * 注意了，如果一个语句已经被完成了，一定要调用这个。
     */
    private void clearLastUncompletedStmt() {
        this.lastUncompleted = null;
    }

    private void unexpectedToken() throws RuntimeException {
        throw new RuntimeException("Unexpected token '" + peek() + "' at position " + position);
    }

    private void unexpectedToken(String hint) throws RuntimeException {
        throw new RuntimeException(hint + "Unexpected token '" + peek() + "' at position " + position);
    }

    /**
     * 解析一个字符字面量。
     *
     * @return 解析到的字符
     */
    private ASTNode parseChar() throws RuntimeException {
        skipWhitespace();
        expectToMove('\'');
        char c = advance();
        expectToMove('\'');
        return new LiteralNode(c, Character.class);
    }

    private String parseStringCharWithEscape() throws RuntimeException {
        if (peek() == '\\') {
            advance();
            char escaped = advance();
            if (Character.isDigit(escaped)) {
                // 八进制转义序列
                String format = "" + escaped + advance() + advance();
                try {
                    int octalValue = Integer.parseInt(format, 8);
                    return String.valueOf((char) octalValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid octal escape sequence \\" + format);
                }
            } else if (escaped == 'x') {
                // 十六进制转义序列
                String format = "" + advance() + advance();
                try {
                    int hexValue = Integer.parseInt(format, 16);
                    return String.valueOf((char) hexValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid hexadecimal escape sequence \\x" + format);
                }
            } else if (Character.toLowerCase(escaped) == 'u') {
                // Unicode转义序列
                String format = "" + advance() + advance() + advance() + advance();
                try {
                    int unicodeValue = Integer.parseInt(format, 16);
                    return String.valueOf((char) unicodeValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid Unicode escape sequence \\u" + format);
                }
            } else {
                return String.valueOf(
                        switch (escaped) {
                            case 'b' -> '\b';
                            case 't' -> '\t';
                            case 'n' -> '\n';
                            case 'f' -> '\f';
                            case 'r' -> '\r';
                            case '"' -> '"';
                            case '\'' -> '\'';
                            case '\\' -> '\\';
                            default -> {
                                logger.warn("未知的转义序列: \\" + escaped);
                                context.printWarn("Invalid escape sequence \\" + escaped + "\n");
                                yield "" + '\\' + escaped;
                            }
                        });
            }
        } else {
            return String.valueOf(advance());
        }
    }

    private ASTNode parseString() throws RuntimeException {
        skipWhitespace();
        expectToMove('"');
        StringBuilder sb = new StringBuilder();
        while (!match('"')) {
            sb.append(parseStringCharWithEscape());
            if (peek() == '\0' || peek() == '\n' || peek() == '\r') {
                throw new RuntimeException("Unterminated string literal");
            }
        }
        return new LiteralNode(sb.toString(), String.class);
    }

    private ASTNode parseNumber() throws RuntimeException {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(peek()) ||
                peek() == '.' ||
                (peek() == '+' && sb.length() == 0) ||
                (peek() == '-' && sb.length() == 0) || peek() == 'o' || peek() == 'O' ||
                peek() == 'l' || peek() == 'L' || peek() == 'x' || peek() == 'X' ||
                (peek() >= 'a' && peek() <= 'f') || (peek() >= 'A' && peek() <= 'F'))
            sb.append(advance());
        String numberStr = sb.toString();
        if (numberStr.startsWith("0") && numberStr.length() >= 2 && Character.isDigit(numberStr.charAt(1))) {
            // 八进制数
            try {
                return new LiteralNode(Integer.parseInt(numberStr, 8), int.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid octal number: " + numberStr);
            }
        } else if (numberStr.startsWith("0b") || numberStr.startsWith("0B")) {
            try {
                return new LiteralNode(Integer.parseInt(numberStr.substring(2), 2), int.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid binary number: " + numberStr);
            }
        } else if (numberStr.startsWith("0x") || numberStr.startsWith("0X")) {
            try {
                return new LiteralNode(Integer.parseInt(numberStr.substring(2), 16), int.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid hexadecimal number: " + numberStr);
            }
        } else if (numberStr.startsWith("0o") || numberStr.startsWith("0O")) {
            try {
                return new LiteralNode(Integer.parseInt(numberStr.substring(2), 8), int.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid octal number: " + numberStr);
            }
        } else if (numberStr.endsWith("f") || numberStr.endsWith("F")) {
            try {
                return new LiteralNode(Float.parseFloat(numberStr.substring(0, numberStr.length() - 1)),
                        Float.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid float number: " + numberStr);
            }
        } else if (numberStr.endsWith("d") || numberStr.endsWith("D")) {
            try {
                return new LiteralNode(Double.parseDouble(numberStr.substring(0, numberStr.length() - 1)),
                        Double.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid double number: " + numberStr);
            }
        } else if (numberStr.contains("e") || numberStr.contains("E") || numberStr.contains(".")) {
            try {
                return new LiteralNode(Double.parseDouble(numberStr), Double.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid double number: " + numberStr);
            }
        } else if (numberStr.endsWith("l") || numberStr.endsWith("L")) {
            try {
                return new LiteralNode(Long.parseLong(numberStr.substring(0, numberStr.length() - 1)), Long.class);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid long number: " + numberStr);
            }
        } else {
            try {
                return new LiteralNode(Integer.parseInt(numberStr), int.class);
            } catch (NumberFormatException e) {
                try {
                    return new LiteralNode(Long.parseLong(numberStr), long.class);
                } catch (NumberFormatException e2) {
                    throw new RuntimeException("Invalid number: " + numberStr);
                }
            }
        }
    }

    private ASTNode parseArrayOrMap() throws RuntimeException {
        skipWhitespace();
        savePosition();
        try {
            ASTNode result = parseArray();
            releasePosition();
            return result;
        } catch (RuntimeException e) {
            restorePosition();
            savePosition();
            try {
                ASTNode result = parseMap();
                releasePosition();
                return result;
            } catch (RuntimeException e2) {
                restorePosition();
                throw new RuntimeException("Cannot parse as array or map: " + e + ", " + e2);
            }
        }
    }

    private ASTNode parseArray() throws RuntimeException {
        skipWhitespace();
        expectToMove('{', '[');
        List<ASTNode> elements = new ArrayList<>();

        while (peek() != '}' && peek() != ']') {
            char currentChar = peek();
            if (currentChar == ',' || currentChar == '}' || currentChar == ']') {
                throw new RuntimeException("Array element cannot be empty, position: " + position
                        + ", current character: '" + currentChar + "'");
            }

            skipWhitespace();
            elements.add(parseLiteral());
            skipWhitespace();
            if (peek() == ',') {
                advance();
                skipWhitespace();
            } else {
                break;
            }
        }
        expectToMove('}', ']');
        return new ArrayNode(elements);
    }

    private ASTNode parseMap() throws RuntimeException {
        skipWhitespace();
        expectToMove('{');
        Map<ASTNode, ASTNode> entries = new HashMap<>();

        while (peek() != '}') {
            skipWhitespace();
            
            ASTNode key = parseLiteral();
            skipWhitespace();
            expectToMove(':');
            ASTNode value = parseLiteral();
            entries.put(key, value);
            skipWhitespace();
            if (peek() == ',') {
                advance();
                skipWhitespace();
            } else {
                break;
            }
        }
        expectToMove('}');
        return new MapNode(entries, Map.class);
    }

    // 从下面开始就是需要用到 save/restore/release Position 的地方了

    /**
     * 解析字面量。
     *
     * @return 节点
     * @throws RuntimeException 解析出错
     */
    private ASTNode parseLiteral() throws RuntimeException {
        skipWhitespace();
        savePosition();
        logger.debug("parseLiteral 开始于位置" + position + ", 大致内容：" + peek(20));
        boolean failure = false;
        try {
            ASTNode expr = parsePrimary();

            while (match('.')) {
                skipWhitespace();
                String member = parseIdentifier();
                skipWhitespace();

                try {
                    if (expr instanceof ClassReferenceNode classRef) {
                        String nestedClassName = classRef.getClassName() + "." + member;
                        if (context.hasClass(nestedClassName)) {
                            // 是嵌套类
                            expr = new ClassReferenceNode(nestedClassName);
                            continue;
                        } else if (context.hasClass(classRef.getClassName())) {
                            try {
                                context.findClass(classRef.getClassName()).getDeclaredField(member);
                                // 是字段
                                expr = new FieldAccessNode(expr, member);
                                continue;
                            } catch (ClassNotFoundException | NoSuchFieldException ignored) {
                            }
                        }
                    } else if (expr.getType(context) != null && expr instanceof VariableNode) {
                        skipWhitespace();
                        if (peek() != '(') { // 不是方法调用
                            if (context.getFlag("W_CLASS_AS_VARIABLE") == null) {
                                logger.warn("尝试将一个类名作为变量值，将不会允许通过点运算符访问内部类，最好直接用类名，将不会展示此警告");
                                context.printlnWarn(
                                        "Attempted to access a class through a variable; consider using class name directly");
                                context.setFlag("W_CLASS_AS_VARIABLE", true);
                            }
                            expr = new FieldAccessNode(expr, member);
                            continue;
                        }
                    }
                } catch (Exception ignored) {
                }

                while (peek(2).equals("[]")) {
                    if (expr instanceof ClassReferenceNode classRef) {
                        expr = new ClassReferenceNode(classRef.getClassName() + "[]");
                    } else {
                        logger.error("不能给" + expr.getClass().getSimpleName() + "类型的变量添加数组后缀");
                        unexpectedToken();
                    }
                }

                // 如果后面有括号，创建方法调用节点
                if (peek() == '(') {
                    expectToMove('(');
                    skipWhitespace();
                    List<ASTNode> args = parseArguments();
                    skipWhitespace();
                    expectToMove(')');
                    expr = new MethodCallNode(expr, member, args);
                } else {
                    // 否则创建成员访问节点
                    expr = new MemberAccessNode(expr, member);
                }
            }

            while (peek(2).equals("[]")) {
                expectWordToMove("[]");
                if (expr instanceof ClassReferenceNode classRef) {
                    expr = new ClassReferenceNode(classRef.getClassName() + "[]");
                } else {
                    logger.error("不能给" + expr.getClass().getSimpleName() + "类型的变量添加数组后缀");
                    unexpectedToken();
                }
            }

            skipWhitespace();
            logger.debug("方法解析完成，接下来的大致内容: " + peek(20));
            while (matchWord("::")) {
                skipWhitespace();
                String methodName = parseIdentifier();
                skipWhitespace();
                
                expr = new MethodReferenceNode(expr, methodName);
            }
            skipWhitespace();

            while (peek() == '[' || peek() == '(') {
                if (match('[')) { // 数组操作
                    skipWhitespace();
                    ASTNode index = parseAdditive();
                    expectToMove(']');
                    expr = new ArrayAccessNode(expr, index);
                    skipWhitespace();
                } else if (match('(')) { // 如果还有函数调用 (比如 (() -> 114514)() 这种)
                    skipWhitespace();
                    List<ASTNode> args = parseArguments();
                    skipWhitespace();
                    expectToMove(')');
                    expr = new DirectFunctionCallNode(expr, args);
                    skipWhitespace();
                }
            }
            return expr;
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析表达式失败: " + e.getMessage());
            throw new RuntimeException("Error parsing expression: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    /**
     * 处理基本字面量。
     *
     * @return 解析出的节点
     * @throws RuntimeException 解析失败
     */
    private ASTNode parsePrimary() throws RuntimeException {
        skipWhitespace();
        savePosition(); // 0
        boolean failure = false;

        try {
            // 处理字面量
            if (peek() == '\'') {
                return parseChar();
            } else if (peek() == '"') {
                return parseString();
            } else if (peek() == '-' || peek() == '+' || Character.isDigit(peek())) {
                return parseNumber();
            } else if (peek() == '{' || peek() == '[') {
                ASTNode result =  parseArrayOrMap(); // 我用久了甚至都忘记new数组语法这一说了
                if (context.getFlag("W_RAW_ARRAY_EXPR") == null) {
                    logger.warn("直接指定的数组一般解释为原始类型，为了防止歧义，最好用 new typename[] {element...} 来代替");
                    context.printlnWarn("Arrays without 'new' constructor will be interpreted into primitive types, " +
                            "consider using 'new typename[] {element...}' for array literals");
                    context.setFlag("W_RAW_ARRAY_EXPR", true);
                }
                return result;
            }

            savePosition();
            try {
                ASTNode lambda = parseLambdaExpression();
                releasePosition();
                return lambda;
            } catch (RuntimeException e) {
                restorePosition();
            }

            if (peek() == '(') {
                expectToMove('(');
                
                // 检查是否是强制转换语法: (Type) expr
                savePosition();
                try {
                    String typeName = parseIdentifier();
                    skipWhitespace();
                    
                    // 如果后面是 )，说明是强制转换
                    if (peek() == ')') {
                        expectToMove(')');
                        skipWhitespace();
                        ASTNode expr = parseExpression();
                        return new CastNode(typeName, expr);
                    }
                } catch (RuntimeException e) {
                    // 不是强制转换，回退到括号表达式
                    restorePosition();
                }
                
                // 普通括号表达式
                ASTNode expr = parseExpression();
                expectToMove(')');
                return new ParenthesizedExpressionNode(expr);
            }

            String identifier;
            try {
                identifier = parseIdentifier();
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Expected identifier or literal at position " + position + ", but found '" + peek() + "'");
            }

            switch (identifier) {
                case "true":
                    return new LiteralNode(true, Boolean.class);
                case "false":
                    return new LiteralNode(false, Boolean.class);
                case "null":
                    return new LiteralNode(null, Void.class);
                case "new":
                    return parseConstructorCall();
            }

            if (context.hasBuiltIn(identifier)) {
                skipWhitespace();
                if (peek() == '(') {
                    expectToMove('(');
                    skipWhitespace();
                    List<ASTNode> args = parseArguments();
                    skipWhitespace();
                    expectToMove(')');
                    return new MethodCallNode(new VariableNode(identifier), identifier, args);
                } else {
                    return new VariableNode(identifier);
                }
            }

            try {
                if (!context.hasClass(identifier))
                    throw new ClassNotFoundException("Class not found: " + identifier);
                return new ClassReferenceNode(identifier);
            } catch (ClassNotFoundException e) {
                // 尝试解析更完整的类名
                skipWhitespace();
                if (peek() == '.') {
                    StringBuilder fullName = new StringBuilder(identifier);
                    String lastValidName = null; // 尽量匹配最长的类名，然后后面解析为字段（貌似可行？）
                    boolean parseSucceedOnce = false;
                    int index = -1; // 最后一次成功的位置
                    int origIndex = position; // 真的不想用那个savePosition了，还是int对我好
                    while (peek() == '.') {
                        expectToMove('.');
                        String nextPart = parseIdentifier();
                        fullName.append('.').append(nextPart);
                        if (context.hasClass(fullName.toString())) {
                            lastValidName = fullName.toString();
                            parseSucceedOnce = true;
                            index = position;
                        } else {
                            if (parseSucceedOnce) { // 后面就是类字段引用了
                                position = index;
                                break;
                            }
                            // 其他情况继续
                        }
                    }

                    if (lastValidName != null) {
                        logger.debug("当前位置: " + position + ", 大致内容: " + input.substring(position));
                        return new ClassReferenceNode(lastValidName);
                    } else {
                        // 没有匹配到类，回滚到原始位置
                        position = origIndex;
                    }
                }

                return new VariableNode(identifier);
            }

        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析基本表达式失败: " + e.getMessage());
            throw e;
        } finally {
            if (failure)
                restorePosition(); // 0
            else
                releasePosition(); // 0
        }
    }

    private ASTNode parseWhileStatement() {
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("while");
            skipWhitespace();
            expectToMove('(');
            ASTNode condition = parseExpression();
            expectToMove(')');

            skipWhitespace();
            ASTNode body;
            if (peek() == '{') {
                body = parseBlock(false);
            } else {
                BlockNode block = new BlockNode();
                ASTNode stmt = parseStatement();
                block.addStatement(stmt);
                body = block;
            }
            return new WhileNode(condition, body);

        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析while语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing while statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseDoWhileStatement() {
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("do");
            skipWhitespace();
            ASTNode body;
            if (peek() == '{') {
                body = parseBlock(false);
            } else {
                BlockNode block = new BlockNode();
                ASTNode stmt = parseStatement();
                block.addStatement(stmt);
                body = block;
            }

            skipWhitespace();
            expectWordToMove("while");
            skipWhitespace();
            expectToMove('(');
            ASTNode condition = parseExpression();
            expectToMove(')');
            skipWhitespace();
            expectToMove(';');

            return new DoWhileNode(condition, body);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析do-while语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing do-while statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseSwitchStatement() {
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("switch");
            skipWhitespace();
            expectToMove('(');
            ASTNode expression = parseExpression();
            expectToMove(')');
            skipWhitespace();
            expectToMove('{');

            List<CaseNode> cases = new ArrayList<>();
            ASTNode defaultCase = null;

            skipWhitespace();
            while (peek() != '}' && position < input.length()) {
                if (isTargetKeyWord("case")) {
                    expectWordToMove("case");
                    skipWhitespace();
                    ASTNode caseValue = parseExpression();
                    expectToMove(':');
                    skipWhitespace();

                    BlockNode caseBody = new BlockNode();
                    while (peek() != '}' && !isTargetKeyWord("case") && !isTargetKeyWord("default")) {
                        ASTNode stmt = parseCompletedStatement();
                        if (stmt != null) {
                            caseBody.addStatement(stmt);
                        } else {
                            char currentChar = peek();
                            if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                                throw new RuntimeException("Unparseable case statement, position: " + position
                                        + ", current character: '" + currentChar + "'");
                            }
                            advance();
                        }
                        skipWhitespace();
                    }
                    cases.add(new CaseNode(caseValue, caseBody));
                } else if (isTargetKeyWord("default")) {
                    expectWordToMove("default");
                    expectToMove(':');
                    skipWhitespace();

                    BlockNode defaultBody = new BlockNode();
                    while (peek() != '}' && !isTargetKeyWord("case")) {
                        ASTNode stmt = parseCompletedStatement();
                        if (stmt != null) {
                            defaultBody.addStatement(stmt);
                        } else {
                            char currentChar = peek();
                            if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                                throw new RuntimeException("Cannot parse default statement, position: " + position
                                        + ", current character: '" + currentChar + "'");
                            }
                            advance();
                        }
                        skipWhitespace();
                    }
                    defaultCase = defaultBody;
                } else {
                    char currentChar = peek();
                    if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                        throw new RuntimeException("Cannot parse switch statement, position: " + position
                                + ", current character: '" + currentChar + "'");
                    }
                    advance();
                    skipWhitespace();
                }
            }

            expectToMove('}');
            return new SwitchNode(expression, cases, defaultCase);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析switch语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing switch statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseReturnStatement() {
        skipWhitespace();
        expectWordToMove("return");
        if (peek() == ';') {
            return null;
        } else {
            ASTNode value = parseExpression();
            // expectToMove(';');
            return new ControlNode("return", value);
        }
    }

    private ASTNode parseTryStatement() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("try");
            skipWhitespace();

            List<ASTNode> resources = new ArrayList<>();
            
            if (peek() == '(') {
                expectToMove('(');
                skipWhitespace();
                
                while (peek() != ')') {
                    resources.add(parseTryResourceDeclaration());
                    skipWhitespace();
                    
                    if (peek() == ';') {
                        expectToMove(';');
                        skipWhitespace();
                    } else if (peek() != ')') {
                        throw new RuntimeException("Expected ';' or ')' in resource declaration, position: " + position);
                    }
                }
                
                expectToMove(')');
                skipWhitespace();
            }

            ASTNode tryBlock;
            if (peek() == '{') {
                tryBlock = parseBlock(false);
            } else {
                throw new RuntimeException("try block must be enclosed in braces");
            }

            List<TryCatchNode.CatchBlock> catchBlocks = new ArrayList<>();

            skipWhitespace();
            while (isTargetWord("catch")) {
                expectWordToMove("catch");
                skipWhitespace();
                expectToMove('(');

                skipWhitespace();
                String exceptionType = parseClassIdentifier();
                skipWhitespace();
                String exceptionName = parseIdentifier();
                skipWhitespace();
                expectToMove(')');

                ASTNode catchBlock;
                skipWhitespace();
                if (peek() == '{') {
                    catchBlock = parseBlock(false);
                } else {
                    catchBlock = parseCompletedStatement();
                }

                catchBlocks.add(new TryCatchNode.CatchBlock(exceptionType, exceptionName, catchBlock));
                skipWhitespace();
            }

            ASTNode finallyBlock = null;
            if (isTargetWord("finally")) {
                expectWordToMove("finally");
                skipWhitespace();
                if (peek() == '{') {
                    finallyBlock = parseBlock(false);
                } else {
                    finallyBlock = parseCompletedStatement();
                }
            }

            if (catchBlocks.isEmpty() && finallyBlock == null) {
                throw new RuntimeException("try statement must have at least one catch block or a finally block");
            }

            return new TryCatchNode(resources, tryBlock, catchBlocks, finallyBlock);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析try语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing try statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseTryResourceDeclaration() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            // 先尝试解析变量声明
            try {
                StringBuilder className = new StringBuilder(parseClassIdentifier());
                skipWhitespace();
                String variableName = parseIdentifier();
                skipWhitespace();

                while (peek() == '[') {
                    expectToMove('[');
                    expectToMove(']');
                    className.append("[]");
                    skipWhitespace();
                }

                ASTNode initialValue = null;
                if (peek() == '=') {
                    expectToMove('=');
                    skipWhitespace();
                    initialValue = parseExpression();
                }

                releasePosition();
                return new VariableDeclarationNode(className.toString(), variableName, initialValue);
            } catch (RuntimeException e) {
                // 变量声明解析失败，尝试解析表达式
                restorePosition();
                ASTNode expr = parseExpression();
                releasePosition();
                return expr;
            }
        } catch (RuntimeException e) {
            failure = true;
            throw new RuntimeException("Invalid resource declaration");
        } finally {
            if (failure)
                restorePosition();
        }
    }

    private ASTNode parseThrowStatement() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("throw");
            skipWhitespace();
            ASTNode exception = parseExpression();
            return new ThrowNode(exception);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析throw语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing throw statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseImportStatement() {
        savePosition();
        skipWhitespace();
        boolean failure = false;
        try {
            expectWordToMove("import");
            skipWhitespace();
            String pkgName = parseImportClassIdentifier();
            return new ImportNode(pkgName);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析Import失败: " + e.getMessage());
            throw new RuntimeException("Error parsing import statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseDeleteStatement() {
        savePosition();
        skipWhitespace();
        boolean failure = false;
        try {
            expectWordToMove("delete");
            skipWhitespace();
            String varName;
            if (peek() == '*') {
                advance();
                varName = "*";
            } else {
                varName = parseIdentifier();
            }
            return new DeleteNode(varName);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析delete失败: " + e.getMessage());
            throw new RuntimeException("Error parsing delete statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseBlock(boolean isIndependent) {
        savePosition();
        skipWhitespace();
        boolean failure = false;
        try {
            expectToMove('{');
            BlockNode block = new BlockNode(isIndependent);
            skipWhitespace();

            while (peek() != '}' && position < input.length()) {
                ASTNode stmt = parseCompletedStatement();
                if (stmt != null) {
                    block.addStatement(stmt);
                } else {
                    char currentChar = peek();
                    if (currentChar != '}' && !Character.isWhitespace(currentChar)) {
                        throw new RuntimeException("Unparseable code block statement, position: " + position
                                + ", current character: '" + currentChar + "'");
                    }
                    advance();
                }
                skipWhitespace();
            }
            expectToMove('}');
            return block;
        } catch (RuntimeException e) {
            failure = true;
            throw new RuntimeException("Error parsing block: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseForStatement() {
        savePosition();
        boolean failure = false;
        try {

            expectWordToMove("for");
            skipWhitespace();
            expectToMove('(');
            skipWhitespace();

            try {
                parseClassIdentifier();
                skipWhitespace();
                parseIdentifier();
                skipWhitespace();
                if (peek() == ':') {
                    restorePosition();
                    savePosition();
                    return parseForEachStatement();
                }
            } catch (Exception ignored) {
            }
            restorePosition();
            savePosition();
            return parseTraditionalForLoop();
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析for语句失败: " + e.getMessage());
            throw new RuntimeException("Error parsing for statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseTraditionalForLoop() {
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("for");
            skipWhitespace();
            expectToMove('(');
            skipWhitespace();
            ASTNode init = null;
            if (peek() != ';') {
                try {
                    init = parseVariableDeclaration();
                } catch (RuntimeException e) {
                    try {
                        init = parseVariableAssignment();
                    } catch (RuntimeException e2) {
                        init = parseExpression();
                    }
                }
            }
            skipWhitespace();
            expectToMove(';');
            skipWhitespace();

            ASTNode condition = null;
            if (peek() != ';')
                condition = parseExpression();

            skipWhitespace();
            expectToMove(';');
            skipWhitespace();

            skipWhitespace();
            ASTNode update = null;
            if (peek() != ')') {
                update = parseStatement();
            }

            skipWhitespace();
            expectToMove(')');

            skipWhitespace();
            ASTNode body;

            if (peek() == '{') {
                body = parseBlock(false);
            } else {
                BlockNode block = new BlockNode();
                ASTNode stmt = parseStatement();
                block.addStatement(stmt);
                body = block;
            }

            return new ForNode(init, condition, update, body);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析for循环失败: " + e.getMessage());
            throw new RuntimeException("Error parsing for loop: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseForEachStatement() {
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("for");
            skipWhitespace();
            expectToMove('(');
            skipWhitespace();

            String typeName = parseClassIdentifier();
            skipWhitespace();
            String itemName = parseIdentifier();
            skipWhitespace();
            expectToMove(':');

            skipWhitespace();
            ASTNode collection = parseExpression();
            expectToMove(')');

            skipWhitespace();
            ASTNode body;
            if (peek() == '{') {
                body = parseBlock(false);
            } else {
                BlockNode block = new BlockNode();
                ASTNode stmt = parseStatement();
                block.addStatement(stmt);
                body = block;
            }

            return new ForEachNode(typeName, itemName, collection, body);
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("解析for-each循环失败: " + e.getMessage());
            throw new RuntimeException("Error parsing for-each statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseConstructorCall() {
        skipWhitespace();
        List<ASTNode> args = null;
        ASTNode arrInitial = null;
        String className = parseClassIdentifier();
        if (className.endsWith("]")) {
            skipWhitespace();
            if (peek() == '{') {
                arrInitial = parseArray();
            } else {
                try {
                    arrInitial = parseExpression();
                } catch (RuntimeException ignored) {
                }
            }
        } else {
            skipWhitespace();
            expectToMove('(');
            skipWhitespace();
            args = parseArguments();
            skipWhitespace();
            expectToMove(')');
        }

        return new ConstructorCallNode(className, args, arrInitial);
    }

    public ASTNode parseStatement() {
        skipWhitespace();
        if (isAtEnd())
            return null;
        ASTNode result;
        skipWhitespace();

        if (isTargetKeyWord("class")) {
            result = parseClassDeclaration();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("interface")) {
            // return parseInterfaceDeclaration();
            throw new RuntimeException("Interface declarations are not yet supported");
        } else if (isTargetKeyWord("enum")) {
            // return parseEnumDeclaration();
            throw new RuntimeException("Enum declarations are not yet supported");
        }

        if (isTargetKeyWord("import")) {
            result = parseImportStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("delete")) {
            result = parseDeleteStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("if")) {
            result = parseIfStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("while")) {
            result = parseWhileStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("do")) {
            result = parseDoWhileStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("switch")) {
            result = parseSwitchStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("for")) {
            result = parseForStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("break")) {
            expectWordToMove("break");
            skipWhitespace();
            clearLastUncompletedStmt();
            return new ControlNode("break");
        } else if (isTargetKeyWord("continue")) {
            expectWordToMove("continue");
            skipWhitespace();
            clearLastUncompletedStmt();
            return new ControlNode("continue");
        } else if (isTargetKeyWord("return")) {
            result = parseReturnStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("try")) {
            result = parseTryStatement();
            clearLastUncompletedStmt();
            return result;
        } else if (isTargetKeyWord("throw")) {
            result = parseThrowStatement();
            clearLastUncompletedStmt();
            return result;
        }

        try {
            result = parseVariableDeclaration();
            setLastUncompletedStmt(result);
            return result;
        } catch (Exception ignored) {
        }

        int pos = position;
        try {
            if (peek() == '{') {
                result = parseBlock(true);
                clearLastUncompletedStmt();
                return result;
            }
        } catch (Exception e) {
            position = pos;
        }

        int savePos = position;
        try {
            result = parseAssignment();
            setLastUncompletedStmt(result);
            return result;
        } catch (Exception e) {
            position = savePos;
            result = parseExpression();
            setLastUncompletedStmt(result);
            skipWhitespace();
            if (peek() == ';') {
                advance();
                clearLastUncompletedStmt();
            }
            return result;
        }
    }

    public ASTNode parseCompletedStatement() {
        ASTNode result = parseStatement();

        if (isAtEnd())
            return result;

        while (!(getLastUncompletedStmt() == null) && !(peek() == ';')) {
            result = parseStatement();
            if (result == null)
                return null;
        }
        if (match(';'))
            clearLastUncompletedStmt();
        return result;
    }

    private String parseIdentifier() {
        StringBuilder sb = new StringBuilder();
        savePosition();
        while (position < input.length()) {
            char c = input.charAt(position);
            // 标识符可以包含字母, 数字, 下划线和美元符号
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                sb.append(c);
                position++;
            } else {
                break;
            }
        }
        String result = sb.toString();

        if (result.isEmpty()) {
            restorePosition();
            throw new RuntimeException("Cannot parse identifier at position " + position);
        } else {
            if (Character.isDigit(result.charAt(0))) {
                restorePosition();
                unexpectedToken("Invalid identifier: ");
            } else if (result.charAt(0) == '.') {
                restorePosition();
                unexpectedToken("Invalid identifier: ");
            }
        }
        releasePosition();
        return result;
    }

    private String parseClassIdentifier() {
        StringBuilder sb = new StringBuilder();
        savePosition(); // 0
        boolean inSquareBrackets = false;
        int genericDepth = 0; // 当前套了几层模板类
        while (position < input.length()) {
            char c = input.charAt(position);
            // 类名可以包含字母, 数字, 下划线, 小数点(完整的类名), 方括号(数组), 大小于号(模板类), 问号(泛型)和美元符号
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '?'
                    || c == '.' || c == '[' || c == ']' || c == '<' || c == '>') {
                if (c == ']') {
                    if (inSquareBrackets)
                        inSquareBrackets = false;
                    else {
                        unexpectedToken("Missing '['");
                    }
                } else if (c == '[') {
                    if (inSquareBrackets) {
                        unexpectedToken("Unexcepted '['");
                    } else
                        inSquareBrackets = true;
                } else if (c == '<') {
                    genericDepth++;
                } else if (c == '>') {
                    if (genericDepth == 0) {
                        restorePosition(); // 0
                        unexpectedToken("Missing '<'");
                    } else
                        genericDepth--;
                }
                sb.append(c);
            } else {
                if (c == ' ') {
                    if (inSquareBrackets || genericDepth > 0) {
                        sb.append(c);
                    } else {
                        break;
                    }
                } else if (c == ',') {
                    if (genericDepth > 0) {
                        sb.append(c);
                    } else {
                        restorePosition(); // 0
                        unexpectedToken("Unexcepted ','");
                    }
                } else {
                    break;
                }
            }
            position++;
        }
        if (genericDepth > 0) {
            restorePosition(); // 0
            unexpectedToken("Missing '>'");
        }
        if (inSquareBrackets) {
            restorePosition(); // 0
            unexpectedToken("Missing ']'");
        }
        String result = sb.toString();
        if (result.isEmpty()) {
            restorePosition(); // 0
            throw new RuntimeException("Cannot parse identifier at position " + position);
        } else {
            if (Character.isDigit(result.charAt(0))) {
                restorePosition(); // 0
                unexpectedToken("Invalid class name: ");
            } else if (result.charAt(0) == '.' || result.charAt(0) == '<' || result.charAt(0) == '>'
                    || result.charAt(0) == '[' || result.charAt(0) == ']') {
                restorePosition(); // 0
                unexpectedToken("Invalid class name: ");
            }
        }
        validateClassIdentifier(result);
        releasePosition(); // 0
        return result;
    }

    private void validateClassIdentifier(String className) {
        if (className == null || className.isEmpty()) {
            return;
        }

        int bracketDepth = 0;
        int angleBracketDepth = 0;

        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (c == '[') {
                bracketDepth++;
            } else if (c == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    throw new RuntimeException("Invalid class name: unmatched closing bracket ']' in " + className);
                }
            } else if (c == '<') {
                angleBracketDepth++;
            } else if (c == '>') {
                angleBracketDepth--;
                if (angleBracketDepth < 0) {
                    throw new RuntimeException(
                            "Invalid class name: unmatched closing angle bracket '>' in " + className);
                }
            }
        }

        if (bracketDepth != 0) {
            throw new RuntimeException("Invalid class name: unmatched opening bracket '[' in " + className);
        }

        if (angleBracketDepth != 0) {
            throw new RuntimeException("Invalid class name: unmatched opening angle bracket '<' in " + className);
        }
    }

    private String parseImportClassIdentifier() {
        StringBuilder sb = new StringBuilder();
        savePosition();
        boolean usedStar = false;
        while (position < input.length()) {
            char c = input.charAt(position);
            // 类名可以包含字母, 数字, 下划线, 小数点(完整的类名), 美元符号和星号（通配符)
            if (Character.isLetterOrDigit(c) || c == '.' || c == '$' || c == '_' || c == '*') {
                if (usedStar)
                    unexpectedToken();
                if (c == '*')
                    usedStar = true;
                sb.append(c);
                position++;
            } else {
                break;
            }
        }
        String result = sb.toString();
        if (result.isEmpty()) {
            restorePosition();
            throw new RuntimeException("Cannot parse identifier at position " + position);
        } else {
            if (Character.isDigit(result.charAt(0))) {
                restorePosition();
                unexpectedToken("Invalid class name: ");
            } else if (result.charAt(0) == '.' || result.charAt(0) == '<' || result.charAt(0) == '>'
                    || result.charAt(0) == '[' || result.charAt(0) == ']') {
                restorePosition();
                unexpectedToken("Invalid class name: ");
            }
        }
        validateImportClassIdentifier(result);
        releasePosition();
        return result;
    }

    private void validateImportClassIdentifier(String className) {
        if (className == null || className.isEmpty()) {
            return;
        }

        if (className.contains("<") || className.contains(">")) {
            throw new RuntimeException(
                    "Invalid import: generic types not allowed in import statement: " + className);
        }

        if (className.contains("[") || className.contains("]")) {
            throw new RuntimeException("Invalid import: array types not allowed in import statement: " + className);
        }
    }

    private ConstructorDefinition tryParseConstructor(String className) {
        skipWhitespace();
        savePosition();
        try {
            ConstructorDefinition constructorDef = parseConstructorDefinition(className, false);
            releasePosition();
            return constructorDef;
        } catch (RuntimeException e) {
            restorePosition();
            return null;
        }
    }

    private List<FieldDefinition> tryParseField() {
        skipWhitespace();
        savePosition();
        try {
            List<FieldDefinition> fieldDefs = parseFieldDeclaration();
            if (fieldDefs != null && !fieldDefs.isEmpty()) {
                releasePosition();
                return fieldDefs;
            }
            restorePosition();
            return null;
        } catch (RuntimeException e) {
            restorePosition();
            return null;
        }
    }

    private MethodDefinition tryParseMethod() {
        skipWhitespace();
        savePosition();
        try {
            MethodDefinition methodDef = parseMethodDeclaration();
            if (methodDef != null) {
                releasePosition();
                return methodDef;
            }
            restorePosition();
            return null;
        } catch (RuntimeException e) {
            restorePosition();
            return null;
        }
    }

    private ClassDeclarationNode parseClassDeclaration() {
        skipWhitespace();

        if (!matchWord("class")) {
            throw new RuntimeException("Expected 'class' keyword at position " + position);
        }
        skipWhitespace();

        String className = parseIdentifier();
        ClassDefinition classDef = new ClassDefinition(className);
        skipWhitespace();

        if (matchWord("extends")) {
            skipWhitespace();
            String superClassName = parseIdentifier();
            classDef.setSuperClassName(superClassName);
            skipWhitespace();
        }

        if (matchWord("implements")) {
            skipWhitespace();

            while (true) {
                String interfaceName = parseIdentifier();
                classDef.addInterfaceName(interfaceName);
                skipWhitespace();

                if (peek() == ',') {
                    expectToMove(',');
                    skipWhitespace();
                } else {
                    break;
                }
            }
        }

        expectToMove('{');
        skipWhitespace();

        int lastPosition = position;
        int stagnantCount = 0;
        while (peek() != '}') {
            skipWhitespace();
            if (peek() == '}')
                break;

            int startPos = position;
            advanceToNextMeaningful();
            String firstWord = peekWord();

            if (firstWord.isEmpty()) {
                throw new RuntimeException("Unterminated class declaration");
            }

            boolean parsed = false;

            if (firstWord.equals(className)) {
                ConstructorDefinition constructorDef = tryParseConstructor(className);
                if (constructorDef != null) {
                    classDef.addConstructor(constructorDef);
                    parsed = true;
                }
            }

            if (!parsed && isAccessModifier(firstWord)) {
                expectWordToMove(firstWord);
                skipWhitespace();
                int afterModifierPos = position;

                String secondWord = peekWord();

                if (secondWord.equals(className)) {
                    ConstructorDefinition constructorDef = parseConstructorDefinition(className, false);
                    if (constructorDef != null) {
                        classDef.addConstructor(constructorDef);
                        parsed = true;
                    } else {
                        position = afterModifierPos;
                    }
                }

                if (!parsed) {
                    List<FieldDefinition> fieldDefs = tryParseField();
                    if (fieldDefs != null && !fieldDefs.isEmpty()) {
                        for (FieldDefinition fieldDef : fieldDefs) {
                            classDef.addField(fieldDef.getFieldName(), fieldDef);
                        }
                        parsed = true;
                    } else {
                        position = afterModifierPos;
                    }
                }

                if (!parsed) {
                    MethodDefinition methodDef = tryParseMethod();
                    if (methodDef != null) {
                        classDef.addMethod(methodDef.getMethodName(), methodDef);
                        parsed = true;
                    } else {
                        position = afterModifierPos;
                    }
                }
            } else if (!parsed
                    && (isTypeOrVoid(firstWord) || firstWord.equals("static") || firstWord.equals("final"))) {
                if (firstWord.equals("static") || firstWord.equals("final")) {
                    String nextWord = peekWordAfterWhitespace();
                    if (isTypeOrVoid(nextWord)) {
                        expectWordToMove(nextWord);
                        skipWhitespace();

                        String thirdWord = peekWord();

                        if (!thirdWord.isEmpty() && !isKeyword(thirdWord) && peekAfterWord(thirdWord) == '(') {
                            MethodDefinition methodDef = parseMethodDeclarationWithType(nextWord);
                            if (methodDef != null) {
                                classDef.addMethod(methodDef.getMethodName(), methodDef);
                                parsed = true;
                            }
                        }

                        if (!parsed) {
                            List<FieldDefinition> fieldDefs = tryParseField();
                            if (fieldDefs != null && !fieldDefs.isEmpty()) {
                                for (FieldDefinition fieldDef : fieldDefs) {
                                    classDef.addField(fieldDef.getFieldName(), fieldDef);
                                }
                                parsed = true;
                            }
                        }

                        if (!parsed) {
                            MethodDefinition methodDef = tryParseMethod();
                            if (methodDef != null) {
                                classDef.addMethod(methodDef.getMethodName(), methodDef);
                                parsed = true;
                            }
                        }
                    } else {
                        List<FieldDefinition> fieldDefs = tryParseField();
                        if (fieldDefs != null && !fieldDefs.isEmpty()) {
                            for (FieldDefinition fieldDef : fieldDefs) {
                                classDef.addField(fieldDef.getFieldName(), fieldDef);
                            }
                            parsed = true;
                        }

                        if (!parsed) {
                            MethodDefinition methodDef = tryParseMethod();
                            if (methodDef != null) {
                                classDef.addMethod(methodDef.getMethodName(), methodDef);
                                parsed = true;
                            }
                        }
                    }
                } else {
                    List<FieldDefinition> fieldDefs = tryParseField();
                    if (fieldDefs != null && !fieldDefs.isEmpty()) {
                        for (FieldDefinition fieldDef : fieldDefs) {
                            classDef.addField(fieldDef.getFieldName(), fieldDef);
                        }
                        parsed = true;
                    }

                    if (!parsed) {
                        MethodDefinition methodDef = tryParseMethod();
                        if (methodDef != null) {
                            classDef.addMethod(methodDef.getMethodName(), methodDef);
                            parsed = true;
                        }
                    }
                }
            }

            if (!parsed) {
                logger.warn("无法识别的类成员 at " + startPos + ": "
                        + input.substring(startPos, Math.min(startPos + 50, input.length())));
                position = startPos;
                advanceToNextMeaningful();
                if (position == startPos) {
                    position++;
                }
            }

            if (position == lastPosition) {
                stagnantCount++;
                if (stagnantCount > 100) {
                    throw new RuntimeException(
                            "Class body parsing stuck in infinite loop, position stuck at: " + position);
                }
            } else {
                stagnantCount = 0;
                lastPosition = position;
            }
        }

        expectToMove('}');

        return new ClassDeclarationNode(className, classDef);
    }

    private ASTNode parseVariableDeclaration() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            StringBuilder className = new StringBuilder(parseClassIdentifier());
            skipWhitespace();
            String variableName = parseIdentifier();
            skipWhitespace();

            while (peek() == '[') { // 也有把方括号写在变量名后面的
                expectToMove('[');
                expectToMove(']');
                className.append("[]");
                skipWhitespace();
            }

            if (peek() == '=') {
                expectToMove('=');
                ASTNode value = parseExpression();
                expect(';');
                return new VariableDeclarationNode(className.toString(), variableName, value);
            } else {
                expect(';');
                return new VariableDeclarationNode(className.toString(), variableName, null);
            }
        } catch (RuntimeException e) {
            failure = true;
            throw new RuntimeException("Invalid variable declaration");
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseVariableAssignment() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            String variableName = parseIdentifier();
            skipWhitespace();
            expectToMove('=');
            ASTNode value = parseExpression();
            expectToMove(';');
            return new VariableAssignmentNode(variableName, value);
        } catch (RuntimeException e) {
            failure = true;
            throw new RuntimeException("Invalid variable assignment");
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseLambdaExpression() {
        skipWhitespace();
        savePosition();
        boolean failure = false;

        try {

            boolean hasLeftParenthesis = match('(');
            List<String> parameters = new ArrayList<>();

            skipWhitespace();
            while (peek() != ')') {
                parameters.add(parseIdentifier());
                skipWhitespace();
                if (peek() == ',') {
                    advance();
                    skipWhitespace();
                } else if (peek(2).equals("->")) {
                    break;
                } else if (peek() == ')') {
                    if (hasLeftParenthesis) {
                        throw new RuntimeException("Invalid lambda expression");
                    }
                } else {
                    throw new RuntimeException("Invalid lambda expression");
                }
            }

            if (hasLeftParenthesis) expectToMove(')');

            skipWhitespace();

            if (!matchWord("->")) {
                throw new RuntimeException("Lambda expression missing '->'");
            }

            skipWhitespace();

            ASTNode body;
            if (peek() == '{') {
                body = parseBlock(false);
            } else {
                body = parseExpression();
            }

            return new LambdaNode(parameters, body, "Lambda");

        } catch (RuntimeException e) {
            failure = true;
            throw e;
        } finally {
            if (failure) {
                restorePosition();
            } else {
                releasePosition();
            }
        }
    }

    private ASTNode parseExpression() {
        ASTNode expr = parseAssignment();

        // 检查是否是直接函数调用：expr(args)
        skipWhitespace();
        if (peek() == '(') {
            // 这是一个直接函数调用
            expectToMove('(');
            skipWhitespace();
            List<ASTNode> args = parseArguments();
            skipWhitespace();
            expectToMove(')');

            // 根据expr的类型决定如何调用
            return new DirectFunctionCallNode(expr, args);
        }

        return expr;
    }

    private ASTNode parseAssignment() {
        savePosition();
        boolean failure = false;
        try {
            // 解析左侧表达式（可能是变量或其他表达式）
            ASTNode left = parseTernary();
            skipWhitespace();

            // 是否是赋值操作
            if (peek() == '=') {
                advance();
                skipWhitespace();
                if (left instanceof VariableNode) {
                    String varName = ((VariableNode) left).name;
                    ASTNode right = parseAssignment(); // 递归解析右侧表达式
                    return new VariableAssignmentNode(varName, right);
                } else if (left instanceof FieldAccessNode fieldAccess) {
                    // 字段赋值：obj.field = value
                    ASTNode right = parseAssignment(); // 递归解析右侧表达式
                    return new FieldAssignmentNode(fieldAccess.target, fieldAccess.fieldName, right);
                } else if (left instanceof ArrayAccessNode arrayAccess) {
                    // 数组赋值：arr[index] = value
                    ASTNode right = parseAssignment(); // 递归解析右侧表达式
                    return new ArrayAssignmentNode(arrayAccess.target, arrayAccess.index, right);
                } else {
                    throw new RuntimeException(
                            "Left side of assignment must be a variable, field access, or array access");
                }
            } else {
                String assignmentOp = null;
                String nextTwo = peek(2);
                String nextThree = peek(3);
                String nextFour = peek(4);

                if (nextTwo.equals("+=")) {
                    assignmentOp = "+";
                    advance(2);
                } else if (nextTwo.equals("-=")) {
                    assignmentOp = "-";
                    advance(2);
                } else if (nextTwo.equals("*=")) {
                    assignmentOp = "*";
                    advance(2);
                } else if (nextTwo.equals("/=")) {
                    assignmentOp = "/";
                    advance(2);
                } else if (nextTwo.equals("%=")) {
                    assignmentOp = "%";
                    advance(2);
                } else if (nextTwo.equals("^=")) {
                    assignmentOp = "^";
                    advance(2);
                } else if (nextTwo.equals("&=")) {
                    assignmentOp = "&";
                    advance(2);
                } else if (nextTwo.equals("|=")) {
                    assignmentOp = "|";
                    advance(2);
                } else if (nextThree.equals("<<=")) {
                    assignmentOp = "<<";
                    advance(3);
                } else if (nextThree.equals(">>=")) {
                    assignmentOp = ">>";
                    advance(3);
                } else if (nextFour.equals(">>>=")) {
                    assignmentOp = ">>>";
                    advance(4);
                }

                if (assignmentOp != null) {
                    skipWhitespace();
                    if (left instanceof VariableNode) {
                        String varName = ((VariableNode) left).name;
                        ASTNode right = parseAssignment();
                        return new VariableAssignmentNode(varName,
                                new BinaryOperatorNode(assignmentOp, left, right));
                    } else if (left instanceof ArrayAccessNode arrayAccess) {
                        ASTNode right = parseAssignment();
                        return new ArrayAssignmentNode(arrayAccess.target, arrayAccess.index,
                                new BinaryOperatorNode(assignmentOp, left, right));
                    } else {
                        logger.error("赋值操作的左侧不是变量，是" + left.getClass().getName());
                        throw new RuntimeException("Left side of assignment must be a variable or array access");
                    }
                }
            }
            return left;
        } catch (RuntimeException e) {
            failure = true;
            logger.debug("无法解析赋值或者表达式: " + e.getMessage());
            throw e;
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    private ASTNode parseIfStatement() {
        skipWhitespace();
        savePosition();
        boolean failure = false;
        try {
            expectWordToMove("if");
            skipWhitespace();
            expectToMove('(');
            ASTNode condition = parseExpression();
            skipWhitespace();
            expectToMove(')');
            ASTNode thenBlock;

            // 首先检查是否有块结构 { }
            skipWhitespace();
            if (peek() == '{') {
                thenBlock = parseBlock(false);
            } else {
                // 如果没有块结构，尝试解析单条语句
                thenBlock = parseCompletedStatement();
            }

            skipWhitespace();
            ASTNode elseBlock = null;
            if (matchWord("else")) {
                skipWhitespace();
                if (peek() == '{') {
                    elseBlock = parseBlock(false);
                } else {
                    elseBlock = parseCompletedStatement();
                }
            }
            return new IfNode(condition, thenBlock, elseBlock);
        } catch (RuntimeException e) {
            failure = true;
            logger.warn("无法解析if语句: " + e.getMessage());
            throw new RuntimeException("Invalid if statement: " + e.getMessage());
        } finally {
            if (failure)
                restorePosition();
            else
                releasePosition();
        }
    }

    /**
     * 解析参数列表。
     * 从左括号右边一个字符开始解析，直到右括号；不会消费右括号。
     *
     * @return 参数列表
     */
    private List<ASTNode> parseArguments() {
        List<ASTNode> args = new ArrayList<>();
        skipWhitespace();

        if (peek() == ')')
            return args;

        while (true) {
            ASTNode expr = parseExpression();
            args.add(expr);
            skipWhitespace();
            if (peek() == ')')
                break;

            if (peek() != ',') {
                char next = peek();
                if (next == ')') {
                    break;
                }
                if (next == '"' || Character.isLetterOrDigit(next) || next == '(' || next == '[') {
                    logger.warn("警告: 参数列表中位置" + position + "可能缺少逗号");
                    continue;
                }
                throw new RuntimeException("Expected ',' or ')' in arguments, found '" + next + "'");
            }

            expectToMove(',');
            skipWhitespace();
        }
        skipWhitespace();

        return args;
    }

    private ASTNode parseTernary() {
        ASTNode condition = parseLogicalOr();
        skipWhitespace();
        if (match('?')) {
            skipWhitespace();
            ASTNode thenExpr = parseTernary();
            skipWhitespace();
            expectToMove(':');
            ASTNode elseExpr = parseTernary();
            return new TernaryExprNode(condition, thenExpr, elseExpr);
        }

        return condition;
    }

    private ASTNode parseLogicalOr() {
        ASTNode left = parseLogicalAnd();
        skipWhitespace();
        while (matchWord("||")) {
            skipWhitespace();
            ASTNode right = parseLogicalAnd();
            left = new BinaryOperatorNode("||", left, right);
        }

        return left;
    }

    private ASTNode parseLogicalAnd() {
        ASTNode left = parseBitwiseOr();
        skipWhitespace();
        while (matchWord("&&")) {
            skipWhitespace();
            ASTNode right = parseBitwiseOr();
            left = new BinaryOperatorNode("&&", left, right);
        }
        return left;
    }

    private ASTNode parseBitwiseOr() {
        ASTNode left = parseBitwiseXor();
        skipWhitespace();
        while (isTargetWord("|") && !isTargetWord("|=") && !isTargetWord("||")) {
            expectToMove('|');
            skipWhitespace();
            ASTNode right = parseBitwiseXor();
            left = new BinaryOperatorNode("|", left, right);
        }
        return left;
    }

    private ASTNode parseBitwiseXor() {
        ASTNode left = parseBitwiseAnd();
        skipWhitespace();
        while (isTargetWord("^") && !isTargetWord("^=")) {
            expectToMove('^');
            skipWhitespace();
            ASTNode right = parseBitwiseAnd();
            left = new BinaryOperatorNode("^", left, right);
        }
        return left;
    }

    private ASTNode parseBitwiseAnd() {
        ASTNode left = parseEquality();
        skipWhitespace();
        while (isTargetWord("&") && !isTargetWord("&=") && !isTargetWord("&&")) {
            expectToMove('&');
            skipWhitespace();
            ASTNode right = parseEquality();
            left = new BinaryOperatorNode("&", left, right);
            releasePosition();
            savePosition();
        }
        return left;
    }

    private ASTNode parseEquality() {
        ASTNode left = parseRelational();
        skipWhitespace();
        while (true) {
            skipWhitespace();
            if (matchWord("instanceof")) {
                ASTNode right = parseRelational();
                left = new BinaryOperatorNode("instanceof", left, right);
            } else if (matchWord("==")) {
                ASTNode right = parseRelational();
                left = new BinaryOperatorNode("==", left, right);
            } else if (matchWord("!=")) {
                ASTNode right = parseRelational();
                left = new BinaryOperatorNode("!=", left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private ASTNode parseRelational() {
        ASTNode left = parseBitwiseShift();
        skipWhitespace();
        while (true) {
            skipWhitespace();
            if (match('<')) {
                if (match('=')) {
                    ASTNode right = parseBitwiseShift();
                    left = new BinaryOperatorNode("<=", left, right);
                } else {
                    ASTNode right = parseBitwiseShift();
                    left = new BinaryOperatorNode("<", left, right);
                }
            } else if (match('>')) {
                if (match('=')) {
                    ASTNode right = parseBitwiseShift();
                    left = new BinaryOperatorNode(">=", left, right);
                } else {
                    ASTNode right = parseBitwiseShift();
                    left = new BinaryOperatorNode(">", left, right);
                }
            } else {
                break;
            }
        }

        return left;
    }

    private ASTNode parseBitwiseShift() {
        ASTNode left = parseAdditive();
        skipWhitespace();
        while (true) {
            skipWhitespace();
            if (isTargetWord("<<") && !isTargetWord("<<=")) {
                expectWordToMove("<<");
                ASTNode right = parseAdditive();
                left = new BinaryOperatorNode("<<", left, right);
            } else if (isTargetWord(">>") && !isTargetWord(">>=")) {
                expectWordToMove(">>");
                ASTNode right = parseAdditive();
                left = new BinaryOperatorNode(">>", left, right);
            } else {
                break;
            }
        }

        return left;

    }

    private ASTNode parseAdditive() {
        ASTNode left = parseMultiplicative();
        skipWhitespace();
        while (true) {
            skipWhitespace();
            if (isTargetWord("+") && !isTargetWord("+=")) {
                expectToMove('+');
                ASTNode right = parseMultiplicative();
                left = new BinaryOperatorNode("+", left, right);
            } else if (isTargetWord("-") && !isTargetWord("-=")) {
                expectToMove('-');
                ASTNode right = parseMultiplicative();
                left = new BinaryOperatorNode("-", left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private ASTNode parseMultiplicative() {
        ASTNode left = parseUnary();
        skipWhitespace();
        while (true) {
            skipWhitespace();
            if (isTargetWord("*") && !isTargetWord("*=")) {
                expectToMove('*');
                ASTNode right = parseUnary();
                left = new BinaryOperatorNode("*", left, right);
            } else if (isTargetWord("/") && !isTargetWord("/=")) {
                expectToMove('/');
                ASTNode right = parseUnary();
                left = new BinaryOperatorNode("/", left, right);
            } else if (isTargetWord("%") && !isTargetWord("%=")) {
                expectToMove('%');
                ASTNode right = parseUnary();
                left = new BinaryOperatorNode("%", left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private ASTNode parseUnary() {
        skipWhitespace();
        boolean hasCast = false;
        String castType = null;
        if (matchWord("++")) {
            ASTNode operand = parseUnary();
            if (operand instanceof VariableNode) {
                return new IncrementNode(((VariableNode) operand).name, true, true);
            } else if (operand instanceof FieldAccessNode fieldAccess) {
                return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, true, true);
            } else {
                logger.error("前置++只能用在变量或字段访问上");
                throw new RuntimeException("++ can only be applied to variables or field access");
            }
        } else if (matchWord("--")) {
            ASTNode operand = parseUnary();
            if (operand instanceof VariableNode) {
                return new IncrementNode(((VariableNode) operand).name, true, false);
            } else if (operand instanceof FieldAccessNode fieldAccess) {
                return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, true, false);
            } else {
                logger.error("前置--只能用在变量或字段访问上");
                throw new RuntimeException("-- can only be applied to variables or field access");
            }
        } else if (match('+')) {
            return parseUnary();
        } else if (match('-')) {
            ASTNode operand = parseUnary();
            return new BinaryOperatorNode("-", new LiteralNode(0), operand);
        } else if (match('!')) {
            ASTNode operand = parseUnary();
            return new BinaryOperatorNode("!", operand, null);
        } else if (match('~')) {
            ASTNode operand = parseUnary();
            return new BinaryOperatorNode("~", operand, null);
        } else if (peek() == '(') {
            savePosition();
            match('(');
            boolean succeed = true;
            try {
                skipWhitespace();
                castType = parseClassIdentifier();
                skipWhitespace();
                expectToMove(')');
                hasCast = true;
            } catch (RuntimeException e) {
                succeed = false;
            } finally {
                if (succeed)
                    releasePosition();
                else
                    restorePosition();
            }
        }

        ASTNode result = parseLiteral();

        if (matchWord("++")) {
            if (result instanceof ParenthesizedExpressionNode paren) {
                if (paren.expression instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) paren.expression).name, false, true);
                } else if (paren.expression instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, true);
                }
                logger.error("后置++不能用在括号表达式上");
                throw new RuntimeException(
                        "++ can only be applied to variables or field access, not parenthesized expressions");
            } else if (result instanceof VariableNode) {
                return new IncrementNode(((VariableNode) result).name, false, true);
            } else if (result instanceof FieldAccessNode fieldAccess) {
                return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, true);
            } else {
                logger.error("后置++只能用在变量或字段访问上");
                throw new RuntimeException("++ can only be applied to variables or field access");
            }
        } else if (matchWord("--")) {
            if (result instanceof ParenthesizedExpressionNode paren) {
                if (paren.expression instanceof VariableNode) {
                    return new IncrementNode(((VariableNode) paren.expression).name, false, false);
                } else if (paren.expression instanceof FieldAccessNode fieldAccess) {
                    return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, false);
                }
                logger.error("后置--不能用在括号表达式上");
                throw new RuntimeException(
                        "-- can only be applied to variables or field access, not parenthesized expressions");
            } else if (result instanceof VariableNode) {
                return new IncrementNode(((VariableNode) result).name, false, false);
            } else if (result instanceof FieldAccessNode fieldAccess) {
                return new FieldIncrementNode(fieldAccess.target, fieldAccess.fieldName, false, false);
            } else {
                logger.error("后置--只能用在变量或字段访问上");
                throw new RuntimeException("-- can only be applied to variables or field access");
            }
        }
        return hasCast ? new CastNode(castType, result) : result;
    }

    /**
     * 解析字段声明
     */
    private List<FieldDefinition> parseFieldDeclaration() {
        try {
            skipWhitespace();
            String type = peekWord();
            expectWordToMove(type);
            skipWhitespace();
            String name = peekWord();
            expectWordToMove(name);
            skipWhitespace();

            if (peek() == '(') {
                return null;
            }

            List<FieldDefinition> fields = new ArrayList<>();

            if (peek() == '=') {
                expectToMove('=');
                skipWhitespace();
                ASTNode value = parseExpression();
                if (value != null) {
                    expectToMove(';');
                    fields.add(new FieldDefinition(name, type, value));
                    return fields;
                }
            } else if (peek() == ';') {
                expectToMove(';');
                fields.add(new FieldDefinition(name, type, null));
                return fields;
            } else if (peek() == ',') {
                fields.add(new FieldDefinition(name, type, null));
                while (peek() == ',') {
                    expectToMove(',');
                    skipWhitespace();
                    String nextName = peekWord();
                    expectWordToMove(nextName);
                    fields.add(new FieldDefinition(nextName, type, null));
                    skipWhitespace();
                    if (peek() == '=') {
                        expectToMove('=');
                        skipWhitespace();
                        parseExpression();
                    }
                }
                expectToMove(';');
                return fields;
            }
            fields.add(new FieldDefinition(name, type, null));
            return fields;
        } catch (Exception e) {
            logger.warn("解析字段声明失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析方法声明（假设当前位置在访问修饰符之后）
     */
    private MethodDefinition parseMethodDeclaration() {
        try {
            skipWhitespace();
            String returnType = peekWord();
            if (!isTypeOrVoid(returnType)) {
                return null;
            }
            expectWordToMove(returnType);
            skipWhitespace();

            String methodName = peekWord();
            if (methodName.isEmpty() || isKeyword(methodName)) {
                return null;
            }
            expectWordToMove(methodName);
            skipWhitespace();

            expectToMove('(');
            skipWhitespace();

            List<Parameter> parameters = new ArrayList<>();
            while (peek() != ')') {
                char currentChar = peek();
                if (currentChar == ',' || currentChar == ')') {
                    throw new RuntimeException("Method parameter cannot be empty, position: " + position
                            + ", current character: '" + currentChar + "'");
                }

                String paramType = peekWord();
                if (paramType.isEmpty()) {
                    throw new RuntimeException("Method parameter type cannot be empty, position: " + position);
                }
                expectWordToMove(paramType);
                skipWhitespace();
                String paramName = peekWord();
                if (paramName.isEmpty()) {
                    throw new RuntimeException("Method parameter name cannot be empty, position: " + position);
                }
                expectWordToMove(paramName);
                parameters.add(new Parameter(paramType, paramName));
                skipWhitespace();
                if (peek() == ',') {
                    expectToMove(',');
                    skipWhitespace();
                }
            }

            expectToMove(')');
            skipWhitespace();
            ASTNode methodBody = parseBlock(false);
            skipWhitespace();

            return new MethodDefinition(methodName, returnType, parameters, methodBody);
        } catch (Exception e) {
            logger.warn("解析方法声明失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析方法声明（带已知类型）
     */
    private MethodDefinition parseMethodDeclarationWithType(String returnType) {
        try {
            expectWordToMove(returnType);
            skipWhitespace();

            String methodName = peekWord();
            if (methodName.isEmpty() || isKeyword(methodName)) {
                return null;
            }
            expectWordToMove(methodName);
            skipWhitespace();

            expectToMove('(');
            skipWhitespace();

            List<Parameter> parameters = new ArrayList<>();
            while (peek() != ')') {
                char currentChar = peek();
                if (currentChar == ',' || currentChar == ')') {
                    throw new RuntimeException("Method parameter cannot be empty, position: " + position
                            + ", current character: '" + currentChar + "'");
                }

                String paramType = peekWord();
                if (paramType.isEmpty()) {
                    throw new RuntimeException("Method parameter type cannot be empty, position: " + position);
                }
                expectWordToMove(paramType);
                skipWhitespace();
                String paramName = peekWord();
                if (paramName.isEmpty()) {
                    throw new RuntimeException("Method parameter name cannot be empty, position: " + position);
                }
                expectWordToMove(paramName);
                parameters.add(new Parameter(paramType, paramName));
                skipWhitespace();
                if (peek() == ',') {
                    expectToMove(',');
                    skipWhitespace();
                }
            }

            expectToMove(')');
            skipWhitespace();
            ASTNode methodBody = parseBlock(false);
            skipWhitespace();

            return new MethodDefinition(methodName, returnType, parameters, methodBody);
        } catch (Exception e) {
            logger.warn("解析方法声明失败: " + e.getMessage());
            return null;
        }
    }

    private String peekWordAfterWhitespace() {
        int savedPosition = position;
        skipWhitespace();
        String word = peekWord();
        position = savedPosition;
        return word;
    }

    private char peekAfterWord(String word) {
        int savedPosition = position;
        expectWordToMove(word);
        skipWhitespace();
        char next = peek();
        position = savedPosition;
        return next;
    }

    /**
     * 解析构造函数定义
     *
     * @param className            类名
     * @param expectAccessModifier 是否期望解析访问修饰符（false表示访问修饰符已经被解析过了）
     */
    private ConstructorDefinition parseConstructorDefinition(String className, boolean expectAccessModifier) {
        skipWhitespace();
        int savedPos = position;
        if (expectAccessModifier) {
            savePosition();
        }

        try {
            // 解析访问修饰符（可选）
            String accessModifier = "public"; // 默认public
            if (expectAccessModifier) {
                if (isTargetWord("public")) {
                    expectWordToMove("public");
                    accessModifier = "public";
                } else if (isTargetWord("private")) {
                    expectWordToMove("private");
                    accessModifier = "private";
                } else if (isTargetWord("protected")) {
                    expectWordToMove("protected");
                    accessModifier = "protected";
                }
                skipWhitespace();
            }

            // 构造函数名应该与类名相同
            String constructorName = peekWord();
            if (!constructorName.equals(className)) {
                if (expectAccessModifier) {
                    restorePosition();
                } else {
                    position = savedPos;
                }
                return null;
            }
            expectWordToMove(constructorName);
            skipWhitespace();

            // 解析参数列表
            expectToMove('(');
            skipWhitespace();

            List<Parameter> parameters = new ArrayList<>();
            while (peek() != ')') {
                char currentChar = peek();
                if (currentChar == ',' || currentChar == ')') {
                    throw new RuntimeException("Constructor parameter cannot be empty, position: " + position
                            + ", current character: '" + currentChar + "'");
                }

                // 解析参数类型和名称 - 使用peekWord而不是parseClassIdentifier以支持原始类型
                int paramStartPos = position;
                String paramType = peekWord();
                if (paramType.isEmpty()
                        || (!TYPES.contains(paramType) && !Character.isUpperCase(paramType.charAt(0)))) {
                    throw new RuntimeException(
                            "Invalid parameter type at position " + paramStartPos + ": " + paramType);
                }
                expectWordToMove(paramType);
                skipWhitespace();
                String paramName = peekWord();
                if (paramName.isEmpty()) {
                    throw new RuntimeException("Constructor parameter name cannot be empty, position: " + position);
                }
                expectWordToMove(paramName);
                parameters.add(new Parameter(paramType, paramName));

                skipWhitespace();
                if (peek() == ',') {
                    expectToMove(',');
                    skipWhitespace();
                }
            }

            expectToMove(')');
            skipWhitespace();

            // 解析构造函数体
            ASTNode constructorBody = parseBlock(false);

            return new ConstructorDefinition(constructorName, parameters, constructorBody);

        } catch (RuntimeException e) {
            if (expectAccessModifier) {
                restorePosition();
            }
            logger.warn("解析构造函数定义失败: " + e.getMessage());
            return null;
        }
    }
}
