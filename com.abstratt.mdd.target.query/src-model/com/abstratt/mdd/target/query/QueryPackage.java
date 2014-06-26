/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * <!-- begin-user-doc --> The <b>Package</b> for the model. It contains
 * accessors for the meta objects to represent
 * <ul>
 * <li>each class,</li>
 * <li>each feature of each class,</li>
 * <li>each enum,</li>
 * <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * 
 * @see com.abstratt.mdd.target.query.QueryFactory
 * @model kind="package"
 * @generated
 */
public interface QueryPackage extends EPackage {
    /**
     * <!-- begin-user-doc --> Defines literals for the meta objects that
     * represent
     * <ul>
     * <li>each class,</li>
     * <li>each feature of each class,</li>
     * <li>each enum,</li>
     * <li>and each data type</li>
     * </ul>
     * <!-- end-user-doc -->
     * 
     * @generated
     */
    interface Literals {
        /**
         * The meta object literal for the '
         * {@link com.abstratt.mdd.target.query.impl.QueryImpl <em>Query</em>}'
         * class. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @see com.abstratt.mdd.target.query.impl.QueryImpl
         * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getQuery()
         * @generated
         */
        EClass QUERY = QueryPackage.eINSTANCE.getQuery();

        /**
         * The meta object literal for the '<em><b>Source Type</b></em>'
         * reference feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference QUERY__SOURCE_TYPE = QueryPackage.eINSTANCE.getQuery_SourceType();

        /**
         * The meta object literal for the '<em><b>Joins</b></em>' containment
         * reference list feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference QUERY__JOINS = QueryPackage.eINSTANCE.getQuery_Joins();

        /**
         * The meta object literal for the '<em><b>Filters</b></em>' reference
         * list feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference QUERY__FILTERS = QueryPackage.eINSTANCE.getQuery_Filters();

        /**
         * The meta object literal for the '
         * {@link com.abstratt.mdd.target.query.impl.JoinImpl <em>Join</em>}'
         * class. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @see com.abstratt.mdd.target.query.impl.JoinImpl
         * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getJoin()
         * @generated
         */
        EClass JOIN = QueryPackage.eINSTANCE.getJoin();

        /**
         * The meta object literal for the '<em><b>Source</b></em>' reference
         * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference JOIN__SOURCE = QueryPackage.eINSTANCE.getJoin_Source();

        /**
         * The meta object literal for the '<em><b>Target</b></em>' reference
         * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference JOIN__TARGET = QueryPackage.eINSTANCE.getJoin_Target();

        /**
         * The meta object literal for the '<em><b>Association</b></em>'
         * reference feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference JOIN__ASSOCIATION = QueryPackage.eINSTANCE.getJoin_Association();

        /**
         * The meta object literal for the '
         * {@link com.abstratt.mdd.target.query.impl.PropertyReferenceImpl
         * <em>Property Reference</em>}' class. <!-- begin-user-doc --> <!--
         * end-user-doc -->
         * 
         * @see com.abstratt.mdd.target.query.impl.PropertyReferenceImpl
         * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getPropertyReference()
         * @generated
         */
        EClass PROPERTY_REFERENCE = QueryPackage.eINSTANCE.getPropertyReference();

        /**
         * The meta object literal for the '<em><b>Property</b></em>' reference
         * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference PROPERTY_REFERENCE__PROPERTY = QueryPackage.eINSTANCE.getPropertyReference_Property();

        /**
         * The meta object literal for the '
         * {@link com.abstratt.mdd.target.query.impl.VariableReferenceImpl
         * <em>Variable Reference</em>}' class. <!-- begin-user-doc --> <!--
         * end-user-doc -->
         * 
         * @see com.abstratt.mdd.target.query.impl.VariableReferenceImpl
         * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getVariableReference()
         * @generated
         */
        EClass VARIABLE_REFERENCE = QueryPackage.eINSTANCE.getVariableReference();

        /**
         * The meta object literal for the '<em><b>Variable</b></em>' reference
         * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
         * 
         * @generated
         */
        EReference VARIABLE_REFERENCE__VARIABLE = QueryPackage.eINSTANCE.getVariableReference_Variable();

    }

    /**
     * The package name. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    String eNAME = "query";

    /**
     * The package namespace URI. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    String eNS_URI = "http://abstratt.com/mdd/1.0.0/query";

    /**
     * The package namespace name. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    String eNS_PREFIX = "query";

    /**
     * The singleton instance of the package. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @generated
     */
    QueryPackage eINSTANCE = com.abstratt.mdd.target.query.impl.QueryPackageImpl.init();

