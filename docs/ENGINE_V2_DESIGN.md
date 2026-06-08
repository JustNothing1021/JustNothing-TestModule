# Engine v2 (engine_new) 重构设计文档

> **版本**: Draft 1.1 | **日期**: 2026-06-08
> **目标**: 从底层重构解释器引擎，解决类型推断、解析期校验、运行时开销等根本性问题
> **核心哲学: Parser 即编译器 — 能确定的错误，立即报错，不产出误导节点**

---

## 1. 背景与动机

### 1.1 现有 engine 的核心问题

| 问题 | 症状 | 根因 |
|------|------|------|
| **类解析报错错误** | `com.a.b.C` 报 "找不到变量 com" | Parser 回退时丢弃上下文，产出误导性 VariableNode |
| **运行时反射开销大** | 每次方法调用都 `Class.getMethod()` + 反射 invoke | 无预解析信息，Evaluator 全靠动态查找 |
| **方法重载决议模糊** | 同名多参数方法选错 | 解析期不知道参数类型 |
| **文件膨胀** | Parser 4000 行、CmdParamProcessor 1400 行 | 单类承担过多职责 |
| **节点无类型信息** | ASTNode 不携带任何类型元数据 | 设计初期未考虑类型系统 |

### 1.2 设计原则

1. **Parser 即校验器 — 能确定的错误立即爆炸（ParseException）** — 不产出"已知错误的节点"再让后续层去发现
2. **单一职责** — 每个类/文件 < 300 行，一个模块只做一件事
3. **类型无处不在** — ASTNode 携带类型标注，ParseContext 携带标识符信息
4. **可测试性** — 核心逻辑不依赖 Android/Xposed/Root（现有 engine 已做到 ✅）
5. **渐进式迁移** — 新旧引擎可共存，逐步替换

---

## 2. 包结构设计

### 2.1 目标包结构

