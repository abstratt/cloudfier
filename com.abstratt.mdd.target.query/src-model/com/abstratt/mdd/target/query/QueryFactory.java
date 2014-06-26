/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc --> The <b>Factory</b> for the model. It provides a
 * create method for each non-abstract class of the model. <!-- end-user-doc -->
 * 
 * @see com.abstratt.mdd.target.query.QueryPackage
 * @generated
 */
public interface QueryFactory extends EFactory {
    /**
     * The singleton instance of the factory. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    QueryFactory eINSTANCE = com.abstratt.mdd.target.query.impl.QueryFactoryImpl.init();

    /**
     * Returns a new object of class '<em>Join</em>'. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @return a new object of class '<em>Join</em>'.
     * @generated
     */
    Join createJoin();

    /**
     * Returns a new object of class '<em>Property Reference</em>'. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return a new object of class '<em>Property Reference</em>'.
     * @generated
     */
    PropertyReference createPropertyReference();

    /**
     * Returns a new object of class '<em>Query</em>'. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @return a new object of class '<em>Query</em>'.
     * @generated
     */
    Query createQuery();

    /**
     * Returns a new object of class '<em>Variable Reference</em>'. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return a new object of class '<em>Variable Reference</em>'.
     * @generated
     */
    VariableReference createVariableReference();

    /**
     * Returns the package supported by this factory. <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * 
     * @return the package supported by this factory.
     * @generated
     */
    QueryPackage getQueryPackage();

} // QueryFactory
