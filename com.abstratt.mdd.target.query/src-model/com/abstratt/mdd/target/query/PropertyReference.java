/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query;

import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Property Reference</b></em>'. <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.abstratt.mdd.target.query.PropertyReference#getProperty <em>
 * Property</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.abstratt.mdd.target.query.QueryPackage#getPropertyReference()
 * @model
 * @generated
 */
public interface PropertyReference extends ValueSpecification {
    /**
     * Returns the value of the '<em><b>Property</b></em>' reference. <!--
     * begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Property</em>' reference isn't clear, there
     * really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * 
     * @return the value of the '<em>Property</em>' reference.
     * @see #setProperty(Property)
     * @see com.abstratt.mdd.target.query.QueryPackage#getPropertyReference_Property()
     * @model required="true" ordered="false"
     * @generated
     */
    Property getProperty();

    /**
     * Sets the value of the '
     * {@link com.abstratt.mdd.target.query.PropertyReference#getProperty
     * <em>Property</em>}' reference. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @param value
     *            the new value of the '<em>Property</em>' reference.
     * @see #getProperty()
     * @generated
     */
    void setProperty(Property value);

} // PropertyReference