```
com.justnothing.engine/
├── api/                        # 公共 API 层（对外暴露的接口）
│   ├── Engine.java             # 引擎入口：源码 → 执行结果
│   ├── ScriptContext.java      # 脚本执行上下文（替代 ExecutionContext）
│   └── TypeSystem.java         # 类型系统公共定义
│
├── type/                       # 类型系统（核心基础设施）
│   ├── JType.java              # 类型表示（统一处理原始/数组/泛型/空）
│   ├── PrimitiveType.java      # int, long, boolean 等
│   ├── ReferenceType.java      # 类类型（含泛型参数）
│   ├── ArrayType.java          # 数组类型
│   ├── FunctionType.java       # 函数类型 (args → return)
│   ├── VoidType.java           # void 类型
│   ├── TypeResolver.java       # 类型解析器（名字 → JType）
│   ├── TypeInferencer.java     # 类型推断器（ASTNode → JType）
│   └── TypeUtils.java          # 类型工具（兼容性检查、装箱拆箱等）
│
├── ast/                        # AST 节点体系
│   ├── node/                   # 节点实现（按类别分目录）
│   │   ├── expressions/        # 表达式节点
│   │   │   ├── LiteralNode.java
│   │   │   ├── BinaryOpNode.java
│   │   │   ├── UnaryOpNode.java
│   │   │   ├── TernaryNode.java
│   │   │   ├── LambdaNode.java
│   │   │   ├── MethodRefNode.java    # 方法引用 ::method
│   │   │   ├── CastNode.java
│   │   │   ├── InstanceofNode.java
│   │   │   └── NewObjectNode.java
│   │   ├── access/             # 访问节点
│   │   │   ├── VarNode.java         # 变量引用
│   │   │   ├── FieldAccessNode.java # 字段访问 obj.field
│   │   │   ├── ArrayAccessNode.java # 数组索引 arr[i]
│   │   │   └── ClassRefNode.java    # 类引用 ClassName
│   │   ├── calls/              # 调用节点
│   │   │   ├── MethodCallNode.java     # obj.method(args)
│   │   │   ├── StaticCallNode.java     # ClassName.method(args)
│   │   │   ├── FuncCallNode.java       # funcName(args) — 解释器函数
│   │   │   ├── NewInstanceNode.java    # new ClassName(args)
│   │   │   └── SuperCallNode.java      # super.method(args)
│   │   ├── statements/        # 语句节点
│   │   │   ├── BlockNode.java
│   │   │   ├── DeclNode.java          # var/def 声明
│   │   │   ├── AssignNode.java
│   │   │   ├── IfNode.java
│   │   │   ├── WhileNode.java
│   │   │   ├── ForNode.java
│   │   │   ├── ReturnNode.java
│   │   │   ├── BreakNode.java
│   │   │   ├── ContinueNode.java
│   │   │   ├── ThrowNode.java
│   │   │   ├── TryCatchNode.java
│   │   │   └── ImportNode.java
│   │   ├── definitions/       # 定义节点
│   │   │   ├── FuncDefNode.java       # def funcName(params): body
│   │   │   ├── ClassDefNode.java      # class ClassName: ...
│   │   │   ├── InterfaceDefNode.java  # interface IName: ...
│   │   │   └── EnumDefNode.java       # enum Name { ... }
│   │   └── special/            # 特殊节点
│   │       ├── PipelineNode.java      # expr |> func
│   │       ├── AsyncNode.java         # async def ...
│   │       ├── AwaitNode.java         # await expr
│   │       └── FStringNode.java       # f"..."
│   ├── base/
│   │   ├── AstNode.java         # 节点基类（带类型！）
│   │   ├── ExprNode.java        # 表达式节点基类
│   │   ├── StmtNode.java        # 语句节点基类
│   │   ├── SourceLocation.java  # 源码位置
│   │   └── NodeVisitor.java     # 访问者接口
│   └── factory/
│       └── NodeFactory.java     # 节点工厂（统一创建入口）
│
├── lexer/                      # 词法分析
│   ├── Lexer.java
│   ├── Token.java
│   ├── TokenType.java
│   └── TokenFactory.java
│
├── parser/                     # 语法分析（细分！）
│   ├── CythavaParser.java      # 主解析器（协调各子解析器）
│   ├── ParseContext.java       # 增强版解析上下文（含标识符追踪 + 作用域）
│   ├── expr/                   # 表达式解析（从 Parser 中拆出）
│   │   ├── ExprParser.java     # 表达式总调度
│   │   ├── PrimaryParser.java  # 基础表达式（字面量、变量、括号、类引用）
│   │   ├── PostfixParser.java  # 后缀操作（. [] ()）← 类解析校验在此
│   │   ├── PrefixParser.java   # 前缀操作（- ! ~ ++ --）
│   │   ├── BinaryParser.java   # 二元运算符
│   │   └── TernaryParser.java  # 三元运算符 ?:
│   ├── stmt/                   # 语句解析
│   │   ├── StmtParser.java
│   │   ├── DeclParser.java     # 声明语句 (def/var/class/...) ← 变量注册在此
│   │   └── ControlParser.java  # 控制流 (if/while/for/try)
│   ├── def/                    # 定义解析
│   │   ├── FuncDefParser.java  ← 函数注册在此
│   │   ├── ClassDefParser.java
│   │   └── ImportParser.java
│   └── error/
│       └── CythavaParseException.java  # 结构化错误（见第6节）
│
├── eval/                       # 求值/执行
│   ├── Evaluator.java          # 主求值器（协调各子求值器）
│   ├── EvalContext.java        # 执行上下文
│   ├── Scope.java              # 作用域
│   ├── ScopeManager.java       # 作用域管理器
│   ├── expr/                   # 表达式求值
│   │   ├── ExprEvaluator.java
│   │   ├── LiteralEval.java
│   │   ├── CallEval.java       # 方法/函数调用求值（利用预解析信息）
│   │   └── AccessEval.java     # 字段/数组访问求值
│   ├── stmt/                   # 语句执行
│   │   ├── StmtExecutor.java
│   │   ├── DeclExec.java
│   │   └── ControlExec.java
│   └── builtin/                # 内建函数
│       ├── BuiltinRegistry.java
│       └── impl/               # 各内建实现
│
├── codegen/                    # 字节码生成（远期目标）
│   ├── BytecodeGenerator.java  # 总入口：AST → byte[]
│   ├── ClassWriter.java        # 类文件写入
│   ├── MethodWriter.java       # 方法体生成
│   ├── InstructionSet.java     # 指令集定义
│   ├── ConstantPool.java       # 常量池管理
│   └── optim/
│       ├── PeepholeOptimizer.java    # 窥孔优化
│       └── DeadCodeEliminator.java   # 死代码消除
│
├── runtime/                    # 运行时支持
│   ├── CythavaRuntime.java     # 运行时环境
│   ├── LambdaRuntime.java      # Lambda 运行时支持
│   └── Sandbox.java            # 沙箱（从 BlockGuardSandbox 迁移改进）
│
└── security/                   # 安全子系统
    ├── Permission.java         # 权限定义
    ├── Policy.java             # 安全策略
    └── enforcer/
        ├── IoEnforcer.java
        ├── NetworkEnforcer.java
        └── ReflectEnforcer.java
```

