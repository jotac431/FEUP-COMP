package pt.up.fe.comp2023;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static pt.up.fe.comp2023.MyAnalyserUtils.getType;
import static pt.up.fe.comp2023.MyReportUtils.ambiguousMethodCallReport;

public class MyAnalyserVisitor extends PostorderJmmVisitor<SymbolTable, List<Report>> {
    //static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    private static List<String> PRIMITIVES = new ArrayList<>(Arrays.asList("int", "void", "boolean", "String"));
    private List<Report> reports = new ArrayList<>();
    private MyAnalyserUtils myAnalyserUtils;

    public MyAnalyserVisitor(MySymbolTable symbolTable){
        PRIMITIVES.add(symbolTable.getClassName());
        PRIMITIVES.addAll(symbolTable.getImports());
        myAnalyserUtils = new MyAnalyserUtils(symbolTable);
    }


    @Override
    protected void buildVisitor() {
        addVisit("EntireProgram", this::dealWithProgram);
        addVisit("Class", this::dealWithClassDeclaration);
        addVisit("Type", this::dealWithType);
        addVisit("BinaryOpCompare", this::dealWithBinaryOpCompare);
        addVisit("BinaryOpLogical", this::dealWithBinaryOpLogical);
        addVisit("BinaryOpArithmetic", this::dealWithBinaryOpArithmetic);
        addVisit("UnaryOp", this::dealWithUnaryOp);
        addVisit("WhileStatement", this::checkCondition);
        addVisit("IfStatement", this::checkCondition);
        addVisit("IfElseStatement", this::checkCondition);
        addVisit("GeneralMethod", this::dealWithMethod);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("NewArrayDeclaration", this::dealWithNewArrayDeclaration);
        addVisit("ArrayDeclaration", this::dealWithNewArrayDeclaration);

        setDefaultValue(Collections::emptyList);
        setReduceSimple(this::reduceReports);
    }

    private List<Report> reduceReports(List<Report> reports1, List<Report> reports2) {
        return SpecsCollections.concatList(reports1, reports2);
    }

    private List<Report> dealWithProgram(JmmNode jmmNode, SymbolTable symbolTable) {
        return reports;
    }

