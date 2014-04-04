package com.abstratt.mdd.core.runtime;

import org.eclipse.uml2.uml.Classifier;

public class CannotInstantiateAbstractClassifier extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Classifier classifier;

	public CannotInstantiateAbstractClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	@Override
	public String getMessage() {
		return classifier.getQualifiedName();
	}

}