### 2.2 对比旧结构

| 维度 | 旧 engine | 新 engine_new |
|------|-----------|---------------|
| 文件数 | ~30 个文件扁平排列 | ~60+ 个文件按职责分层 |
| 最大单文件 | Parser.java 4000+ 行 | 目标 < 300 行/文件 |
| AST 节点 | 全在 ast/nodes/ 一个包 | 按 expression/call/statement/access 分类 |
| 类型系统 | 无 | 完整 JType 体系 |
| 解析期校验 | 几乎没有 |独立的 resolve/ 包 |
| 字节码 | 无 | codegen/ 预留接口 |

---

## 3. AST 节点体系（带类型）

### 3.1 节点基类设计

```java
/**
 * 所有 AST 节点的基类。
 * 核心改进：每个节点携带解析期确定的类型标注。
 */
public abstract class AstNode {
    // === 位置信息 ===
    private final SourceLocation location;

    // === 类型标注（核心新增！）===
    // 解析期由 TypeInferencer 填充，求值期可直接使用
    private JType resolvedType;
    
    // === 解析状态 ===
    private ResolveStatus resolveStatus = ResolveStatus.UNRESOLVED;

    public enum ResolveStatus {
        UNRESOLVED,    // 尚未尝试解析
        RESOLVING,    // 正在解析中（检测循环依赖用）
        RESOLVED,     // 已成功解析
        ERROR         // 解析失败（附带错误信息）
    }

    // === 访问者模式 ===
    public abstract <T> T accept(NodeVisitor<T> visitor);

    // === 类型查询 ===
    public JType getType() { return resolvedType; }
    public void setType(JType type) { this.resolvedType = type; }
    public boolean isTypeResolved() { return resolveStatus == ResolveStatus.RESOLVED; }
}
```

### 3.2 表达式节点基类

```java
public abstract class ExprNode extends AstNode {
    // 表达式都有返回值（类型即返回值类型）
    
    /**
     * 编译期常量折叠：
     * 如果这个表达式在编译期就能确定值，返回该值。
     * 用于优化：2 + 3 → 直接存为 LiteralNode(5)
     */
    public Object tryConstantFold() { return null; } // 默认不支持
}
```

### 3.3 关键节点类型示例

#### ClassRefNode — 类引用（解决当前的核心痛点）

