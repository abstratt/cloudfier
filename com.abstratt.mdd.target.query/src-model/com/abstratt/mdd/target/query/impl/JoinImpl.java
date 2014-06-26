/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.abstratt.mdd.target.query.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.internal.impl.ElementImpl;

import com.abstratt.mdd.target.query.Join;
import com.abstratt.mdd.target.query.QueryPackage;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Join</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link com.abstratt.mdd.target.query.impl.JoinImpl#getSource <em>Source
 * </em>}</li>
 * <li>{@link com.abstratt.mdd.target.query.impl.JoinImpl#getTarget <em>Target
 * </em>}</li>
 * <li>{@link com.abstratt.mdd.target.query.impl.JoinImpl#getAssociation <em>
 * Association</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class JoinImpl extends ElementImpl implements Join {
    /**
     * The cached value of the '{@link #getSource() <em>Source</em>}' reference.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see #getSource()
     * @generated
     * @ordered
     */
    protected Property source;

    /**
     * The cached value of the '{@link #getTarget() <em>Target</em>}' reference.
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see #getTarget()
     * @generated
     * @ordered
     */
    protected Property target;

    /**
     * The cached value of the '{@link #getAssociation() <em>Association</em>}'
     * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @see #getAssociation()
     * @generated
     * @ordered
     */
    protected Association association;

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    protected JoinImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    public Association basicGetAssociation() {
        return association;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    public Property basicGetSource() {
        return source;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    public Property basicGetTarget() {
        return target;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
        case QueryPackage.JOIN__SOURCE:
            if (resolve)
                return getSource();
            return basicGetSource();
        case QueryPackage.JOIN__TARGET:
            if (resolve)
                return getTarget();
            return basicGetTarget();
        case QueryPackage.JOIN__ASSOCIATION:
            if (resolve)
                return getAssociation();
            return basicGetAssociation();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public boolean eIsSet(int featureID) {
        switch (featureID) {
        case QueryPackage.JOIN__SOURCE:
            return source != null;
        case QueryPackage.JOIN__TARGET:
            return target != null;
        case QueryPackage.JOIN__ASSOCIATION:
            return association != null;
        }
        return super.eIsSet(featureID);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
        case QueryPackage.JOIN__SOURCE:
            setSource((Property) newValue);
            return;
        case QueryPackage.JOIN__TARGET:
            setTarget((Property) newValue);
            return;
        case QueryPackage.JOIN__ASSOCIATION:
            setAssociation((Association) newValue);
            return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public void eUnset(int featureID) {
        switch (featureID) {
        case QueryPackage.JOIN__SOURCE:
            setSource((Property) null);
            return;
        case QueryPackage.JOIN__TARGET:
            setTarget((Property) null);
            return;
        case QueryPackage.JOIN__ASSOCIATION:
            setAssociation((Association) null);
            return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Association getAssociation() {
        if (association != null && association.eIsProxy()) {
            InternalEObject oldAssociation = (InternalEObject) association;
            association = (Association) eResolveProxy(oldAssociation);
            if (association != oldAssociation) {
                if (eNotificationRequired())
                    eNotify(new ENotificationImpl(this, Notification.RESOLVE, QueryPackage.JOIN__ASSOCIATION, oldAssociation, association));
            }
        }
        return association;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Property getSource() {
        if (source != null && source.eIsProxy()) {
            InternalEObject oldSource = (InternalEObject) source;
            source = (Property) eResolveProxy(oldSource);
            if (source != oldSource) {
                if (eNotificationRequired())
                    eNotify(new ENotificationImpl(this, Notification.RESOLVE, QueryPackage.JOIN__SOURCE, oldSource, source));
            }
        }
        return source;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public Property getTarget() {
        if (target != null && target.eIsProxy()) {
            InternalEObject oldTarget = (InternalEObject) target;
            target = (Property) eResolveProxy(oldTarget);
            if (target != oldTarget) {
                if (eNotificationRequired())
                    eNotify(new ENotificationImpl(this, Notification.RESOLVE, QueryPackage.JOIN__TARGET, oldTarget, target));
            }
        }
        return target;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public void setAssociation(Association newAssociation) {
        Association oldAssociation = association;
        association = newAssociation;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, QueryPackage.JOIN__ASSOCIATION, oldAssociation, association));
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public void setSource(Property newSource) {
        Property oldSource = source;
        source = newSource;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, QueryPackage.JOIN__SOURCE, oldSource, source));
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    public void setTarget(Property newTarget) {
        Property oldTarget = target;
        target = newTarget;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, QueryPackage.JOIN__TARGET, oldTarget, target));
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated
     */
    @Override
    protected EClass eStaticClass() {
        return QueryPackage.Literals.JOIN;
    }

} // JoinImpl
