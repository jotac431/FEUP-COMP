package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
public class MyReportUtils {
    private static Report baseReport(JmmNode at, ReportType type, Stage stage, String message) {
        return new Report(type, stage, Integer.parseInt(at.get("lineStart")), Integer.parseInt(at.get("colStart")), message);
    }

    public static Report cyclicInheritance(JmmNode at, String type) {
        StringBuilder message = new StringBuilder();
        message.append("cyclic inheritance involving ");
        message.append(type);
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report nonStaticInStaticContext(JmmNode at, String entity, String type) {
        StringBuilder message = new StringBuilder();
        message.append("non-static ");
        message.append(entity);
        message.append(" ");
        message.append(type);
        message.append(" cannot be referenced from a static context");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report symbolAlreadyDefinedReport(JmmNode at, String symbolType, String symbol, String locationType, String location) {
        StringBuilder message = new StringBuilder();
        message.append(symbolType);
        message.append(" ");
        message.append(symbol);
        message.append(" is already defined in ");
        message.append(locationType);
        message.append(" ");
        message.append(location);

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotBeDereferencedReport(JmmNode at, String type) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(type);
        message.append(" cannot be dereferenced");
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report incompatibleTypesReport(JmmNode at, String actual, String expected) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(actual);
        message.append(" cannot be converted to ");
        message.append(expected);
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report operatorCannotBeAppliedReport(JmmNode at, String operator, String lhs, String rhs) {
        StringBuilder message = new StringBuilder();
        message.append("operator '");
        message.append(operator);
        message.append("' cannot be applied to '");
        message.append(lhs);
        message.append("' and '");
        message.append(rhs);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report operatorCannotBeAppliedReport(JmmNode at, String operator, String lhs) {
        StringBuilder message = new StringBuilder();
        message.append("operator '");
        message.append(operator);
        message.append("' cannot be applied to '");
        message.append(lhs);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report arrayRequiredReport(JmmNode at, String found) {
        StringBuilder message = new StringBuilder();
        message.append("array required, but ");
        message.append(found);
        message.append(" found");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotFindSymbolReport(JmmNode at, String symbol) {
        StringBuilder message = new StringBuilder();
        message.append("cannot find symbol '");
        message.append(symbol);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotFindTypeReport(JmmNode at, String type) {
        StringBuilder message = new StringBuilder();
        message.append("cannot find type '");
        message.append(type);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report ambiguousMethodCallReport(JmmNode at, String methodname) {
        StringBuilder message = new StringBuilder();
        message.append("reference to ");
        message.append(methodname);
        message.append(" is ambiguous");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report alreadyImported(JmmNode at, String lastName, String other) {
        StringBuilder message = new StringBuilder();
        message.append("a type with the same simple name '");
        message.append(lastName);
        message.append("' has already been imported from '");
        message.append(other);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }
}