```java
public class ClassRefNode extends ExprNode {
    private final String qualifiedName;   // 用户写的原始名称
    private final List<ClassRefNode> typeArgs; // 泛型参数

    // 解析期填充（由 Parser 的 PostfixParser 在创建时确定）
    private final Class<?> resolvedClass;  // 反射得到的 Class 对象 — 不可能为 null！
                                           // 如果类不存在，Parser 直接抛异常了

    @Override
    public JType getType() {
        // 类型在节点创建时就已确定，无需延迟
        return ReferenceType.of(resolvedClass, typeArgs);
    }
}
// 注意：不存在 "未解析的 ClassRefNode" 这种状态。
// Parser 要么产出已解析的 ClassRefNode，要么抛 CythavaParseException。
```

#### MethodCallNode — 方法调用（预解析候选方法）

```java
public class MethodCallNode extends ExprNode {
    private final ExprNode target;        // 调用目标（可为 null 表示当前实例/静态）
    private final String methodName;      // 方法名
    private final List<ExprNode> arguments; // 参数列表

    // ===== 解析期填充（核心！）=====
    private List<Method> candidateMethods;  // 按名称匹配的候选方法列表
    private Method resolvedMethod;          // 如果能唯一确定的重载
    
    private boolean isStaticCall;           // 是否是静态调用
    private boolean isConstructor;          // 是否是构造器调用

    /** 
     * 参数类型全部已知后，做最终重载决议。
     * 在 TypeInferencer 的第二遍（或 Evaluator 前）调用。
     */
    public void resolveOverload() {
        if (candidateMethods == null || candidateMethods.isEmpty()) return;
        
        JType[] argTypes = arguments.stream()
            .map(ExprNode::getType)
            .toArray(JType[]::new);
            
        for (Method m : candidateMethods) {
            if (isApplicable(m, argTypes)) {
                this.resolvedMethod = m;
                this.resolvedType = ReferenceType.of(m.getReturnType());
                return;
            }
        }
        // 无法确定唯一重载 → 运行时再决定（保留动态分发能力）
    }
}
```

#### VarNode — 变量引用（关联声明）

```java
public class VarNode extends ExprNode {
    private final String name;

    // 解析期填充
    private DeclNode declaration;     // 指向对应的声明节点
    private int scopeDepth;           // 所在作用域深度（用于闭包捕获判断）
    private boolean isCaptured;       // 是否被闭包捕获
}
```

#### FuncDefNode — 函数定义（携带签名类型）

```java
public class FuncDefNode extends StmtNode {
    private final String name;
    private final List<ParamDecl> parameters;  // 参数列表（含类型标注）
    private final ClassRefNode returnType;     // 返回类型（可能为 auto/infer）
    private final BlockNode body;

    // 解析期填充
    private FunctionType functionType;         // (paramTypes...) → returnType
    private boolean isAsync;                   // 是否是 async 函数
}
```

### 3.4 节点对比表：旧 vs 新

| 旧节点 | 新节点 | 改进 |
|--------|--------|------|
| VariableNode (只有 name) | VarNode (name + declaration + scopeDepth) | 可追踪到声明位置 |
| ClassReferenceNode (name + resolvedClass) | ClassRefNode (name + class + attemptedFullName + typeArgs) | 失败时可报告完整路径 |
| MethodCallNode (target + name + args) | MethodCallNode (+ candidateMethods + resolvedMethod + isStatic) | 预解析减少运行时反射 |
| FieldAccessNode (target + fieldName) | FieldAccessNode (+ resolvedField + isStatic) | 可提前验证字段存在性 |
| FunctionDefNode (分散在多处) | FuncDefNode (统一 + functionType + param types) | 完整签名信息 |
| LambdaNode (node + context) | LambdaNode (+ capturedVars + functionType) | 明确闭包捕获变量 |

---

## 4. 类型系统 (JType)

### 4.1 类型层次

