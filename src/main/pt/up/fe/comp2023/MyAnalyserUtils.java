package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import java.util.List;
import java.util.Objects;


public class MyAnalyserUtils {
    static MySymbolTable symbolTable = new MySymbolTable();

    public MyAnalyserUtils(MySymbolTable symbolTable) {
        MyAnalyserUtils.symbolTable = symbolTable;
    }

    public static Type getType(JmmNode jmmNode) {
        if (Objects.equals(jmmNode.getKind(), "ArrayType") || Objects.equals(jmmNode.getKind(), "NewArrayDeclaration") || Objects.equals(jmmNode.getKind(), "ArrayDeclaration")) {
            return new Type(getType(jmmNode.getJmmChild(0)).getName(), true);
        }
        if (jmmNode.getKind().equals("Integer"))
            return new Type("int", false);
        if (jmmNode.getKind().equals("Boolean"))
            return new Type("boolean", false);
        if (jmmNode.getKind().equals("Void"))
            return new Type("void", false);
        List<Symbol> fields = symbolTable.getFields();
        //System.out.println("the fields are " + fields);
        for (Symbol i : fields) {
            if (jmmNode.get("name").equals(i.getName())) {
                return i.getType();
            }
        }
        try {
            if (jmmNode.getKind().equals("Identifier")) {
                JmmNode methodNode = jmmNode.getJmmParent();
                while (!methodNode.getKind().equals("GeneralMethod") && !methodNode.getKind().equals("MainMethod"))
                    methodNode = methodNode.getJmmParent();
                String parent = methodNode.get("methodName");
                if (symbolTable.getLocalVariables(parent) != null) {
                    List<Symbol> localVariables = symbolTable.getLocalVariables(parent);
                    for (Symbol i : localVariables) {
                        if (jmmNode.get("name").equals(i.getName())) {
                            return i.getType();
                        }
                    }
                }
            }
            JmmNode methodNode = jmmNode;
            while ((!methodNode.hasAttribute("name")))
                methodNode = methodNode.getJmmParent();
            return new Type(methodNode.get("name"), false);
        } catch (Exception e) {
            if(jmmNode.getKind().equals("MethodCall")){return getType(jmmNode.getJmmChild(0));}
            return new Type(jmmNode.get("name"), false);
        }
    }


    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("name");
        Type type = getType(jmmNode.getJmmChild(0));
        return new Symbol(type, name);
    }

    public static String getMethodSignature(String methodName, List<Symbol> parameters) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(methodName);
        for (Symbol parameter: parameters) {
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(parameter.getType().print());
        }

        return methodSignatureBuilder.toString();
    }

    public static String getMethodSymbolName(String methodSignature) {
        if (methodSignature.contains("#")) {
            return methodSignature.replaceFirst("#", "(")
                    .replaceAll("##UNKNOWN", ",?")
                    .replaceAll("\\(#UNKNOWN", "(?")
                    .replaceAll("#", ",")
                    .concat(")");
        }
        return  methodSignature.concat("()");
    }
}
