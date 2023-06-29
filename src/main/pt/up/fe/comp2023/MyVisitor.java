package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyVisitor extends AJmmVisitor<String, String> {

    private String className;
    private String current;
    private final MySymbolTable table;
    public MyVisitor(MySymbolTable symbolTable, String className) {
        this.className = className;
        this.table = symbolTable;
    }

    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("VarDeclaration", this::dealWithField);
        addVisit("Type", this::dealWithType);
        addVisit("MethodDeclaration", this::dealWithMethod);
        this.setDefaultVisit(this::defaultVisitor);
    }

    private String defaultVisitor(JmmNode jmmNode, String s) {
        return "";
    }
    private String dealWithProgram(JmmNode jmmNode, String s) {
        s = (s!= null ?s:"");

        for (JmmNode child : jmmNode.getChildren()){
            s += visit(child,"");
            s += ";\n";
        }
        return s;
    }

    private String dealWithImport(JmmNode jmmNode, String s) {
        var names = (List<String>) jmmNode.getObject("value");
        var impS = String.join(".", names);
        table.addImport(impS);
        return s + "import " + impS;
    }

    private String dealWithClass(JmmNode jmmNode, String s) {
        table.setClassName(jmmNode.get("className"));
        s += "public class " + jmmNode.get("className");
        if (jmmNode.hasAttribute("superClass")){
            table.setSuper(jmmNode.get("superClass"));
            s += " extends " + jmmNode.get("superClass");
        }
        s += " {\n";
        for (JmmNode child : jmmNode.getChildren()){
            s += "\t" + visit(child,"");
            s += ";\n";
        }
        s += "}";
        return s;
    }

    private String dealWithField(JmmNode jmmNode, String s) {
        Symbol sym = getSymbol(jmmNode);
        table.addField(sym);
        for (JmmNode child : jmmNode.getChildren()){
            s += visit(child,"") + " ";
        }
        //s += jmmNode.get("name");
        return s;
    }

    private String dealWithType(JmmNode jmmNode, String s) {
        if (jmmNode.hasAttribute("name")){
            return s + jmmNode.get("name");
        }
        switch (jmmNode.getKind()) {
            case "ArrayType" -> {
                for (JmmNode child : jmmNode.getChildren()) {
                    s += visit(child, "") + "[]";
                }
                return s;
            }
            case "BooleanType" -> {
                return s + "boolean";
            }
            case "IntegerType" -> {
                return s + "int";
            }
            case "CharType" -> {
                return s + "char";
            }
            case "DoubleType" -> {
                return s + "double";
            }
            case "FloatType" -> {
                return s + "float";
            }
            case "ByteType" -> {
                return s + "byte";
            }
            case "ShortType" -> {
                return s + "short";
            }
            case "LongType" -> {
                return s + "long";
            }
            case "VoidType" -> {
                return s + "void";
            }
        }
        return s;
    }

    private String dealWithMethod(JmmNode jmmNode, String s) {
        String methodName;
        if (Objects.equals(jmmNode.getKind(), "MainMethod"))
            methodName = "main";
        else
            methodName = jmmNode.get("methodName");

        Type returnType = getType(jmmNode.getJmmChild(0));
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for (JmmNode child : jmmNode.getChildren()){
            if (child.getKind().equals("Parameter")){
                var names = (List<String>) child.getObject("name");
                int cnt = 0;
                for (JmmNode grandchild : child.getChildren()){
                    parameters.add(new Symbol(getType(grandchild), names.get(cnt)));
                    cnt++;
                }
            } else if (child.getKind().equals("Variable")){
                variables.add(getSymbol(child));
                s += visit(child.getJmmChild(0),"") + " ";
            }
        }

        table.addMethod(methodName, returnType, parameters, variables);
        return s;
    }

    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode.getJmmChild(0));
        return new Symbol(type, name);
    }

    public static Type getType(JmmNode jmmNode) {
        switch (jmmNode.getKind()) {
            case "ArrayType" -> {
                return new Type(getType(jmmNode.getJmmChild(0)).getName(), true);
            }
            case "BooleanType" -> {
                return new Type("boolean", false);
            }
            case "IntegerType" -> {
                return new Type("int", false);
            }
            case "CharType" -> {
                return new Type("char", false);
            }
            case "DoubleType" -> {
                return new Type("double", false);
            }
            case "FloatType" -> {
                return new Type("float", false);
            }
            case "ByteType" -> {
                return new Type("byte", false);
            }
            case "ShortType" -> {
                return new Type("short", false);
            }
            case "LongType" -> {
                return new Type("long", false);
            }
            case "VoidType" -> {
                return new Type("void", false);
            }
        }
        return new Type(jmmNode.get("name"), false);
    }

}