```
JType (接口)
├── VoidType          // void
├── PrimitiveType     // int, long, float, double, boolean, char, byte, short
├── ReferenceType     // 类/接口类型
│   ├── className: String
│   ├── typeParameters: JType[]     // 泛型参数
│   ├── rawClass: Class<?>           // 反射 Class
│   ├── nullable: boolean            // 是否可空
│   └── isArray / dimensions: int    // 数组信息
├── ArrayType         // 显式数组类型 (简化版，也可合并到 ReferenceType)
└── FunctionType      // 函数类型 (用于 Lambda/高阶函数)
    ├── paramTypes: JType[]
    ├── returnType: JType
    └── isVarArgs: boolean
```

### 4.2 类型解析流程

```
源码中的类型引用
    ↓
TypeResolver.resolve("List<String>")
    ↓
1. 查找基础类型 List → Class<?>
2. 解析泛型参数 String → PrimitiveType/String
3. 组合 → ReferenceType(List, [String])
    ↓
存储到对应 ASTNode.resolvedType
```

### 4.3 类型推断规则

| 表达式 | 推断出的类型 |
|--------|-------------|
| `42` | `PrimitiveType.INT` |
| `"hello"` | `ReferenceType(String)` |
| `[1, 2, 3]` | `ReferenceType(int[])` |
| `x + y` | 数值提升规则 (int+int→int, int+double→double) |
| `a && b` | `PrimitiveType.BOOLEAN` |
| `obj.method()` | 方法的返回类型（来自 resolvedMethod） |
| `lambda x: x*2` | `FunctionType(INT → INT)` |
| `SomeClass::method` | 取决于签名 |
| `new ArrayList()` | `ReferenceType(ArrayList)` |
| `condition ? a : b` | LCA(a.type, b.type) 最小公共父类型 |

---

## 5. ParseContext 重设计

### 5.1 新增能力

```java
public class ParseContext {

    // === 保留的原有能力 ===
    private List<String> imports;
    private Map<String, String> typeAliases;
    private Set<String> declaredClasses;
    private ClassLoader classLoader;
    private IClassFinder classFinder;

    // ====== 新增：标识符追踪 ======
    
    /** 所有已声明的变量名（全局 + 当前作用域） */
    private final Deque<ScopeFrame> scopeStack = new ArrayDeque<>();
    
    /** 所有已声明的函数名 */
    private final Set<String> knownFunctions = new HashSet<>();
    
    /** 当前正在解析的类的字段 */
    private Set<String> currentClassFields;

    /** 当前方法的参数 */
    private Set<String> currentMethodParams;

    // ====== 作用域帧 ======
    public static class ScopeFrame {
        final Set<String> variables = new HashSet<>();  // 本层声明的变量
        final int depth;                                  // 嵌套深度
        final ScopeKind kind;                             // BLOCK / FUNCTION / CLASS / SCRIPT
        
        enum ScopeKind { SCRIPT, BLOCK, FUNCTION, CLASS, LAMBDA }
    }

    // ====== 声明注册 API ======

    /** 进入新作用域 */
    public void pushScope(ScopeKind kind) {
        scopeStack.push(new ScopeFrame(scopeStack.size(), kind));
    }

    /** 退出作用域 */
    public void popScope() {
        scopeStack.pop();
    }

    /** 注册变量声明 (def/var) */
    public void declareVariable(String name) {
        if (!scopeStack.isEmpty()) {
            scopeStack.peek().variables.add(name);
        }
    }

    /** 注册函数声明 (def funcName) */
    public void declareFunction(String name) {
        knownFunctions.add(name);
    }

    /** 判断一个名称是否是已知的标识符（变量/函数/参数） */
    public boolean isKnownIdentifier(String name) {
        // 1. 检查局部变量（从内到外）
        for (ScopeFrame frame : scopeStack) {
            if (frame.variables.contains(name)) return true;
        }
        // 2. 检查方法参数
        if (currentMethodParams != null && currentMethodParams.contains(name)) return true;
        // 3. 检查类字段
        if (currentClassFields != null && currentClassFields.contains(name)) return true;
        // 4. 检查函数
        if (knownFunctions.contains(name)) return true;
        // 5. 检查内置函数
        if (BuiltinRegistry.isBuiltin(name)) return true;
        return false;
    }

    /** 判断是否应该将点号名称作为类来解析 */
    public boolean shouldTryAsClass(String firstName) {
        return !isKnownIdentifier(firstName);
    }

    // ... 保留原有的 resolveClass / addImport / etc.
}
```

