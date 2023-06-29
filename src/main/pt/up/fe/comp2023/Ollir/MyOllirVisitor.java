package pt.up.fe.comp2023.Ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.MyAnalyserUtils;
import pt.up.fe.comp2023.MySymbolTable;
import pt.up.fe.comp2023.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class MyOllirVisitor extends AJmmVisitor<String, String> {

    private final StringBuilder ollirCode;
    private final SymbolTable symbolTable;

    private int temporaryVarCounter = 0;

    private int labelCounter = 0;

    public MyOllirVisitor(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
    }
    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("VarDeclaration", this::dealWithField);
        addVisit("GeneralMethod", this::dealWithGeneralMethod);
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("Integer", this::dealWithInteger);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("GeneralStatement", this::dealWithGeneralStatement);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("NewArrayDeclaration", this::dealWithNewArrayDeclaration);
        addVisit("Length", this::dealWithLength);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("BinaryOpCompare", this::dealWithBinaryOpCompare);
        addVisit("BinaryOpLogical", this::dealWithBinaryOpLogical);
        addVisit("BinaryOpArithmetic", this::dealWithBinaryOpArithmetic);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("This", this::dealWithThis);
        addVisit("GeneralDeclaration", this::dealWithGeneralDeclaration);
        addVisit("Brackets", this::dealWithBrackets);
        addVisit("IfElseStatement", this::dealWithIfElseStatement);
        addVisit("IfStatement", this::dealWithIfStatement);
        addVisit("WhileStatement", this::dealWithWhileStatement);
        addVisit("BlockStatement", this::dealBlockStatement);

        this.setDefaultVisit(this::defaultVisitor);
    }

    private Type getTypeName(JmmNode jmmNode, String name){
        String methodName = "";


        if (jmmNode.getAncestor("GeneralMethod").isPresent()){
            methodName = jmmNode.getAncestor("GeneralMethod").get().get("methodName");
        } else {
            methodName = jmmNode.getAncestor("MainMethod").get().get("methodName");
        }

        Type type = new Type(name, false);

        for(Symbol variable: symbolTable.getLocalVariables(methodName)) {
            if (Objects.equals(name, variable.getName())){
                type = variable.getType();
            }
        }

        for(Symbol variable: symbolTable.getParameters(methodName)) {
            if (Objects.equals(name, variable.getName())){
                type = variable.getType();
            }
        }

        for(Symbol variable: symbolTable.getFields()) {
            if (Objects.equals(name, variable.getName())){
                type = variable.getType();
            }
        }
        return type;
    }

    private Boolean isImport(String s){
        boolean isImport = false;
        for (String imp : symbolTable.getImports()) {
            if (Objects.equals(s, imp)) {
                isImport = true;
                break;
            }
        }
        return isImport;
    }

    private Boolean isVirtual(String s){
        boolean isVirtual = false;
        for (String imp : symbolTable.getMethods()) {
            if (Objects.equals(s, imp)) {
                isVirtual = true;
                break;
            }
        }
        return isVirtual;
    }

    private Boolean isSpecial(String s){
        return Objects.equals(s, symbolTable.getClassName());
    }

    private int isParameter(String s, String methodName){
        int isParameter = 0;
        int cnt = 0;
        for (Symbol imp : symbolTable.getParameters(methodName)) {
            if (Objects.equals(s, imp.getName())) {
                isParameter = cnt;
                break;
            }
            cnt++;
        }
        return isParameter;
    }

    private String getNextTemp(Type type) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter++), type);
    }

    private String getNextTemp(String typecode, int check) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter++), typecode, check);
    }

    private String getNextTemp(String type) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter++), type);
    }

    private String getCurrentTemp(String type) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter), type);
    }
    private String getCurrentTemp(Type type) {
        return OllirUtils.getTempCode(String.valueOf(temporaryVarCounter), type);
    }

    private String getNextTempIndexed(Type type) {
        return OllirUtils.getTempCodeIndexed(String.valueOf(temporaryVarCounter++), type);
    }

    public enum LabelType {
        THEN,
        ENDIF,
        LOOP,
        BODY,
        ENDLOOP,
        CONDITION,
    }

    private String getNextLabel(LabelType label) {
        int labelVal = labelCounter++;
        switch (label) {
            case THEN -> {
                return "THEN_" + labelVal;
            }
            case ENDIF -> {
                return "ENDIF_" + labelVal;
            }
            case LOOP ->  {
                return "LOOP_" + labelVal;
            }
            case BODY -> {
                return "BODY_" + labelVal;
            }
            case ENDLOOP -> {
                return "ENDLOOP_" + labelVal;
            }
            case CONDITION -> {
                return "CONDITION_" + labelVal;
            }
        }

        return "NULL_" + labelVal;
    }

    private String defaultVisitor(JmmNode jmmNode, String s) {
        return "";
    }
    private String dealWithProgram(JmmNode jmmNode, String s) {
        s = (s!= null ?s:"");
        for(String importString: symbolTable.getImports()) {
            ollirCode.append("import "+ importString + ";\n");
        }

        for(JmmNode child: jmmNode.getChildren()) {
            ollirCode.append(visit(child, ""));
        }
        //ollirCode += s;
        return s;
    }

    private String emptyConstructor() {

        return ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "\t\tinvokespecial(this, \"\").V;\n" +
                "\t}\n\n";
    }

    private String dealWithClass(JmmNode jmmNode, String s) {

        ollirCode.append(symbolTable.getClassName());

        String superClass = symbolTable.getSuper();

        if(superClass != null) {
            ollirCode.append( " extends " + superClass);
        }

        ollirCode.append( " {\n");

        boolean insertedConstructor = false;

        for(JmmNode child: jmmNode.getChildren()) {
            if (!child.getKind().equals("Variable")
                    && !insertedConstructor) {
                ollirCode.append( "\t" + emptyConstructor());
                insertedConstructor = true;
            }

            ollirCode.append( visit(child, ""));
        }

        ollirCode.append( "}\n");

        return s;
    }

    private String dealWithField(JmmNode jmmNode, String s) {

        if(!jmmNode.getJmmParent().getKind().equals("Class"))
            return "";

        Symbol sym = MyAnalyserUtils.getSymbol(jmmNode);
        ollirCode.append( "\t.field private " + sym.getName() + "." + OllirUtils.getCode(sym.getType()) + ";\n\n");
        return s;
    }

    private String dealWithGeneralMethod(JmmNode jmmNode, String s) {
        String methodName;

        ollirCode.append( "\t.method public ");

        methodName = jmmNode.get("methodName");

        ollirCode.append( methodName + "(");

        List<Symbol> parameters = symbolTable.getParameters(methodName);

        String parametersCode = parameters.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append(parametersCode).append(").")
                .append(OllirUtils.getCode(symbolTable.getReturnType(methodName)))
                .append(" {\n");

        for(JmmNode child: jmmNode.getChildren()) {
            if(child.getIndexOfSelf() == jmmNode.getNumChildren()-1){
                if (child.getKind().equals("BinaryOpArithmetic") || child.getKind().equals("BinaryOpCompare") || child.getKind().equals("BinaryOpLogical") || child.getKind().equals("UnaryOp")){
                    String expression = visit(child, symbolTable.getReturnType(methodName).getName());
                    ollirCode.append( "\t\tret." + OllirUtils.getCode(symbolTable.getReturnType(methodName))
                            + " " + expression);
                }else{
                    ollirCode.append( "\t\tret." + OllirUtils.getCode(symbolTable.getReturnType(methodName)) + " ");
                    ollirCode.append( visit(child, symbolTable.getReturnType(methodName).getName()));
                }
            }else
                ollirCode.append( visit(child, ""));
            if(child.getIndexOfSelf() == jmmNode.getNumChildren()-1){
                ollirCode.append( ";\n");
            }
        }
        ollirCode.append( "\t}\n\n");
        return s;
    }

    private String dealWithMainMethod(JmmNode jmmNode, String s) {
        String methodName;

        ollirCode.append( "\t.method public ");

        ollirCode.append( "static ");
        methodName = "main";

        ollirCode.append( methodName + "(");

        List<Symbol> parameters = symbolTable.getParameters(methodName);

        String parametersCode = parameters.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append( parametersCode + ")." + OllirUtils.getCode(symbolTable.getReturnType(methodName)) + " {\n");

        for(JmmNode child: jmmNode.getChildren()) {
            if(!Objects.equals(child.getKind(), "VoidType") && !Objects.equals(child.getKind(), "Parameter"))
                ollirCode.append( visit(child, ""));
        }
        ollirCode.append("\t\tret.V;\n");
        ollirCode.append( "\t}\n\n");
        return s;
    }

    private String dealWithInteger(JmmNode jmmNode, String s) {
        int num = 0;
        if (jmmNode.getAncestor("GeneralMethod").isPresent() && isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName")) > 0){
            num = isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName"));
            return "$" + num + jmmNode.get("name") + ".i32";
        }else if(jmmNode.getAncestor("MainMethod").isPresent() && isParameter(jmmNode.get("name"), jmmNode.getAncestor("MainMethod").get().get("methodName")) > 0){
            num = isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName"));
            return "$" + num + jmmNode.get("name") + ".i32";
        }
        return jmmNode.get("name") + ".i32";
    }
    private String dealWithBoolean(JmmNode jmmNode, String s) {
        int num = 0;
        if (jmmNode.getAncestor("GeneralMethod").isPresent() && isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName")) > 0){
            num = isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName"));
            return "$" + num + jmmNode.get("name") + ".bool";
        }else if(jmmNode.getAncestor("MainMethod").isPresent() && isParameter(jmmNode.get("name"), jmmNode.getAncestor("MainMethod").get().get("methodName")) > 0){
            num = isParameter(jmmNode.get("name"), jmmNode.getAncestor("GeneralMethod").get().get("methodName"));
            return "$" + num + jmmNode.get("name") + ".bool";
        }
        return jmmNode.get("name") + ".bool";
    }

    private String dealWithIdentifier(JmmNode jmmNode, String s) {

        String ret = "";
        if(OllirUtils.isField(jmmNode.get("name"), symbolTable)){
            Type type = getTypeName(jmmNode, jmmNode.get("name"));

            String temp =  getNextTemp(type);

            ollirCode.append("\t\t").append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(type))
                    .append(" getfield(this, ")
                    .append(jmmNode.get("name")).append(".")
                    .append(OllirUtils.getCode(type))
                    .append(").")
                    .append(OllirUtils.getCode(type))
                    .append(";\n");

            return temp;
        }else{
            Type type = getTypeName(jmmNode, jmmNode.get("name"));
            ret = " " + jmmNode.get("name") + ".";
            ret += OllirUtils.getCode(type);
        }

        return ret;
    }

    private String dealWithBrackets(JmmNode jmmNode, String typecode) {
        return visit(jmmNode.getJmmChild(0), typecode);
    }


    private String dealWithGeneralStatement(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()){
            visit(child, "");
        }
        return s;
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {

        if(OllirUtils.isField(jmmNode.get("var"), symbolTable)){
            Type type = getTypeName(jmmNode, jmmNode.get("var"));
            String lhs = " " + jmmNode.get("var") + ".";
            lhs += OllirUtils.getCode(type);
            String rhs = visit(jmmNode.getJmmChild(0), "");
            ollirCode.append("\t\tputfield(this,")
                    .append(lhs)
                    .append(", ")
                    .append(rhs)
                    .append(").V;\n");
        }else{
            Type type = getTypeName(jmmNode, jmmNode.get("var"));

            String lhs = "\t\t" + jmmNode.get("var") + ".";
            lhs += OllirUtils.getCode(type);

            String rhs = visit(jmmNode.getJmmChild(0), OllirUtils.getCode(type));

            ollirCode.append(lhs + " :=." + OllirUtils.getCode(type) + " ");
            ollirCode.append( rhs);
            ollirCode.append( ";\n");
        }
        return s;
    }

    private String dealWithNewArrayDeclaration(JmmNode jmmNode, String s) {

        String arrVal = visit(jmmNode.getJmmChild(0), "");
        StringBuilder newIntArray = new StringBuilder();
        Type arrayType = new Type("int", true);

        newIntArray.append("new(array,")
                .append(arrVal)
                .append(").")
                .append(OllirUtils.getCode(arrayType));

        String temp =  getNextTemp(arrayType);
        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(arrayType))
                .append(" ")
                .append(newIntArray)
                .append(";\n");

        return temp;

    }

    private String dealWithLength(JmmNode jmmNode, String typecode) {
        String arg1 = visit(jmmNode.getJmmChild(0), typecode);

        Type type = new Type("int", false);

        String temp = getNextTemp(type);
        ollirCode.append("\t\t" + temp)
                .append(" :=.")
                .append(OllirUtils.getCode(type))
                .append(" arraylength(")
                .append(arg1)
                .append(").")
                .append(OllirUtils.getCode(type))
                .append(";\n");

        return temp;
    }
    private String dealWithUnaryOp(JmmNode jmmNode, String typecode) {
        String arg1 = visit(jmmNode.getJmmChild(0), typecode);


        String temp = getNextTemp(typecode, 0);
        ollirCode.append("\t\t" + temp)
                .append(" :=.")
                .append(typecode)
                .append(" !.")
                .append(typecode)
                .append(" ")
                .append(arg1)
                .append(";\n");

        return temp;
    }

    private String dealWithBinaryOpCompare(JmmNode jmmNode, String typecode) {

        String arg1 = visit(jmmNode.getJmmChild(0), "i32");
        String arg2 = visit(jmmNode.getJmmChild(1), "i32");

        String temp = getNextTemp("bool", 0);
        ollirCode.append("\t\t" + temp)
                .append(" :=.")
                .append("bool")
                .append(" ")
                .append(arg1);
        ollirCode.append( " " + jmmNode.get("op") + ".")
                .append("bool ")
                .append(arg2 + ";\n");

        return temp;
    }

    private String dealWithBinaryOpLogical(JmmNode jmmNode, String typecode) {

        String arg1 = visit(jmmNode.getJmmChild(0), typecode);
        String arg2 = visit(jmmNode.getJmmChild(1), typecode);

        String temp = getNextTemp(typecode, 0);
        ollirCode.append("\t\t").append(temp)
                .append(" :=.")
                .append(typecode)
                .append(" ")
                .append(arg1);
        ollirCode.append(" ")
                .append(jmmNode.get("op"))
                .append(".")
                .append("bool")
                .append(" ")
                .append(arg2).append(";\n");

        return temp;
    }

    private String dealWithBinaryOpArithmetic(JmmNode jmmNode, String typecode) {

        String arg1 = visit(jmmNode.getJmmChild(0), typecode);
        String arg2 = visit(jmmNode.getJmmChild(1), typecode);

        String temp = getNextTemp(typecode, 0);
        ollirCode.append("\t\t" + temp)
                .append(" :=.")
                .append(typecode)
                .append(" ")
                .append(arg1);
        ollirCode.append( " " + jmmNode.get("op") + ".")
                .append(typecode + " ")
                .append(arg2 + ";\n");

        return temp;
    }

    private String dealWithMethodCall(JmmNode jmmNode, String s) {
        String temp = "";
        Type type;
        List<String> string_args = new ArrayList<>();
        for (JmmNode child : jmmNode.getChildren()) {
            if (child != jmmNode.getJmmChild(0)) {
                string_args.add(visit(child, ""));
            }
        }
        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "This")) {
            type = new Type(symbolTable.getClassName(), false);
        }else if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "GeneralDeclaration")) {
            type = getTypeName(jmmNode, jmmNode.getJmmChild(0).get("name"));
            visit(jmmNode.getJmmChild(0));
        }else{
            type = getTypeName(jmmNode, jmmNode.getJmmChild(0).get("name"));
        }

        if(jmmNode.getJmmParent().getKind().equals("Assignment") || jmmNode.getJmmParent().getKind().equals("MethodCall") || jmmNode.getJmmParent().getKind().equals("Brackets")) {
            temp = getNextTemp(type);

            ollirCode.append(temp)
                    .append(" :=.")
                    .append(OllirUtils.getCode(type))
                    .append(" ");
        }
        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "This")) {
            ollirCode.append("invokevirtual(")
                    .append(visit(jmmNode.getJmmChild(0), ""))
                    .append(", \"")
                    .append(jmmNode.get("methodName"))
                    .append("\"");
        } else if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "Identifier")) {
            if (isImport(jmmNode.getJmmChild(0).get("name"))) {
                ollirCode.append("invokestatic(")
                        .append(jmmNode.getJmmChild(0).get("name"))
                        .append(", \"")
                        .append(jmmNode.get("methodName"))
                        .append("\"");
            }
        } else if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "GeneralDeclaration")) {
            ollirCode.append("invokevirtual(")
                    .append(getCurrentTemp(type))
                    .append(", \"")
                    .append(jmmNode.get("methodName"))
                    .append("\"");
        }

        for (String arg : string_args) {
            ollirCode.append(", ").append(arg);
        }
        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "This") || Objects.equals(jmmNode.getJmmChild(0).getKind(), "GeneralDeclaration")){
            ollirCode.append(").")
                    .append(OllirUtils.getCode(type))
                    .append(";\n");
        }
        else
            ollirCode.append(").V;\n");

        return temp;
    }

    private String dealWithThis(JmmNode jmmNode, String s) {
        return "this";
    }

    private String dealWithGeneralDeclaration(JmmNode jmmNode, String s) {
        StringBuilder newObj = new StringBuilder();
        Type objType = Utils.getType(jmmNode);

        newObj.append("new(")
                .append(jmmNode.get("name"))
                .append(").")
                .append(OllirUtils.getCode(objType));

        String temp =  getNextTemp(objType);

        ollirCode.append(temp)
                .append(" :=.")
                .append(OllirUtils.getCode(objType))
                .append(" ")
                .append(newObj)
                .append(";\n");

        ollirCode.append("invokespecial(")
                .append(temp)
                .append(",\"\").V;\n");

        return temp;
    }

    private String dealWithIfElseStatement(JmmNode jmmNode, String typecode) {

        String condition = visit(jmmNode.getJmmChild(0), typecode);
        String thenLabel = getNextLabel(LabelType.THEN);
        String endifLabel = getNextLabel(LabelType.ENDIF);

        JmmNode thenNode = jmmNode.getJmmChild(1);
        JmmNode elseNode = jmmNode.getJmmChild(2);

        ollirCode.append("if (")
                .append(condition)
                .append(") goto ")
                .append(thenLabel)
                .append(";\n");

        visit(elseNode, typecode);

        ollirCode.append("goto ")
                .append(endifLabel)
                .append(";\n")
                .append(thenLabel)
                .append(":\n");

        visit(thenNode, typecode);

        ollirCode.append(endifLabel)
                .append(":\n");

        return "";
    }

    private String dealWithIfStatement(JmmNode jmmNode, String typecode) {

        String condition = visit(jmmNode.getJmmChild(0), typecode);
        String thenLabel = getNextLabel(LabelType.THEN);
        String endifLabel = getNextLabel(LabelType.ENDIF);

        JmmNode thenNode = jmmNode.getJmmChild(1);

        ollirCode.append("if (")
                .append(condition)
                .append(") goto ")
                .append(thenLabel)
                .append(";\n");

        ollirCode.append("goto ")
                .append(endifLabel)
                .append(";\n")
                .append(thenLabel)
                .append(":\n");

        visit(thenNode, typecode);

        ollirCode.append(endifLabel)
                .append(":\n");

        return "";
    }

    private String dealWithWhileStatement(JmmNode jmmNode, String typecode) {

        String bodyLabel = getNextLabel(LabelType.BODY);
        String conLabel = getNextLabel(LabelType.CONDITION);
        JmmNode conditionNode = jmmNode.getJmmChild(0);
        JmmNode bodyNode = jmmNode.getJmmChild(1);

        ollirCode.append("\ngoto ")
                .append(conLabel)
                .append(";\n")
                .append(bodyLabel)
                .append(":\n");

        visit(bodyNode, typecode);

        ollirCode.append(conLabel)
                .append(":\n");

        String condition = visit(conditionNode, typecode);

        ollirCode.append("if (")
                .append(condition)
                .append(") goto ")
                .append(bodyLabel)
                .append(";\n");

        return "";
    }

    private String dealBlockStatement(JmmNode jmmNode, String typecode) {
        for (JmmNode child: jmmNode.getChildren()) {
            visit(child, typecode);
        }
        return typecode;
    }


    public String getCode() {
        return ollirCode.toString();
    }
}