    /**
     * The meta object id for the '
     * {@link com.abstratt.mdd.target.query.impl.QueryImpl <em>Query</em>}'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see com.abstratt.mdd.target.query.impl.QueryImpl
     * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getQuery()
     * @generated
     */
    int QUERY = 0;

    /**
     * The feature id for the '<em><b>EAnnotations</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__EANNOTATIONS = UMLPackage.ELEMENT__EANNOTATIONS;

    /**
     * The feature id for the '<em><b>Owned Element</b></em>' reference list.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__OWNED_ELEMENT = UMLPackage.ELEMENT__OWNED_ELEMENT;

    /**
     * The feature id for the '<em><b>Owner</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__OWNER = UMLPackage.ELEMENT__OWNER;

    /**
     * The feature id for the '<em><b>Owned Comment</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__OWNED_COMMENT = UMLPackage.ELEMENT__OWNED_COMMENT;

    /**
     * The feature id for the '<em><b>Source Type</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__SOURCE_TYPE = UMLPackage.ELEMENT_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Joins</b></em>' containment reference
     * list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__JOINS = UMLPackage.ELEMENT_FEATURE_COUNT + 1;

    /**
     * The feature id for the '<em><b>Filters</b></em>' reference list. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY__FILTERS = UMLPackage.ELEMENT_FEATURE_COUNT + 2;

    /**
     * The number of structural features of the '<em>Query</em>' class. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int QUERY_FEATURE_COUNT = UMLPackage.ELEMENT_FEATURE_COUNT + 3;

    /**
     * The meta object id for the '
     * {@link com.abstratt.mdd.target.query.impl.JoinImpl <em>Join</em>}' class.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see com.abstratt.mdd.target.query.impl.JoinImpl
     * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getJoin()
     * @generated
     */
    int JOIN = 1;

    /**
     * The feature id for the '<em><b>EAnnotations</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__EANNOTATIONS = UMLPackage.ELEMENT__EANNOTATIONS;

    /**
     * The feature id for the '<em><b>Owned Element</b></em>' reference list.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__OWNED_ELEMENT = UMLPackage.ELEMENT__OWNED_ELEMENT;

    /**
     * The feature id for the '<em><b>Owner</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__OWNER = UMLPackage.ELEMENT__OWNER;

    /**
     * The feature id for the '<em><b>Owned Comment</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__OWNED_COMMENT = UMLPackage.ELEMENT__OWNED_COMMENT;

    /**
     * The feature id for the '<em><b>Source</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__SOURCE = UMLPackage.ELEMENT_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Target</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__TARGET = UMLPackage.ELEMENT_FEATURE_COUNT + 1;

    /**
     * The feature id for the '<em><b>Association</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN__ASSOCIATION = UMLPackage.ELEMENT_FEATURE_COUNT + 2;

    /**
     * The number of structural features of the '<em>Join</em>' class. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int JOIN_FEATURE_COUNT = UMLPackage.ELEMENT_FEATURE_COUNT + 3;

    /**
     * The meta object id for the '
     * {@link com.abstratt.mdd.target.query.impl.PropertyReferenceImpl
     * <em>Property Reference</em>}' class. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @see com.abstratt.mdd.target.query.impl.PropertyReferenceImpl
     * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getPropertyReference()
     * @generated
     */
    int PROPERTY_REFERENCE = 2;