### 5.2 Parser 中的使用方式

```java
// 在 parsePrimaryExpression() 中遇到 identifier.dot.xxx 时：

if (consumedDot) {
    if (context.shouldTryAsClass(firstToken)) {
        // 尝试类解析
        String fullAttemptedName = buildFullDottedName();
        Class<?> clazz = context.resolveClass(fullAttemptedName);
        if (clazz != null) {
            return new ClassRefNode(fullAttemptedName, clazz, location);
        }
        // 类解析失败 → 直接报错！（不再回退到 VariableNode）
        throw new ParseException(
            "Class not found: " + fullAttemptedName,
            ErrorCode.CLASS_NOT_FOUND,
            location
        );
    } else {
        // 是已知标识符 → 正常的字段/方法访问链
        return parseFieldOrMethodChain(varNode);
    }
} else {
    // 没吃 dot → 普通 VariableNode
    return new VarNode(tokenText, location);
}
```

### 5.3 声明时的注册时机

```java
// 解析 def x = 10 时:
private StmtNode parseDeclaration() {
    Token nameToken = consume(IDENTIFIER);
    String varName = nameToken.text();
    
    // ⬇️ 立即注册到 ParseContext
    context.declareVariable(varName);
    
    // ... 继续解析初始化表达式
}

// 解析 def myFunc(params): 时:
private FuncDefNode parseFunctionDef() {
    String funcName = consume(IDENTIFIER).text();
    
    // ⬇️ 立即注册到 ParseContext
    context.declareFunction(funcName);
    
    context.pushScope(ScopeKind.FUNCTION);  // 进入函数作用域
    // ... 解析参数并逐个 declareVariable
    // ... 解析函数体
    context.popScope();                     // 退出函数作用域
}
```

---

## 6. Parser 内联校验（核心设计决策）

### 6.1 哲学：不需要独立的 Resolver 层

> **关键洞察：如果 Parser 已经能判断某事是错的，为什么还要产出节点让别的层来发现它是错的？**

传统编译器有独立的 "语义分析/类型检查" 阶段，是因为：
- 多遍扫描架构（先建 AST 再分析）
- 需要处理前向引用（函数定义在使用之后）
- 类型推断需要完整的全局信息

**但我们的场景不同：**
- 脚本是**从头到尾顺序解析**的
- `def` / `var` 声明在解析时就能立即注册到 ParseContext
- 类名解析只需要 ClassLoader + imports，不依赖后续代码

**结论：Parser 自身就是校验器。能确定的错误，立即 throw ParseException。**

### 6.2 新流程：Parser = 解析 + 校验 + 标注 一体化

```
旧: 源码 → Lexer → Parser(产出可能错误的AST) → Resolver(二次检查) → Evaluator
新: 源码 → Lexer → Parser(解析+校验+类型标注一体化)     → Evaluator
                         ↑
                    这一步完成所有静态可确定的检查
```

### 6.3 Parser 在哪些点做校验？

| 校验点 | 触发条件 | 处理方式 |
|--------|---------|---------|
| **类不存在** | 吃了 dot + 首段不在已知标识符中 + 类解析失败 | **立即 throw ParseException** |
| **变量未声明** | 使用标识符但 ParseContext 中找不到 | **立即 throw ParseException** |
| **函数未声明** | 调用函数名但 knownFunctions 中找不到 | **立即 throw ParseException** |
| **方法不存在** | 目标是已解析类 + 方法名在类中完全不存在 | **立即 throw ParseException** |
| **字段不存在** | 目标是已解析类 + 字段名在类中不存在 | **立即 throw ParseException** |
| **重复声明** | 同一作用域内同名 def/var | **立即 throw ParseException** |
| **类型不兼容** | 赋值/参数传递时两侧类型不匹配 | **立即 throw ParseException** |
| **方法重载模糊** | 多个候选方法都匹配（无法唯一确定） | **Warning 或 ParseException（严格模式）** |

