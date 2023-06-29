package pt.up.fe.comp2023.Ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        String s = "";

        if (type.isArray()) {
            s += "array.";
        }
        return s += getOllirType(type.getName());
    }

    public static String getCode(Type type, Boolean indexed) {
        StringBuilder code = new StringBuilder();

        if (type.isArray() && !indexed) {
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getTempCode(String value, Type type) {
        return "t" + value + "." + getCode(type);
    }

    public static String getTempCode(String value, String type) {
        return "t" + value + "." + getOllirType(type);
    }

    public static String getTempCode(String value, String typecode, int check) {
        return "t" + value + "." + typecode;
    }

    public static String getTempCodeIndexed(String value, Type type) {
        return "temp_" + value + "." + getCode(type, true);
    }

    public static String getOllirType(String type) {

        switch (type) {
            case "void": return "V";
            case "int": return "i32";
            case "boolean": return "bool";
            default: return type;
        }
    }

    public static Boolean isField(String var, SymbolTable symbolTable) {
        for (Symbol field: symbolTable.getFields()) {
            if(field.getName().equals(var))
                return true;
        }
        return false;
    }
}
