/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query.operations;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.internal.operations.ElementOperations;

import com.abstratt.mdd.target.query.Query;

/**
 * <!-- begin-user-doc --> A static utility class that provides operations
 * related to '<em><b>Query</b></em>' model objects. <!-- end-user-doc -->
 *
 * <p>
 * The following operations are supported:
 * <ul>
 * <li>{@link com.abstratt.mdd.target.query.Query#getTargetType() <em>Get Target
 * Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class QueryOperations extends ElementOperations {
    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated NOT
     */
    public static Classifier getTargetType(Query query) {
        if (query.getJoins().isEmpty())
            return query.getSourceType();
        return (Classifier) query.getJoins().get(query.getJoins().size() - 1).getTarget().getType();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    protected QueryOperations() {
        super();
    }
} // QueryOperations