### 6.4 什么留给运行时？

| 检查项 | 为什么不能在 Parser 做 |
|--------|---------------------|
| **多态分派** | 实际对象类型运行时才知道 |
| **动态字段访问** | 解释器对象的属性可能是动态的 |
| **null 安全** | 运行时值是否为 null |
| **数组越界** | 运行时索引值才知道 |
| **除零错误** | 运行时除数才知道 |

这些才是 Evaluator 的职责。Evaluator 收到的 AST 是 **经过 Parser 全部静态校验通过的**，只负责运行时的动态行为。

### 6.5 类型标注时机

类型标注不是单独的一遍 pass，而是 **伴随解析过程同步进行**：

```java
// 解析表达式时，每个子解析器返回带类型的节点：

// PrimaryParser.parseLiteral("42")
//   → LiteralNode(value=42, type=PrimitiveType.INT) ← 立即确定

// BinaryParser.parseAdd(a, b)
//   → BinaryOpNode(op=+, left=a, right=b, type=数值提升结果) ← 子节点类型已知道

// PostfixParser.parseMethodCall(target, "methodName", args)
//   → MethodCallNode(
//         target=target,
//         methodName="methodName",
//         args=args,
//         candidateMethods=从target.type的Class中按名查找,  // ← 立即填充！
//         resolvedMethod=如果能唯一确定则填充,              // ← 可能需要延迟
//         type=resolvedMethod?.returnType ?? 推断中          // ← 大部分情况可确定
//       )
```

对于少数需要延迟的情况（如方法重载决议依赖复杂表达式的类型），使用 `ResolveStatus.RESOLVING` 标记，在创建完所有子节点后回填。

### 6.6 ParseException 结构化

```java
public class CythavaParseException extends Exception {
    public enum ErrorCode {
        CLASS_NOT_FOUND("类不存在: %s"),
        UNDECLARED_VARIABLE("未声明的变量: %s"),
        UNDECLARED_FUNCTION("未定义的函数: %s"),
        METHOD_NOT_FOUND("类 %s 中不存在方法 %s"),
        FIELD_NOT_FOUND("不存在字段: %s"),
        TYPE_MISMATCH("类型不匹配: 不能将 %s 赋给 %s"),
        DUPLICATE_DECLARATION("重复声明: %s"),
        INVALID_SYNTAX("语法错误: %s"),
        ARGUMENT_COUNT_MISMATCH("参数数量不匹配: 方法 %s 需要 %d 个, 提供 %d 个");
        
        private final String template;
        // format(args...) → 用户友好的错误消息
    }
    
    private final ErrorCode errorCode;
    private final SourceLocation location;      // 精确到 token 级的位置
    private final String[] contextLines;        // 出错行的上下文源码
    
    /** 友好的错误输出 */
    public String getFormattedMessage() { ... }
    
    /** 类似编译器的输出格式:
     *  Error at line 42:14: Class not found: com.a.b.NonExisting
     *      |
     *  42   | val x = com.a.b.NonExisting.field
     *      |            ^^^^^^^^^^^^^^^^^^^^^^^^
     */
}
```

---

## 7. 字节码生成 (codegen/) — 远期蓝图

### 7.1 为什么考虑字节码？

| 方案 | 执行速度 | 内存开销 | 动态特性 | 实现复杂度 |
|------|---------|---------|---------|-----------|
| AST 树遍历求值（现状） | 慢（每次都要判别节点类型） | 高（整棵 AST 常驻内存） | 强（随时可修改） | 低 |
| **字节码 + JVM 执行** | **快（原生 JVM 速度）** | **低（字节码紧凑）** | 中（需重新生成） | 高 |
| ASM 动态生成 | 最快 | 最低 | 弱 | 最高 |

