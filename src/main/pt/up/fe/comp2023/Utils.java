package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Objects;

public class Utils {

    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("varName");
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
