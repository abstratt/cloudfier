/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query;

import org.eclipse.emf.common.util.EList;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Element;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Query</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 *  Represents a query. 
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.abstratt.mdd.target.query.Query#getSourceType <em>Source Type</em>}</li>
 *   <li>{@link com.abstratt.mdd.target.query.Query#getJoins <em>Joins</em>}</li>
 *   <li>{@link com.abstratt.mdd.target.query.Query#getFilters <em>Filters</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.abstratt.mdd.target.query.QueryPackage#getQuery()
 * @model
 * @generated
 */
public interface Query extends Element {
	/**
	 * Returns the value of the '<em><b>Source Type</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Source Type</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Source Type</em>' reference.
	 * @see #setSourceType(Classifier)
	 * @see com.abstratt.mdd.target.query.QueryPackage#getQuery_SourceType()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	Classifier getSourceType();

	/**
	 * Sets the value of the '{@link com.abstratt.mdd.target.query.Query#getSourceType <em>Source Type</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source Type</em>' reference.
	 * @see #getSourceType()
	 * @generated
	 */
	void setSourceType(Classifier value);

	/**
	 * Returns the value of the '<em><b>Joins</b></em>' containment reference list.
	 * The list contents are of type {@link com.abstratt.mdd.target.query.Join}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Joins</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Joins</em>' containment reference list.
	 * @see com.abstratt.mdd.target.query.QueryPackage#getQuery_Joins()
	 * @model containment="true" resolveProxies="true" ordered="false"
	 * @generated
	 */
	EList<Join> getJoins();

	/**
	 * Returns the value of the '<em><b>Filters</b></em>' reference list.
	 * The list contents are of type {@link org.eclipse.uml2.uml.Activity}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Filters</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Filters</em>' reference list.
	 * @see com.abstratt.mdd.target.query.QueryPackage#getQuery_Filters()
	 * @model ordered="false"
	 * @generated
	 */
	EList<Activity> getFilters();

	/**
	 * Retrieves the first {@link org.eclipse.uml2.uml.Activity} with the specified '<em><b>Name</b></em>' from the '<em><b>Filters</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name The '<em><b>Name</b></em>' of the {@link org.eclipse.uml2.uml.Activity} to retrieve, or <code>null</code>.
	 * @return The first {@link org.eclipse.uml2.uml.Activity} with the specified '<em><b>Name</b></em>', or <code>null</code>.
	 * @see #getFilters()
	 * @generated
	 */
	Activity getFilters(String name);

	/**
	 * Retrieves the first {@link org.eclipse.uml2.uml.Activity} with the specified '<em><b>Name</b></em>' from the '<em><b>Filters</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name The '<em><b>Name</b></em>' of the {@link org.eclipse.uml2.uml.Activity} to retrieve, or <code>null</code>.
	 * @param ignoreCase Whether to ignore case in {@link java.lang.String} comparisons.
	 * @return The first {@link org.eclipse.uml2.uml.Activity} with the specified '<em><b>Name</b></em>', or <code>null</code>.
	 * @see #getFilters()
	 * @generated
	 */
	Activity getFilters(String name, boolean ignoreCase);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" required="true" ordered="false"
	 * @generated
	 */
	Classifier getTargetType();

} // Query
