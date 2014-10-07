package com.abstratt.mdd.target.engine.st;

import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.GroupLexer;
import org.antlr.stringtemplate.language.GroupParser;
import org.eclipse.core.runtime.CoreException;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamRecognitionException;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.util.MDDUtil;
import com.abstratt.mdd.frontend.core.IProblem;
import com.abstratt.mdd.frontend.core.IProblem.Severity;
import com.abstratt.mdd.frontend.core.Problem;
import com.abstratt.mdd.frontend.core.spi.CompilationContext;
import com.abstratt.mdd.frontend.core.spi.ICompiler;

public class STValidator implements ICompiler {
    
    private static Pattern LINE_NUMBER_PATTERN = Pattern.compile(".*line ([0-9]*).*");
    
    public static class STProblem extends Problem {

        private String message;

        public STProblem(Severity severity, String message) {
            super(severity);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

	@Override
	public void compile(Reader source, final CompilationContext context)
			throws CoreException {
	    List<URI> baseURIs = new ArrayList<URI>();
        baseURIs.add(MDDUtil.fromEMFToJava(context.getRepository().getBaseURI()));
        String imported = context.getRepository().getProperties().getProperty(IRepository.IMPORTED_PROJECTS);
           
        if (imported != null)
            for (String importedURI : imported.split(","))
                baseURIs.add(URI.create(importedURI));

        STEGroupLoader.registerBaseURIs(baseURIs.toArray(new URI[0]));
        try {
    		new ProblemReporter(source, context);
        } catch (STException e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getCause() != null ? e.getCause().getMessage() : e.toString();
            context.getProblemTracker().add(new STProblem(Severity.ERROR, message));
        } finally {
            STEGroupLoader.clearBaseURI();
        }
	}

	@Override
	public String findModelName(String toParse) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String format(String toFormat) {
		return toFormat;
	}

	/**
	 * This intermediate class only exists so we can have access to the compilation context from methods invoked by the super-class' constructor.
	 */
	private class ProblemReporter {
		private CompilationContext context;
		ProblemReporter(Reader source, CompilationContext context) {
			this.context = context;
			new CustomStringTemplateGroup(source);
		}

        public void reportProblem(String msg, Throwable t, Severity severity) {
            String message = null;
            Integer lineNumber = null;
            if (t instanceof TokenStreamRecognitionException)
                t = ((TokenStreamRecognitionException) t).recog;
            if (msg == null) {
                if (t instanceof ANTLRException)
                    message = t.getMessage();
                else if (t instanceof RuntimeException)
                    message = t.toString();
            } else {
                message = msg;
                if (t != null && t.getMessage() != null)
                    message +=  " (" + t.getMessage() + ")";
            }
            if (message != null && message.contains("line")) {
                // desperate measure - extract line number info from the error
                // message
                Matcher matched = LINE_NUMBER_PATTERN.matcher(message);
                if (matched.matches())
                    lineNumber = Integer.parseInt(matched.group(1));
            }
            if (lineNumber == null && t instanceof RecognitionException)
                lineNumber = ((RecognitionException) t).getLine();
            IProblem toReport = new STProblem(severity, message);
            toReport.setAttribute(IProblem.LINE_NUMBER, lineNumber);
            context.getProblemTracker().add(toReport);
        }
		
		private final class CustomStringTemplateGroup extends StringTemplateGroup
				implements StringTemplateErrorListener {
	
			private CustomStringTemplateGroup(Reader r) {
				super(r);
			} 
	
			@Override 
			public void error(String msg, Exception e) {
				reportProblem(msg, e, Severity.ERROR);
			}
	
			protected void parseGroup(Reader r) {
				this.listener = this;
				try {
					GroupLexer lexer = new GroupLexer(r) {
						@Override
						public void reportError(RecognitionException e) {
							reportProblem(null, e, Severity.ERROR);
						}
	
						@Override
						public void reportError(String arg0) {
							reportProblem(arg0, null, Severity.ERROR);
						}
	
						@Override
						public void reportWarning(String arg0) {
							reportProblem(arg0, null, Severity.WARNING);
						}
					};
					GroupParser parser = new GroupParser(lexer) {
						@Override
						public void reportError(RecognitionException e) {
							reportProblem(null, e, Severity.ERROR);
						}
	
						@Override
						public void reportError(String arg0) {
							reportProblem(arg0, null, Severity.ERROR);
						}
	
						@Override
						public void reportWarning(String arg0) {
							reportProblem(arg0, null, Severity.WARNING);
						}
					};
					parser.group(this);
				} catch (ANTLRException e) {
					reportProblem(null, e, Severity.ERROR);
				}
			}
			@Override
			public void error(String msg, Throwable e) {
				reportProblem(msg, e, Severity.ERROR);
			}

			@Override
			public void warning(String msg) {
				reportProblem(msg, null, Severity.WARNING);
			}
		}
	}
}