    private List<Report> dealWithClassDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {

        if (!jmmNode.getAttributes().contains("superClass")) {
            return reports;
        }

        String extendsName = jmmNode.get("superClass");
        String className = jmmNode.get("className");
        if (className.equals(extendsName)) {
            reports.add(MyReportUtils.cyclicInheritance(jmmNode, extendsName));
            return reports;
        }
        if (symbolTable.getImports().contains(extendsName))
            return reports;

        reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, extendsName));

        return reports;
    }

    private List<Report> dealWithType(JmmNode jmmNode, SymbolTable symbolTable) {
        //List<Report> reports = new ArrayList<>();

        Type objType = getType(jmmNode);
        if (objType.isArray()) {
            return dealWithType(jmmNode.getJmmChild(0), symbolTable);
        }
        if (PRIMITIVES.contains(objType.getName()) || symbolTable.getImports().contains(objType.getName())) {
            return reports;
        }
        reports.add(MyReportUtils.cannotFindTypeReport(jmmNode, jmmNode.get("name")));
        return reports;
    }

    private boolean typeIsCompatibleWith(Type type1, Type type2, SymbolTable symbolTable) {
        String sup = "";
        try{
            sup = symbolTable.getSuper();
        }
        catch (Exception e){
        }
        if (type1.equals(type2)) return true;
        if (type1.getName().equals("#ASSUME")) return true;
        if (type2.getName().equals("#ASSUME")) return true;
        if (type1.getName().equals("boolean") && (type2.getName().equals("false") || (type2.getName().equals("true")))) return true; //I know... I know...
        if (type1.isArray() != type2.isArray()) return false;
        if (type1.isArray() && type2.isArray() && type1.getName().equals(type2.getName())) return false;
        if ((symbolTable.getImports().contains(type1.getName()) &&
                symbolTable.getImports().contains(type2.getName()))) return true;
        if ((symbolTable.getImports().contains(type1.getName()) ||
                symbolTable.getImports().contains(type2.getName())) && sup != "" && sup != "null" && sup != null) return true;
        if (symbolTable.getClassName().equals(type1.getName())) return true;
        if (PRIMITIVES.contains(type1.getName()) || PRIMITIVES.contains(type2.getName())) return false;
        if (type2.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null) return false;
        if (symbolTable.getSuper() == null) return true;
        return !(type1.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper().equals(type2.getName()));
    }

    private List<Report> dealWithBinaryOpCompare(JmmNode jmmNode, SymbolTable symbolTable) {
        //List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (lhsType.getName().equals("#ASSUME") || rhsType.getName().equals("#ASSUME")) {
            putAssumeType(jmmNode);
            return reports;
        }


        if (!lhsType.equals(rhsType)) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (lhsType.isArray()) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!lhsType.getName().equals("int")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!PRIMITIVES.contains(lhsType.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }
        putType(jmmNode, new Type("boolean", false));

        return reports;
    }

    private List<Report> dealWithBinaryOpLogical(JmmNode jmmNode, SymbolTable symbolTable) {
        //List<Report> reports = new ArrayList<>();
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (lhsType.getName().equals("#ASSUME") || rhsType.getName().equals("#ASSUME")) {
            putAssumeType(jmmNode);
            return reports;
        }

        if (!lhsType.equals(rhsType)) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (lhsType.isArray()) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!lhsType.getName().equals("boolean")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!PRIMITIVES.contains(lhsType.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }
        putType(jmmNode, new Type("boolean", false));
        //putType(jmmNode, lhsType); TODO check which version is correct

        return reports;
    }

    private List<Report> dealWithBinaryOpArithmetic(JmmNode jmmNode, SymbolTable symbolTable) {
        Type lhsType = getType(jmmNode.getJmmChild(0));
        Type rhsType = getType(jmmNode.getJmmChild(1));

        if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!lhsType.equals(rhsType)) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (lhsType.isArray() || rhsType.isArray()) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!lhsType.getName().equals("int")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }

        if (!PRIMITIVES.contains(lhsType.getName())) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
            return reports;
        }
        jmmNode.put("name", lhsType.getName());

        return reports;
    }

    private List<Report> dealWithUnaryOp(JmmNode jmmNode, SymbolTable symbolTable) {
        //List<Report> reports = new ArrayList<>();
        Type type = getType(jmmNode.getJmmChild(0));

        if (type.getName().equals("#UKNOWN")) {
            putUnknownType(jmmNode);
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, type.print(), "boolean"));
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
    private List<Report> dealWithReturn(JmmNode jmmNode, SymbolTable symbolTable){
            String methodSignature = jmmNode.getJmmParent().getJmmParent().get("signature");
            Type returnType = symbolTable.getReturnType(methodSignature);
            Type expressionType = getType(jmmNode.getJmmChild(0));

            if (returnType.getName().equals("#UNKNOWN") || expressionType.getName().equals("#UNKNOWN")) {
                reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), expressionType.print(), returnType.print()));
                return reports;
            }

            if (!typeIsCompatibleWith(returnType, expressionType, symbolTable)) {
                reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), expressionType.print(), returnType.print()));
            }

            return reports;
        }
    private List<Report> dealWithMainMethod(JmmNode jmmNode, SymbolTable symbolTable) {
      return reports;
    }
    private List<Report> dealWithMethod(JmmNode jmmNode, SymbolTable symbolTable) {
        if (!MyAnalyserUtils.getType(jmmNode.getJmmChild(0)).getName().equals(symbolTable.getClassName())) {
            String methodSignature = inferMethodSignature(jmmNode);
            boolean methodExists = symbolTable.getMethods().contains(jmmNode.get("methodName"));

            if (methodExists) {
                String symbolName = myAnalyserUtils.getMethodSymbolName(methodSignature);
                //reports.add(MyReportUtils.nonStaticInStaticContext(jmmNode, "method", symbolName));
            } else if (symbolTable.getSuper() == null && symbolTable.getImports().isEmpty()) {
                String symbolName = myAnalyserUtils.getMethodSymbolName(methodSignature);
                reports.add(MyReportUtils.cannotFindSymbolReport(jmmNode, symbolName));
            }
        }
        putAssumeType(jmmNode);
        if(jmmNode.getKind().equals("GeneralMethod")){
            JmmNode returnNode = jmmNode.getJmmChild(jmmNode.getNumChildren() - 1);
            if(!typeIsCompatibleWith(getType(returnNode),symbolTable.getReturnType(jmmNode.get("methodName")), symbolTable)) {
                reports.add(MyReportUtils.incompatibleTypesReport(returnNode,
                        getType(returnNode).getName(), symbolTable.getReturnType(jmmNode.get("methodName")).getName()));
            }
        }
        return reports;
    }

    private List<Report> dealWithMethodCall(JmmNode jmmNode, SymbolTable symbolTable) {
            String methodName = jmmNode.get("methodName");
            List<String> imports = symbolTable.getImports();
            String mainClass = symbolTable.getClassName();
            String extension = symbolTable.getSuper();
            List<String> methods = symbolTable.getMethods();
        if(reports.size() > 0)
                return reports;
            if((getType(jmmNode.getJmmChild(0)).getName().equals(mainClass) && !isNull(extension)) ||
                    imports.contains(getType(jmmNode.getJmmChild(0)).getName()) ||
                    getType(jmmNode.getJmmChild(0)).getName().equals("#ASSUME") ||
                    (jmmNode.getJmmChild(0).hasAttribute("value") && imports.contains(jmmNode.getJmmChild(0).get("value")))){
                return reports;
            }
            if(methods.contains(methodName) && getType(jmmNode.getJmmChild(0)).getName().equals(mainClass)){
                if(symbolTable.getParameters(methodName).size() != jmmNode.getNumChildren() - 1){
                    reports.add(ambiguousMethodCallReport(jmmNode, methodName));
                    return reports;
                }
                int i = 1;
                for (Symbol param : symbolTable.getParameters(methodName)) {
                    visit(jmmNode.getJmmChild(i));
                    if(reports.size() > 0)
                        break;
                    if(!param.getType().getName().equals(getType(jmmNode.getJmmChild(i)).getName()) ||
                            param.getType().isArray() != getType(jmmNode.getJmmChild(i)).isArray()){
                        reports.add(ambiguousMethodCallReport(jmmNode, methodName));
                        break;
                    }
                    i++;
                }
            } else if(!methods.contains(methodName)){
                reports.add(ambiguousMethodCallReport(jmmNode, methodName));
            } else if(!getType(jmmNode.getJmmChild(0)).getName().equals(mainClass))
                reports.add(ambiguousMethodCallReport(jmmNode, methodName));
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
        methodSignatureBuilder.append(jmmNode.get("methodName"));
        if(!jmmNode.getJmmChild(0).getKind().equals("IDStringType")){
        for (JmmNode argument: jmmNode.getJmmChild(0).getChildren()) {
            Type argType = getType(argument);
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
            }
        }
        else{
            Type argType = getType(jmmNode.getJmmChild(0));
            methodSignatureBuilder.append("#");
            methodSignatureBuilder.append(argType.print());
        }
        return methodSignatureBuilder.toString();
    }

    private List<Report> checkCondition(JmmNode jmmNode, SymbolTable symbolTable) {
        Type conditionType = getType(jmmNode.getJmmChild(0));

        if (conditionType.getName().equals("boolean")) {
            return reports;
        }

        if (conditionType.isArray() || !conditionType.getName().equals("boolean")) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode.getJmmChild(0), conditionType.print(), "boolean"));
        }

        return reports;
    }

    private List<Report> dealWithAssignment(JmmNode jmmNode, SymbolTable symbolTable) {
        String parent = jmmNode.getJmmParent().get("methodName");
        List<Symbol> localVariables = symbolTable.getLocalVariables(parent);
        Type lhsType = null;
        Type rhsType;
        for(Symbol i : localVariables){
            if(jmmNode.get("var").equals(i.getName())){
                jmmNode.put("name", i.getType().getName());
                jmmNode.put("isArray", String.valueOf(i.getType().isArray()));
                lhsType = i.getType();
                break;
            }
        }
        rhsType = getType(jmmNode.getJmmChild(0));
        System.out.println(rhsType);
        System.out.println(jmmNode.getJmmChild(0).getKind());

        if (lhsType.getName().equals("#ASSUME")) {
            return reports;
        }
        if (rhsType.getName().equals("#ASSUME")) {
            putType(jmmNode.getJmmChild(1), lhsType);
            return reports;
        }

        if (!typeIsCompatibleWith(lhsType, rhsType, symbolTable)) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, rhsType.print(), lhsType.print()));
        }

        return reports;
    }
    private List<Report> dealWithArrayAccess(JmmNode jmmNode, SymbolTable symbolTable){
        Type arrayType = getType(jmmNode.getJmmChild(0));
        Type indexType = getType(jmmNode.getJmmChild(1));

        if(!arrayType.isArray()){
            reports.add(MyReportUtils.arrayRequiredReport(jmmNode, arrayType.print()));
        }
        if(!indexType.getName().equals("int") && !indexType.getName().equals("#ASSUME")) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, indexType.print(), "int"));
        }
        return reports;
    }

    private List<Report> dealWithNewArrayDeclaration(JmmNode jmmNode, SymbolTable symbolTable) {
        Type type = MyAnalyserUtils.getType(jmmNode.getJmmChild(0));
        if (!type.getName().equals("#ASSUME") && !(type.getName().equals("int") && !type.isArray())) {
            reports.add(MyReportUtils.incompatibleTypesReport(jmmNode, type.print(), "int"));
        }

        putType(jmmNode, new Type("int", true));

        return reports;
    }
    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("name", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("name", "#UNKNOWN");
    }
    private void putAssumeType(JmmNode jmmNode) {
        jmmNode.put("name", "#ASSUME");
    }
}