    /**
     * The feature id for the '<em><b>EAnnotations</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__EANNOTATIONS = UMLPackage.VALUE_SPECIFICATION__EANNOTATIONS;

    /**
     * The feature id for the '<em><b>Owned Element</b></em>' reference list.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__OWNED_ELEMENT = UMLPackage.VALUE_SPECIFICATION__OWNED_ELEMENT;

    /**
     * The feature id for the '<em><b>Owner</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__OWNER = UMLPackage.VALUE_SPECIFICATION__OWNER;

    /**
     * The feature id for the '<em><b>Owned Comment</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__OWNED_COMMENT = UMLPackage.VALUE_SPECIFICATION__OWNED_COMMENT;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__NAME = UMLPackage.VALUE_SPECIFICATION__NAME;

    /**
     * The feature id for the '<em><b>Visibility</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__VISIBILITY = UMLPackage.VALUE_SPECIFICATION__VISIBILITY;

    /**
     * The feature id for the '<em><b>Qualified Name</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__QUALIFIED_NAME = UMLPackage.VALUE_SPECIFICATION__QUALIFIED_NAME;

    /**
     * The feature id for the '<em><b>Client Dependency</b></em>' reference
     * list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__CLIENT_DEPENDENCY = UMLPackage.VALUE_SPECIFICATION__CLIENT_DEPENDENCY;

    /**
     * The feature id for the '<em><b>Namespace</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__NAMESPACE = UMLPackage.VALUE_SPECIFICATION__NAMESPACE;

    /**
     * The feature id for the '<em><b>Name Expression</b></em>' containment
     * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__NAME_EXPRESSION = UMLPackage.VALUE_SPECIFICATION__NAME_EXPRESSION;

    /**
     * The feature id for the '<em><b>Owning Template Parameter</b></em>'
     * container reference. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__OWNING_TEMPLATE_PARAMETER = UMLPackage.VALUE_SPECIFICATION__OWNING_TEMPLATE_PARAMETER;

    /**
     * The feature id for the '<em><b>Template Parameter</b></em>' reference.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__TEMPLATE_PARAMETER = UMLPackage.VALUE_SPECIFICATION__TEMPLATE_PARAMETER;

    /**
     * The feature id for the '<em><b>Type</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__TYPE = UMLPackage.VALUE_SPECIFICATION__TYPE;

    /**
     * The feature id for the '<em><b>Property</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE__PROPERTY = UMLPackage.VALUE_SPECIFICATION_FEATURE_COUNT + 0;

    /**
     * The number of structural features of the '<em>Property Reference</em>'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int PROPERTY_REFERENCE_FEATURE_COUNT = UMLPackage.VALUE_SPECIFICATION_FEATURE_COUNT + 1;

    /**
     * The meta object id for the '
     * {@link com.abstratt.mdd.target.query.impl.VariableReferenceImpl
     * <em>Variable Reference</em>}' class. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @see com.abstratt.mdd.target.query.impl.VariableReferenceImpl
     * @see com.abstratt.mdd.target.query.impl.QueryPackageImpl#getVariableReference()
     * @generated
     */
    int VARIABLE_REFERENCE = 3;

    /**
     * The feature id for the '<em><b>EAnnotations</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__EANNOTATIONS = UMLPackage.VALUE_SPECIFICATION__EANNOTATIONS;

    /**
     * The feature id for the '<em><b>Owned Element</b></em>' reference list.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__OWNED_ELEMENT = UMLPackage.VALUE_SPECIFICATION__OWNED_ELEMENT;

    /**
     * The feature id for the '<em><b>Owner</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__OWNER = UMLPackage.VALUE_SPECIFICATION__OWNER;

    /**
     * The feature id for the '<em><b>Owned Comment</b></em>' containment
     * reference list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__OWNED_COMMENT = UMLPackage.VALUE_SPECIFICATION__OWNED_COMMENT;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__NAME = UMLPackage.VALUE_SPECIFICATION__NAME;

    /**
     * The feature id for the '<em><b>Visibility</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__VISIBILITY = UMLPackage.VALUE_SPECIFICATION__VISIBILITY;

    /**
     * The feature id for the '<em><b>Qualified Name</b></em>' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__QUALIFIED_NAME = UMLPackage.VALUE_SPECIFICATION__QUALIFIED_NAME;

    /**
     * The feature id for the '<em><b>Client Dependency</b></em>' reference
     * list. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__CLIENT_DEPENDENCY = UMLPackage.VALUE_SPECIFICATION__CLIENT_DEPENDENCY;

    /**
     * The feature id for the '<em><b>Namespace</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__NAMESPACE = UMLPackage.VALUE_SPECIFICATION__NAMESPACE;

    /**
     * The feature id for the '<em><b>Name Expression</b></em>' containment
     * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__NAME_EXPRESSION = UMLPackage.VALUE_SPECIFICATION__NAME_EXPRESSION;

    /**
     * The feature id for the '<em><b>Owning Template Parameter</b></em>'
     * container reference. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__OWNING_TEMPLATE_PARAMETER = UMLPackage.VALUE_SPECIFICATION__OWNING_TEMPLATE_PARAMETER;

    /**
     * The feature id for the '<em><b>Template Parameter</b></em>' reference.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__TEMPLATE_PARAMETER = UMLPackage.VALUE_SPECIFICATION__TEMPLATE_PARAMETER;

    /**
     * The feature id for the '<em><b>Type</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__TYPE = UMLPackage.VALUE_SPECIFICATION__TYPE;

    /**
     * The feature id for the '<em><b>Variable</b></em>' reference. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE__VARIABLE = UMLPackage.VALUE_SPECIFICATION_FEATURE_COUNT + 0;

    /**
     * The number of structural features of the '<em>Variable Reference</em>'
     * class. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     * @ordered
     */
    int VARIABLE_REFERENCE_FEATURE_COUNT = UMLPackage.VALUE_SPECIFICATION_FEATURE_COUNT + 1;

