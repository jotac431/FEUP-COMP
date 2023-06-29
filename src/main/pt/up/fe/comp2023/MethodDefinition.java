package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class MethodDefinition {
    private String methodName;
    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> variables;

    public MethodDefinition(String methodName, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.variables = variables;
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return variables;
    }

}
