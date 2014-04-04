//package com.abstratt.mdd.core.runtime.external.mdd_console;
//
//import com.abstratt.mdd.core.runtime.ExecutionContext;
//import com.abstratt.mdd.core.runtime.external.ExternalClass;
//
//import org.eclipse.uml2.uml.Classifier;
//
//public class Console extends ExternalClass {
//	public static void write(@SuppressWarnings("unused")
//	ExecutionContext context, Object[] arguments) {
//		for (int i = 0; i < arguments.length; i++)
//			System.out.print(arguments[i]);
//	}
//
//	public static void writeln(ExecutionContext context, Object[] arguments) {
//		write(context, arguments);
//		System.out.println();
//	}
//
//	public Console(Classifier classifier) {
//		super(classifier);
//		// TODO Auto-generated constructor stub
//	}
//}