    /**
     * Returns the meta object for class '
     * {@link com.abstratt.mdd.target.query.Join <em>Join</em>}'. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for class '<em>Join</em>'.
     * @see com.abstratt.mdd.target.query.Join
     * @generated
     */
    EClass getJoin();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.Join#getAssociation
     * <em>Association</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Association</em>'.
     * @see com.abstratt.mdd.target.query.Join#getAssociation()
     * @see #getJoin()
     * @generated
     */
    EReference getJoin_Association();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.Join#getSource <em>Source</em>}'.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Source</em>'.
     * @see com.abstratt.mdd.target.query.Join#getSource()
     * @see #getJoin()
     * @generated
     */
    EReference getJoin_Source();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.Join#getTarget <em>Target</em>}'.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Target</em>'.
     * @see com.abstratt.mdd.target.query.Join#getTarget()
     * @see #getJoin()
     * @generated
     */
    EReference getJoin_Target();

    /**
     * Returns the meta object for class '
     * {@link com.abstratt.mdd.target.query.PropertyReference
     * <em>Property Reference</em>}'. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @return the meta object for class '<em>Property Reference</em>'.
     * @see com.abstratt.mdd.target.query.PropertyReference
     * @generated
     */
    EClass getPropertyReference();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.PropertyReference#getProperty
     * <em>Property</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Property</em>'.
     * @see com.abstratt.mdd.target.query.PropertyReference#getProperty()
     * @see #getPropertyReference()
     * @generated
     */
    EReference getPropertyReference_Property();

    /**
     * Returns the meta object for class '
     * {@link com.abstratt.mdd.target.query.Query <em>Query</em>}'. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for class '<em>Query</em>'.
     * @see com.abstratt.mdd.target.query.Query
     * @generated
     */
    EClass getQuery();

    /**
     * Returns the meta object for the reference list '
     * {@link com.abstratt.mdd.target.query.Query#getFilters <em>Filters</em>}'.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference list '<em>Filters</em>'.
     * @see com.abstratt.mdd.target.query.Query#getFilters()
     * @see #getQuery()
     * @generated
     */
    EReference getQuery_Filters();

    /**
     * Returns the meta object for the containment reference list '
     * {@link com.abstratt.mdd.target.query.Query#getJoins <em>Joins</em>}'.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the containment reference list '
     *         <em>Joins</em>'.
     * @see com.abstratt.mdd.target.query.Query#getJoins()
     * @see #getQuery()
     * @generated
     */
    EReference getQuery_Joins();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.Query#getSourceType
     * <em>Source Type</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Source Type</em>'.
     * @see com.abstratt.mdd.target.query.Query#getSourceType()
     * @see #getQuery()
     * @generated
     */
    EReference getQuery_SourceType();

    /**
     * Returns the factory that creates the instances of the model. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the factory that creates the instances of the model.
     * @generated
     */
    QueryFactory getQueryFactory();

    /**
     * Returns the meta object for class '
     * {@link com.abstratt.mdd.target.query.VariableReference
     * <em>Variable Reference</em>}'. <!-- begin-user-doc --> <!-- end-user-doc
     * -->
     * 
     * @return the meta object for class '<em>Variable Reference</em>'.
     * @see com.abstratt.mdd.target.query.VariableReference
     * @generated
     */
    EClass getVariableReference();

    /**
     * Returns the meta object for the reference '
     * {@link com.abstratt.mdd.target.query.VariableReference#getVariable
     * <em>Variable</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return the meta object for the reference '<em>Variable</em>'.
     * @see com.abstratt.mdd.target.query.VariableReference#getVariable()
     * @see #getVariableReference()
     * @generated
     */
    EReference getVariableReference_Variable();

} // QueryPackage
