
package pt.up.fe.comp2023;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;
import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

public class ReferenceVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>> {
    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("+", "-", "*", "/");
    static final List<String> COMPARISON_OP = List.of("<");
    static final List<String> LOGICAL_OP = List.of("&&");


    public ReferenceVisitor() {
    }
    @Override
    protected void buildVisitor() {
        addVisit("program", this::visitProgram);
        addVisit("classDeclaration", this::visitClassDeclaration);
        addVisit("type", this::visitType); //TODO
        addVisit("ReturnStatement", this::visitReturn); //TODO
        addVisit("IfStatement", this::visitCondition); //TODO
        addVisit("WhileStatement", this::visitCondition); //TODO
        addVisit("Assignment", this::visitAssignment); //TODO
        addVisit("BinaryOp", this::visitBinaryOp); //TODO
        addVisit("NotExpression", this::visitNot); //TODO
        addVisit("NewIntArray", this::visitNewIntArray); //TODO
        addVisit("MethodCall", this::visitMethodCall); //TODO

        setDefaultValue(Collections::emptyList);
        setReduceSimple(this::reduceReports);
    }

    private List<Report> reduceReports(List<Report> reports1, List<Report> reports2) {
        return SpecsCollections.concatList(reports1, reports2);
    }
    private List<Report> visitProgram(JmmNode jmmNode, SymbolTable symbolTable){
        List<Report> reports = new ArrayList<>();
        return reports;
    }
    private List<Report> visitClassDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (!jmmNode.getAttributes().contains("extends")) {
            return reports;
        }

        String extendsName = jmmNode.get("extends");

        if (jmmNode.get("className").equals(extendsName)) {
            reports.add(MyReportUtils.cyclicInheritance(jmmNode, extendsName));
            return reports;
        }

