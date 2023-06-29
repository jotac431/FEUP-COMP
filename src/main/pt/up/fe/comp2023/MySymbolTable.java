package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySymbolTable implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private String className = null;
    private String superClass = null;
    private List<Symbol> fields = new ArrayList<Symbol>();
    private final Map<String, MethodDefinition> methods = new HashMap<>();

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importDeclaration) {
        imports.add(importDeclaration);
    }
    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String name){
        this.className = name;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    public void setSuper(String name){
        this.superClass = name;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void addField(Symbol field){
        fields.add(field);
    }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(methods.keySet());
    }

    public void addMethod(String methodName, Type returnType, List<Symbol> parameters, List<Symbol> variables) {
        methods.put(methodName, new MethodDefinition(methodName, returnType, parameters, variables));
    }

    @Override
    public Type getReturnType(String methodName) {
        return methods.get(methodName).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methods.get(methodName).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methods.get(methodName).getLocalVariables();
    }
}