### 7.2 架构构想

```
AST (带完整类型信息)
    ↓ BytecodeGenerator.generate(methodNode)
    ↓
byte[] bytecode (JVM 字节码)
    ↓
ClassLoader.defineClass(name, bytecode, 0, len)
    ↓
Method.invoke() 或直接调用（接近原生 Java 性能）
```

### 7.3 关键设计决策

**1. 生成粒度：按方法生成**

每个 FuncDefNode / LambdaNode / ScriptBlock 生成一个独立的方法体。不需要一次性生成整个程序。

**2. 运行时支持**

解释器的特殊语义需要在运行时支持：
```java
public class CythavaRuntime {
    // 动态字段访问（解释器对象的属性读写）
    public static Object getField(Object target, String name) { ... }
    public static void setField(Object target, String name, Object value) { ... }
    
    // 动态方法调用（解释器级别的方法分派）
    public static Object callMethod(Object target, String name, Object[] args) { ... }
    
    // 闭包/Lambda 支持
    public static Object createLambda(ClosureData data) { ... }
    
    // 类型转换（跨类型操作）
    public static Object convert(Object value, JType targetType) { ... }
    
    // 内建函数桥接
    public static Object builtin(String name, Object[] args) { ... }
}
```

**3. 热更新策略**

```
脚本修改 → 重新 Lexer+Parser+Resolver → 重新 generate → 新 ClassLoader 加载
旧的 ClassLoader 可以 GC 回收（确保没有活跃引用）
```

### 7.4 与当前引擎的关系

```
Phase 1 (当前):  AST 求值（树遍历）— 保持不变
Phase 2 (中期):  对热点代码（循环体、频繁调用的函数）选择性生成字节码
Phase 3 (远期):  全部代码生成字节码执行，AST 仅用于调试/IDE 支持
```

---

## 8. 迁移计划

### Phase 1: 基础设施搭建（本次）

1. 创建 `engine_new` Gradle 模块
2. 实现 `type/JType` 类型体系
3. 实现 `ast/base/AstNode` 基类（带类型标注）
4. 实现 `parser/ParseContext`（增强版，含标识符追踪）
5. 实现最小可用 Parser（能解析简单表达式）
6. 实现最小可用 Evaluator（能执行简单表达式）

**验收标准**: 能解析和执行 `def x = 42; def f(n) = n * 2; f(x);` 并正确报错 `NonExistingClass.foo`

### Phase 2: 核心语言特性

1. 补全所有表达式节点（二元/一元/三元/调用/访问）
2. 补全所有语句节点（控制流/声明/异常）
3. 补全定义节点（函数/类/接口）
4. 实现 TypeInferencer（自动类型推断）
5. 实现 Resolver（名称/类型/方法三重校验）

**验收标准**: MiniApp RPG 游戏能在新引擎上运行

### Phase 3: 高级特性 & 字节码

1. Pipeline / async-await / f-string
2. 预处理器
3. codegen/ 基础框架
4. 选择性 JIT（热点方法生成字节码）

### Phase 4: 替换

1. app 模块切换依赖: `engine` → `engine_new`
2. 废弃旧 engine
3. 清理迁移

---

## 9. 文件大小控制策略

| 规则 | 说明 |
|------|------|
| **硬上限**: 300 行/文件 | 超过就必须拆分 |
| **单一职责**: 一个类只做一件事 | Parser 不负责求值，Evaluator 不负责解析 |
| **组合优于继承** | 用小对象组合而非深层继承 |
| **接口先行** | 先定义接口再写实现，便于测试和替换 |
| **禁止上帝类** | 不允许出现 1000+ 行的协调者类 |

---

> 下一步: 基于这份设计文档开始搭建 engine_new 项目骨架。
