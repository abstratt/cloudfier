package com.abstratt.kirra.populator.compiler;

import static com.abstratt.mdd.core.IProblem.Severity.ERROR;
import static com.abstratt.mdd.core.IProblem.Severity.WARNING;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.core.runtime.CoreException;

import com.abstratt.kirra.SchemaManagement;
import com.abstratt.kirra.populator.DataParser;
import com.abstratt.kirra.populator.DataValidator;
import com.abstratt.kirra.populator.DataValidator.ErrorCollector;
import com.abstratt.mdd.core.IProblem;
import com.abstratt.mdd.core.IProblem.Severity;
import com.abstratt.mdd.core.Problem;
import com.abstratt.mdd.core.RepositoryService;
import com.abstratt.mdd.frontend.core.spi.CompilationContext;
import com.abstratt.mdd.frontend.core.spi.ICompiler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class DataCompiler implements ICompiler {

    public static class DataProblem extends Problem {

        private String message;

        public DataProblem(Severity severity, String message) {
            super(severity);
            this.message = message;
        }

        public DataProblem(String message) {
            this(Severity.ERROR, message);
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    private static DataProblem addProblem(CompilationContext context, Severity severity, String message) {
        DataProblem toReport = new DataProblem(severity, message);
        context.getProblemTracker().add(toReport);
        return toReport;
    }

    @Override
    public void compile(Reader source, final CompilationContext context) throws CoreException {
        try {
            final JsonNode root = DataParser.parse(source);
            if (root == null)
                return;
            if (!root.isObject())
                DataCompiler.addProblem(context, WARNING, "Data ignored, root object must be a map (of package names to maps of classes)");
            final ErrorCollector errorCollector = new ErrorCollector() {
                @Override
                public void addError(String description) {
                    DataCompiler.addProblem(context, ERROR, removeNewLines(description));

                }

                @Override
                public void addWarning(String description) {
                    DataCompiler.addProblem(context, WARNING, removeNewLines(description));
                }
            };
            SchemaManagement schema = RepositoryService.DEFAULT.getFeature(SchemaManagement.class);
            new DataValidator(schema, errorCollector).validate(root);
        } catch (JsonProcessingException e) {
            DataProblem reported = DataCompiler.addProblem(context, ERROR, removeNewLines(e.getMessage()));
            reported.setAttribute(IProblem.LINE_NUMBER, e.getLocation().getLineNr());
        } catch (IOException e) {
            DataCompiler.addProblem(context, ERROR, "Could not read data: " + e.getMessage());
        }
    }

    @Override
    public String findModelName(String toParse) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String format(String toFormat) {
        return toFormat;
    }

    protected String removeNewLines(String text) {
        return text.replace("\n", "").replace("\r", "");
    }
}