        boolean foundImport = symbolTable
                .getImports()
                .stream()
                .anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(extendsName));

        if (foundImport) {
            return reports;
        }

        reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, extendsName));

        return reports;
    }

    private List<Report> visitIdentifier(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        String identifier = jmmNode.get("name");

        String methodSignature = jmmNode
                .getAncestor("GeneralMethod")
                .or(() -> jmmNode.getAncestor("MainMethod"))
                .get()
                .get("signature");

        Optional<Symbol> parameterSymbol = symbolTable
                .getParameters(methodSignature)
                .stream()
                .filter(p -> p.getName().equals(identifier))
                .findFirst();

        if (parameterSymbol.isPresent()) {
            putType(jmmNode, parameterSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> localVarSymbol = symbolTable
                .getLocalVariables(methodSignature)
                .stream()
                .filter(l -> l.getName().equals(identifier))
                .findFirst();

        if (localVarSymbol.isPresent()) {
            putType(jmmNode, localVarSymbol.get().getType());
            return reports;
        }

        Optional<Symbol> fieldSymbol = symbolTable
                .getFields()
                .stream()
                .filter(f -> f.getName().equals(identifier))
                .findFirst();

        if (fieldSymbol.isPresent()) {
            if (jmmNode.getAncestor("MainMethodDef").isPresent()) {
                putUnknownType(jmmNode);
                reports.add(MyReportUtils.nonStaticInStaticContext(jmmNode, "variable", "this"));
                return reports;
            }

            putType(jmmNode, fieldSymbol.get().getType());
            return reports;
        }

        if (!jmmNode.getJmmParent().getKind().equals("MethodCall")
                || (
                !identifier.equals("String") &&
                        !symbolTable.getClassName().equals(identifier) &&
                        !symbolTable.getImports().stream().anyMatch(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(identifier)))
        ) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, identifier));
        }

        return reports;
    }

    private List<Report> visitThisKeyword(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        if (jmmNode.getAncestor("MethodDef").isEmpty()) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.nonStaticInStaticContext(jmmNode, "variable", "this"));
            return reports;
        }

        putType(jmmNode, new Type(symbolTable.getClassName(), false));
        return reports;
    }

    private List<Report> visitMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        // Static method call nodes have no attribute "type" so that may be used to check if the method call is static
        if (!jmmNode.getJmmChild(0).getAttributes().contains("type")) {
            if (jmmNode.getJmmChild(0).get("name").equals(symbolTable.getClassName())) {
                String methodSignature = inferMethodSignature(jmmNode);
                boolean methodExists = symbolTable
                        .getMethods()
                        .stream()
                        .anyMatch(m -> m.substring(0, m.indexOf("#")).equals(jmmNode.get("methodname")));

                if (methodExists) {
                    String symbolName = MyAnalyserUtils.getMethodSymbolName(methodSignature);
                    reports.add(MyReportUtils.nonStaticInStaticContext(jmmNode, "method", symbolName));
                } else if (symbolTable.getSuper() == null) {
                    String symbolName = MyAnalyserUtils.getMethodSymbolName(methodSignature);
                    reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, symbolName));
                }
            }
            putUnknownType(jmmNode);
            return reports;
        }

        Type operand = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        if (PRIMITIVES.contains(operand.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.cannotBeDereferencedReport(jmmNode, operand.getName()));
            return reports;
        }

        if (operand.getName().equals("#UNKNOWN") || operand.isArray() || !symbolTable.getClassName().equals(operand.getName())) {
            putUnknownType(jmmNode);
            return reports;
        }

        String methodSignature = inferMethodSignature(jmmNode);
        Optional<String> foundMethodSignature;
        if (symbolTable.getMethods().contains(methodSignature)) {
            foundMethodSignature = Optional.of(methodSignature);
        } else {
            List<String> foundMethodSignatures = symbolTable
                    .getMethods()
                    .stream()
                    .filter(m -> signatureIsCompatibleWith(m, methodSignature, symbolTable))
                    .collect(Collectors.toList());
            if (foundMethodSignatures.size() > 1) {
                reports.add(MyReportUtils.ambiguousMethodCallReport(jmmNode, jmmNode.get("methodName")));
                putUnknownType(jmmNode);
                return reports;
            } else {
                foundMethodSignature = foundMethodSignatures.stream().findAny();
            }
        }

        if (foundMethodSignature.isPresent()) {
            Type returnType = symbolTable.getReturnType(foundMethodSignature.get());
            putType(jmmNode, returnType);
        } else {
            boolean incompleteSignature = methodSignature.contains("##UNKNOWN");
            boolean methodLooselyExists = incompleteSignature &&
                    symbolTable
                            .getMethods()
                            .stream()
                            .anyMatch(m -> m.split("#")[0].equals(jmmNode.get("methodName")));
            if (!methodLooselyExists && symbolTable.getSuper() == null) {
                String symbolName = MyAnalyserUtils.getMethodSymbolName(methodSignature);
                reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, symbolName));
            }
            putUnknownType(jmmNode);
        }

        return reports;
    }

    private boolean signatureIsCompatibleWith(String sig1, String sig2, SymbolTable symbolTable) {
        List<String> m1 = Arrays.asList(sig1.split("(?<!#)#"));
        List<String> m2 = Arrays.asList(sig2.split("(?<!#)#"));

        if (!m1.get(0).equals(m2.get(0)) || m1.size() != m2.size()) return false;
        m1 = m1.subList(1, m1.size());
        m2 = m2.subList(1, m2.size());

        for (int i = 0; i < m1.size(); i++) {
            if (m1.get(i).equals("#UNKNOWN") || m2.get(i).equals("#UNKNOWN")) continue;
            Type t1 = new Type(m1.get(i).split("\\[]")[0], m1.get(i).endsWith("[]"));
            Type t2 = new Type(m2.get(i).split("\\[]")[0], m2.get(i).endsWith("[]"));
            if (!typeIsCompatibleWith(t1, t2, symbolTable)) return false;
        }

        return true;
    }

    private String inferMethodSignature(JmmNode jmmNode) {
        StringBuilder methodSignatureBuilder = new StringBuilder();
        methodSignatureBuilder.append(jmmNode.get("methodname"));
        for (JmmNode argument: jmmNode.getJmmChild(1).getChildren()) {
            Type argType = MyAnalyserUtils.getType(argument);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }
        return methodSignatureBuilder.toString();
    }

    private List<Report> visitLengthCall(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("#UNKNOWN") && !type.isArray()) {
            reports.add(MyReportUtils.arrayRequiredReport(jmmNode, type.print()));
        }

        putType(jmmNode, new Type("int", false));

        return reports;
    }

    private List<Report> visitIndexing(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type operandType = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        Type indexType = MyAnalyserUtils.getType(jmmNode.getJmmChild(1));

        if (!indexType.getName().equals("#UNKNOWN") && (!indexType.getName().equals("int") || indexType.isArray())) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(1), indexType.print(), "int"));
        }

        if (operandType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
        } else if (operandType.isArray()) {
            putType(jmmNode, new Type(operandType.getName(), false));
        } else {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.arrayRequiredReport(jmmNode.getJmmChild(0), operandType.print()));
        }

        return reports;
    }

    private List<Report> visitNewObject(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = MyAnalyserUtils.getType(jmmNode);
        if (objType.isArray() || PRIMITIVES.contains(objType.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, "new", objType.print()));
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> visitNewIntArray(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type type = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("#UNKNOWN") && !(type.getName().equals("int") && !type.isArray())) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, type.print(), "int"));
        }

        putType(jmmNode, new Type("int", true));

        return reports;
    }

    private List<Report> visitNot(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type type = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));

        if (type.getName().equals("#UKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (type.isArray() || !type.getName().equals("boolean")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode.getJmmChild(0), "!", type.print(), "boolean"));
            return reports;
        }

        putType(jmmNode, new Type("boolean", false));

        return reports;
    }

    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        Type rhsType = MyAnalyserUtils.getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            return reports;
        }

        if (!lhsType.equals(rhsType)) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (ARITHMETIC_OP.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
            if (lhsType.isArray()) {
                putUnknownType(jmmNode);
                reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        }

        if (ARITHMETIC_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("int")) {
                putUnknownType(jmmNode);
                reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        }

        if (LOGICAL_OP.contains(jmmNode.get("op"))) {
            if (!lhsType.getName().equals("boolean")) {
                putUnknownType(jmmNode);
                reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                return reports;
            }
        }

        if (!PRIMITIVES.contains(lhsType.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (COMPARISON_OP.contains(jmmNode.get("op"))) {
            putType(jmmNode, new Type("boolean", false));
        } else {
            putType(jmmNode, lhsType);
        }

        return reports;
    }

    private List<Report> visitAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type lhsType = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        Type rhsType = MyAnalyserUtils.getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN")) {
            return reports;
        }
        if (rhsType.getName().equals("#UNKNOWN")) {
            putType(jmmNode.getJmmChild(1), lhsType);
            return reports;
        }

        if (!typeIsCompatibleWith(lhsType, rhsType, symbolTable)) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, rhsType.print(), lhsType.print()));
        }

        return reports;
    }

    private List<Report> visitCondition(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        Type conditionType = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));

        if (conditionType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (conditionType.isArray() || !conditionType.getName().equals("boolean")) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), conditionType.print(), "boolean"));
        }

        return reports;
    }

    private List<Report> visitReturn(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();
        String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
        Type returnType = symbolTable.getReturnType(methodSignature);
        Type expressionType = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));

        if (returnType.getName().equals("#UNKNOWN") || expressionType.getName().equals("#UNKNOWN")) {
            return reports;
        }

        if (!typeIsCompatibleWith(returnType, expressionType, symbolTable)) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), expressionType.print(), returnType.print()));
        }

        return reports;
    }

    private List<Report> visitType(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = MyAnalyserUtils.getType(jmmNode);
        if (PRIMITIVES.contains(objType.getName())) {
            return reports;
        }

        return checkTypeImport(jmmNode, symbolTable);
    }

    private List<Report> checkTypeImport(JmmNode jmmNode, SymbolTable symbolTable) {
        List<Report> reports = new ArrayList<>();

        Type objType = MyAnalyserUtils.getType(jmmNode);

        if (symbolTable.getClassName().equals(objType.getName()) || objType.getName().equals("String")) {
            return reports;
        }

        Optional<String> qualifiedName = symbolTable
                .getImports()
                .stream()
                .filter(imp -> imp.substring(imp.lastIndexOf(".") + 1).equals(objType.getName()))
                .findAny();

        if (qualifiedName.isPresent()) {
            return reports;
        }

        putUnknownType(jmmNode);
        reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, objType.print()));

        return reports;
    }

    private boolean typeIsCompatibleWith(Type type1, Type type2, SymbolTable symbolTable) {
        if (type1.equals(type2)) return true;
        if (type1.isArray() != type2.isArray()) return false;
        if (PRIMITIVES.contains(type1.getName()) || PRIMITIVES.contains(type2.getName())) return false;
        if (type2.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null) return false;
        if (symbolTable.getSuper() == null) return true;
        return !(type1.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper().equals(type2.getName()));
    }

    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("type", "#UNKNOWN");
    }
}
