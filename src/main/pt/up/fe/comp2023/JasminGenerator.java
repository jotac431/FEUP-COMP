package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.time.format.DecimalStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class JasminGenerator implements JasminBackend {
    String className;
    String superClassName;
    int conditionalsCounter;
    int stackCounter;
    int localsCounter;

    @Override
        public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        String jasminCode = generateClass(ollirClass);

        try {
            String path = "C:/universidade/compiladores";
            File file = new File(path, "resultado" + ".j");
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jasminCode.toString());
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ollirClass.checkMethodLabels();
            ollirClass.buildCFGs();
            ollirClass.buildVarTables();
            System.out.println("\n\n---------\n" + jasminCode + "\n---------\n\n");

            // More reports from this stage
            List<Report> reports = new ArrayList<>();
            return new JasminResult(ollirResult, jasminCode, reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    // Basic class structure (including constructor <init>)
    private String generateClass(ClassUnit classUnit){
        superClassName = classUnit.getSuperClass();
        className = classUnit.getClassName();

        StringBuilder classJasmin = new StringBuilder(generateClassHeader(classUnit));
        for (Field field : classUnit.getFields()){
            classJasmin.append("\n").append(generateField(field));
        }


        for (Method method : classUnit.getMethods())
            classJasmin.append("\n").append(generateMethod(method));

        return classJasmin.toString();
    }

    private String generateClassHeader(ClassUnit classUnit){
        StringBuilder classHeader = new StringBuilder(".class ");
        AccessModifiers classAccessModifier = classUnit.getClassAccessModifier();

        if (classAccessModifier == AccessModifiers.DEFAULT)
            classAccessModifier = AccessModifiers.PUBLIC;
        classHeader.append(classAccessModifier.toString().toLowerCase());


        classHeader.append(" ").append(classUnit.getClassName()).append("\n")
                .append(".super ");

        if ( superClassName == null){
            superClassName = "java/lang/Object";
            classHeader.append("java/lang/Object").append("\n");
        }
        else
            classHeader.append(classUnit.getSuperClass()).append("\n");

        return classHeader.toString();
    }


    // Class fields
    private String generateField(Field field){
        StringBuilder fieldResult = new StringBuilder(".field ");
        String fieldName = field.getFieldName();
        Type fieldType = field.getFieldType();
        AccessModifiers fieldAccessModifier = field.getFieldAccessModifier();

        // TODO:
        if (fieldAccessModifier != AccessModifiers.DEFAULT){
            fieldResult.append(fieldAccessModifier.toString().toLowerCase());
        }

        if (field.isStaticField())
            fieldResult.append(" static");
        if (field.isFinalField())
            fieldResult.append(" final");

        fieldResult.append(" ").append(fieldName).append(" ").append(getTypeDescriptor(fieldType));

        if (field.isInitialized()){
            fieldResult.append(" = ").append(field.getInitialValue());
        }

        return fieldResult.append("\n").toString();
    }

    private String getTypeDescriptor(Type type){
        ElementType typeOfElement = type.getTypeOfElement();

        if (typeOfElement == ElementType.ARRAYREF){
            ElementType arrayElementsType = ((ArrayType) type).getTypeOfElements();
            String res = "[" + ((arrayElementsType == ElementType.STRING) ? "Ljava/lang/String;" : "I");
            return res;
        }

        switch (typeOfElement){
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case OBJECTREF:
                String className = ((ClassType) type).getName();
                return "L" + className + ";";
            case VOID:
                return "V";
            default:
                return "; ERROR: Unable to get type descriptor";
        }
    }


    // Method structure (in Jasmin, you can ignore stack and local limits for now, use ‘limit stack 99‘ and ‘limit locals 99‘)
    private String generateMethodHeader(Method method){
        StringBuilder methodHeader = new StringBuilder(".method ");
        Type methodReturn = method.getReturnType();

        if (method.isConstructMethod()){
            methodHeader.append("public");
            methodHeader.append(" <init>");
            methodHeader.append("(");
            for (Element parameter : method.getParams()){
                methodHeader.append(getTypeDescriptor(parameter.getType()));
            }
            methodHeader.append(")").append(getTypeDescriptor(methodReturn)).append("\n");
            return methodHeader.toString();
        }



        methodHeader.append(method.getMethodAccessModifier().toString().toLowerCase());

        if (method.isStaticMethod())
            methodHeader.append(" static");

        methodHeader.append(" ").append(method.getMethodName());


        if (method.isFinalMethod())
            methodHeader.append(" final");

        methodHeader.append("(");
        for (Element parameter : method.getParams()){
            methodHeader.append(getTypeDescriptor(parameter.getType()));
        }
        methodHeader.append(")").append(getTypeDescriptor(methodReturn)).append("\n");

        return methodHeader.toString();
    }

    private String generateMethod(Method method){
        StringBuilder methodJasmin = new StringBuilder(generateMethodHeader(method));
        HashMap<String, Descriptor> varTable = method.getVarTable();
        StringBuilder methodInstructions = new StringBuilder();

        if (!method.isConstructMethod()){
            methodJasmin.append(".limit stack 99\n");
            methodJasmin.append(".limit locals 99\n");
        }
        for (int i = 0; i < method.getInstructions().size(); i++){
            Instruction instruction = method.getInstr(i);
                    methodInstructions.append(generateInstruction(instruction, varTable)).append("\n");
                    if (instruction.getInstType() == InstructionType.CALL){
                        if (((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID){
                            methodInstructions.append("pop\n");
                        }
                    }
        }
        methodJasmin.append(methodInstructions);
        if (method.isConstructMethod()){
            methodJasmin.append("return\n");
        }
        methodJasmin.append(".end method");

        return methodJasmin.toString();
    }

    private String generateInstruction(Instruction instruction, HashMap<String, Descriptor> varTable){
        switch (instruction.getInstType()){
            case ASSIGN: return generateAssignInstruction((AssignInstruction) instruction, varTable);
            case CALL: return generateCallInstruction((CallInstruction) instruction, varTable);
            case RETURN: return generateReturnInstruction((ReturnInstruction) instruction, varTable);
            case PUTFIELD: return generatePutFieldInstruction((PutFieldInstruction) instruction, varTable);
            case GETFIELD: return generateGetFieldInstruction((GetFieldInstruction) instruction, varTable);
            case BINARYOPER: return generateBinaryOperInstruction((BinaryOpInstruction) instruction, varTable);
            case GOTO:return generateGotoInstruction((GotoInstruction)instruction, varTable);
            case BRANCH:return generateBranchInstruction((CondBranchInstruction)instruction, varTable);
            case NOPER: return generateNoperInstruction((SingleOpInstruction) instruction, varTable);
            default: return ";Error: Invalid Instruction " + instruction.getInstType().toString();
        }
    }

    private String generateBranchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable){
        stackCounter--;
        return "ifeq " + instruction.getLabel() + "\n";
    }

    private String generateGotoInstruction(GotoInstruction instruction, HashMap<String, Descriptor> varTable){
        return String.format("goto %s\n", instruction.getLabel());
    }


    private String generateAssignInstruction(AssignInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder jasminAssign = new StringBuilder();
        Operand operand = (Operand) instruction.getDest();
        Descriptor descriptor = varTable.get(operand.getName());
        ElementType operandType = operand.getType().getTypeOfElement();
        int register = descriptor.getVirtualReg();

        if (descriptor.getVarType().getTypeOfElement() == ElementType.ARRAYREF && operandType != ElementType.ARRAYREF){
            ArrayOperand arrayOperand = (ArrayOperand) operand;
            Element indexOperand = arrayOperand.getIndexOperands().get(0);
            jasminAssign.append(loadDescriptor(descriptor)).append(loadElement(indexOperand, varTable));
        }

        jasminAssign.append(generateInstruction(instruction.getRhs(), varTable));

        if (operandType == ElementType.INT32 || operandType == ElementType.BOOLEAN){
            if (descriptor.getVarType().getTypeOfElement() == ElementType.ARRAYREF){
                jasminAssign.append("iastore\n");
                return jasminAssign.toString();
            }else{
                jasminAssign.append("istore");
            }
        }else{
            jasminAssign.append("astore");
        }

        jasminAssign.append((register <= 3) ? "_" : " ").append(register);

        return jasminAssign.toString();
    }

    private String generateCallInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder jasminCall = new StringBuilder();
        ElementType typeOfElement = instruction.getReturnType().getTypeOfElement();

        switch (instruction.getInvocationType()) {
            case invokevirtual:
                jasminCall.append(loadElement(instruction.getFirstArg(), varTable));
                for (Element element : instruction.getListOfOperands())
                    jasminCall.append(loadElement(element, varTable));
                jasminCall.append("invokevirtual ")
                        .append(getObjectName(((ClassType) instruction.getFirstArg().getType()).getName()))
                        .append("/").append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for (Element element : instruction.getListOfOperands())
                    jasminCall.append(getTypeDescriptor(element.getType()));

                jasminCall.append(")").append(getTypeDescriptor(instruction.getReturnType())).append("\n");
                return jasminCall.toString();

            case invokespecial:
                jasminCall.append(loadElement(instruction.getFirstArg(), varTable));

                jasminCall.append("invokespecial ")
                        .append((instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) ? superClassName : className)
                        .append("/<init>(");

                for (Element element : instruction.getListOfOperands())
                    jasminCall.append(getTypeDescriptor(element.getType()));
                jasminCall.append(")").append(getTypeDescriptor(instruction.getReturnType()));
                return jasminCall.toString();
            case invokestatic:
                for (Element element : instruction.getListOfOperands())
                    jasminCall.append(loadElement(element, varTable));

                jasminCall.append("invokestatic ")
                        .append(getObjectName(((Operand) instruction.getFirstArg()).getName()))
                        .append("/").append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for (Element e : instruction.getListOfOperands())
                    jasminCall.append(getTypeDescriptor(e.getType()));

                jasminCall.append(")").append(getTypeDescriptor(instruction.getReturnType()));
                return jasminCall.toString();

            case NEW:
                if (typeOfElement == ElementType.OBJECTREF) {
                    for (Element element : instruction.getListOfOperands()) {
                        jasminCall.append(loadElement(element, varTable));
                    }
                    jasminCall.append("new ")
                            .append(((Operand) instruction.getFirstArg()).getName()).append("\ndup\n");
                } else if (typeOfElement == ElementType.ARRAYREF) {
                    for (Element element : instruction.getListOfOperands()) {
                        jasminCall.append(loadElement(element, varTable));
                    }
                    jasminCall.append("newarray int\n");
                } else
                    jasminCall.append("; Error in instruction NEW\n");
                return jasminCall.toString();
            case ldc:
                return loadElement(instruction.getFirstArg(), varTable);
            case arraylength:
                return loadElement(instruction.getFirstArg(), varTable) + "arraylength\n";
            default:
                return "; ERROR: Instruction CALL not implemented\n";
        }
    }

    private String generateReturnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable){
        if (!instruction.hasReturnValue())
            return "return";

        ElementType returnType =  instruction.getOperand().getType().getTypeOfElement();
        String jasminReturn = loadElement(instruction.getOperand(), varTable);

        if (returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN)
            jasminReturn += "ireturn";
        else
            jasminReturn += "areturn";

        return jasminReturn;
    }

    // expression of type --> this.current = "Hello, world!";
    private String generatePutFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        String jasminPutField = loadElement(instruction.getFirstOperand(), varTable);

        jasminPutField += loadElement(instruction.getThirdOperand(), varTable) +  "putfield ";

        jasminPutField += getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() + " ";

        jasminPutField += getTypeDescriptor(instruction.getSecondOperand().getType()) + "\n";

        return jasminPutField;
    }

    // expression of type --> String message = this.current;
    private String generateGetFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        String jasminGetField = loadElement(instruction.getFirstOperand(), varTable) +
                "getfield ";

        jasminGetField += getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() + " ";

        jasminGetField += getTypeDescriptor(instruction.getFieldType()) + "\n";

        return jasminGetField;
    }

    private String getObjectName(String name) {
        if (name.equals("this"))
            return className;
        return name;
    }


    private String generateBinaryOperInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        OperationType typeOfOperation = instruction.getOperation().getOpType();


        /*if (typeOfOperation == OperationType.ANDB || typeOfOperation == OperationType.LTH) {
            return getBooleanOperation(instruction, varTable);
        }*/

        return loadElement(instruction.getLeftOperand(), varTable)
                + loadElement(instruction.getRightOperand(), varTable)
                    + getTypeOfOperation(instruction.getOperation()) + "\n";
    }

    private String getBooleanOperation(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        OperationType typeOfOperation = instruction.getOperation().getOpType();
        StringBuilder jasminBoolean = new StringBuilder();

        switch (typeOfOperation){
            case LTH: {
                conditionalsCounter++;
                jasminBoolean.append(loadElement(instruction.getLeftOperand(), varTable)).
                        append(loadElement(instruction.getRightOperand(), varTable)).
                        append("if_icmpge ").append("jasminTrue").append(conditionalsCounter).
                        append("iconst_1\n").append("goto ").
                        append("jasminEndif").append(conditionalsCounter).append("\n").
                        append("jasminTrue").append(conditionalsCounter).append(":\n").
                        append("iconst_0\n").append("jasminEndif").append(":\n");
                return jasminBoolean.toString();
            }
            case ANDB: {
                conditionalsCounter++;
                // left operand
                jasminBoolean.append(loadElement(instruction.getLeftOperand(), varTable)).
                        append("ifeq jasminTrue").append(conditionalsCounter).append("\n");

                jasminBoolean.append(loadElement(instruction.getRightOperand(), varTable)).
                        append("ifeq jasminTrue").append(conditionalsCounter).append("\n");


                jasminBoolean.append("iconst_1\n").append("goto ").
                        append("jasminEndif").append(conditionalsCounter).append("\n").
                        append("jasminTrue").append(conditionalsCounter).append(":\n").
                        append("iconst_0\n").append("jasminEndif").append(":\n");
                return jasminBoolean.toString();

            }
            default: {
                return "; ERROR: Instruction not implemented\n";
            }

        }
    }



    private String getTypeOfComparison(Operation operation){
        switch (operation.getOpType()) {
            case GTE:
                return "if_icmpge";
            case LTH:
                return "if_icmplt";
            case EQ:
                return "if_icmpeq";
            case NOTB:
            case NEQ:
                return "if_icmpne";
            default:
                System.out.println(operation.getOpType());
                return "; ERROR: Couldn't find the type of comparison";
        }
    }

    private String getTypeOfOperation(Operation operation){
        switch (operation.getOpType()) {
            case ADD:
                return "iadd";
            case MUL:
                return "imul";
            case SUB:
                return "isub";
            case DIV:
                return "idiv";
            default:
                System.out.println(operation.getOpType());
                return "; ERROR: Couldn't find the type of operation";
        }
    }

    private String generateNoperInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable){
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable){
        if (element.isLiteral()) return loadLiteralElement((LiteralElement) element);

        Descriptor descriptor = varTable.get(((Operand) element).getName());
        if (descriptor == null)
            return "!!!" + ((Operand) element).getName();

        try{
            if (element.getType().getTypeOfElement() != ElementType.ARRAYREF
                    && descriptor.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                ArrayOperand arrayOp = (ArrayOperand) element;
                Element index = arrayOp.getIndexOperands().get(0);
                return loadDescriptor(descriptor) + loadElement(index, varTable) + "iaload\n";
            }
        }catch (NullPointerException | ClassCastException exception){
            System.out.println(((Operand) element).getName());
            System.out.println(descriptor.getVirtualReg() + " " + descriptor.getVarType());
        }

        return loadDescriptor(descriptor);
    }

    private String loadLiteralElement(LiteralElement literalElement){
        String jasminLiteralElement = "";
        int n = Integer.parseInt(literalElement.getLiteral());
        if (literalElement.getType().getTypeOfElement() == ElementType.INT32 || literalElement.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (n <= 5)
                jasminLiteralElement += "iconst_";
            else if (n <= 127)
                jasminLiteralElement += "bipush ";
            else if (n <= 32767)
                jasminLiteralElement += "sipush ";
            else
                jasminLiteralElement += "ldc ";
        } else
            jasminLiteralElement += "ldc ";

        if (n == -1)
            return jasminLiteralElement + "m1\n";

        return jasminLiteralElement + n + "\n";

    }

    private String loadDescriptor(Descriptor descriptor){
        ElementType type = descriptor.getVarType().getTypeOfElement();

        if (type == ElementType.THIS)
            return "aload_0\n";

        int register = descriptor.getVirtualReg();

        if (type == ElementType.INT32 || type == ElementType.BOOLEAN)
            return "iload" + ((register <= 3) ? "_" : " ") + register + "\n";
        else
            return "aload" + ((register <= 3) ? "_" : " ") + register + "\n";
    }

}
