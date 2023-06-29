package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.ArrayList;
import java.util.List;

public class MyAnalyser implements JmmAnalysis {
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        MySymbolTable symbolTable = new MySymbolTable();
        MyVisitor visitor = new MyVisitor(symbolTable, "");
        String generatedCode = visitor.visit(parserResult.getRootNode(),"");
        List<Report> symbolTableReports = parserResult.getReports();
        //List<Report> reports = new ArrayList<>();
        List<Report> analysisReports = new MyAnalyserVisitor(symbolTable).visit(parserResult.getRootNode(), symbolTable);
        List<Report> reports = SpecsCollections.concat(symbolTableReports, analysisReports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
