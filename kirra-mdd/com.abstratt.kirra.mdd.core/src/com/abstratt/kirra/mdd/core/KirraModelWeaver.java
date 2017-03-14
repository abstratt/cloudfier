package com.abstratt.kirra.mdd.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.VisibilityKind;
import org.eclipse.uml2.uml.util.UMLUtil;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.isv.IModelWeaver;
import com.abstratt.mdd.core.util.ClassifierUtils;
import com.abstratt.mdd.core.util.ConnectorUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.PackageUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;

/**
 * A model weaver for turning plain UML models into Kirra-compatible models.
 */
public class KirraModelWeaver implements IModelWeaver {
	
	public void packageCreated(IRepository repository, Package created) {
		Profile kirraProfile = (Profile) repository.loadPackage(URI.createURI(KirraMDDCore.KIRRA_URI));
		Package types = repository.findPackage(IRepository.TYPES_NAMESPACE, null);
		Package extensions = repository.findPackage(IRepository.EXTENSIONS_NAMESPACE, null);
		PackageUtils.safeApplyProfile(created, kirraProfile);
		PackageUtils.importPackage(created, types);
		PackageUtils.importPackage(created, extensions);
		PackageUtils.importPackage(created, kirraProfile);
	}

	/**
	 * Applies the Kirra stereotypes wherever it makes sense. Automatically
	 * creates reference-like associations for attributes whose types are
	 * entities themselves.
	 * Creates wirings between user entities and the User built-in class.
	 */
	@Override
	public void repositoryComplete(IRepository repository) {
		new Session(repository).weave();
	}

	public class Session {

		private Stereotype roleStereotype;
		private Stereotype debuggableStereotype;
		@Deprecated
		private Stereotype serviceStereotype;
		private Class baseObject;
		private IRepository repository;
		private boolean enabled;
		private ArrayList<BehavioredClassifier> services;
		private List<Class> entities;

		public Session(IRepository repository) {
			this.repository = repository;
			roleStereotype = repository.findNamedElement(MDDExtensionUtils.ROLE_CLASS_STEREOTYPE, Literals.STEREOTYPE, null);
			debuggableStereotype = repository.findNamedElement(MDDExtensionUtils.DEBUGGABLE_STEREOTYPE,
					Literals.STEREOTYPE, null);
			serviceStereotype = repository.findNamedElement("kirra::Service", Literals.STEREOTYPE, null);

			baseObject = repository.findNamedElement("mdd_types::Object", Literals.CLASS, null);
			enabled = !(baseObject == null);

			if (!enabled)
				return;

			// collect all services
			services = new ArrayList<BehavioredClassifier>();
			repository.findAll(new EObjectCondition() {

				@Override
				public boolean isSatisfied(EObject eObject) {
					if (UMLPackage.Literals.PORT != eObject.eClass())
						return false;
					BehavioredClassifier serviceClass = ConnectorUtils.findProvidingClassifier((Port) eObject);
					if (serviceClass != null) {
						services.add(serviceClass);
						StereotypeUtils.safeApplyStereotype(serviceClass, serviceStereotype);
					}
					return false;
				}
			}, true);

			// collect all entity-candidate classes
			entities = repository.findAll(new EObjectCondition() {
				@Override
				public boolean isSatisfied(EObject eObject) {
					if (UMLPackage.Literals.CLASS != eObject.eClass())
						return false;
					Class asClass = (Class) eObject;
					if (asClass.getName() == null)
						return false;
					if (!asClass.conformsTo(baseObject))
						return false;
					if (services.contains(asClass))
						return false;
					// we accept two stereotypes - anything else will exclude
					// them from
					// automatic entity stereotype application
					List<Stereotype> appliedStereotypes = new ArrayList<Stereotype>(asClass.getAppliedStereotypes());
					appliedStereotypes.removeAll(Arrays.asList(roleStereotype, debuggableStereotype));
					return appliedStereotypes.isEmpty();
				}
			}, true);

		}

		public void weave() {
			if (!enabled)
				return;
			enhanceUserEntities();
			configureEntities();
			buildAssociations();
		}

		private void configureEntities() {
			// apply entity stereotype
//			for (Class entity : entities)
//				StereotypeUtils.safeApplyStereotype(entity, entityStereotype);
		}

		private void enhanceUserEntities() {
			List<Class> roleEntities = entities.stream().filter(it -> KirraHelper.isRole(it)).collect(Collectors.toList());
			if (roleEntities.isEmpty())
				return;
			Package kirraProfilePackage = repository.findPackage(KirraMDDConstants.USER_PROFILE_MODEL, null);
			if (kirraProfilePackage == null)
				throw new IllegalStateException("No package for user profiles");
			
			Class profileClass = (Class) repository.findNamedElement("userprofile::Profile", UMLPackage.Literals.CLASS, null);
			
			// if there are any role classes in this package, create relationships to the profile class
			// (ignore role classes that specialize other role classes)
			List<Class> nonDerivedRoleEntities = roleEntities.stream().filter(it -> !roleEntities.stream().anyMatch(other -> it != other && ClassifierUtils.isKindOf(it,  other))).collect(Collectors.toList());
			nonDerivedRoleEntities.stream().forEach(roleClass -> {
				PackageUtils.importPackage(roleClass.getNearestPackage(), kirraProfilePackage).setVisibility(VisibilityKind.PRIVATE_LITERAL);
				
				Property userProfile = roleClass.createOwnedAttribute(KirraMDDConstants.USER_PROFILE_ASSOCIATION_END, profileClass);
				userProfile.setIsReadOnly(true);
				userProfile.setLower(0);
				
				Property otherEnd = buildAssociationForAttribute(roleClass, userProfile, "roleAs" + roleClass.getName(), false);
				otherEnd.setIsNavigable(true);
				userProfile.setIsNavigable(true);
			});
		}

		private void buildAssociations() {
			// ensure properties that refer to entities are part of associations
			// (just like references)
			for (Class entity : entities)
				for (Property property : entity.getAttributes())
					buildAssociationForAttribute(entity, property, null, false);
		}

		private Property buildAssociationForAttribute(Class entity, Property property, String name, boolean memberOwned) {
			if (KirraHelper.isRegularProperty(property)) {
				Type propertyType = property.getType();
				if (propertyType != null && KirraHelper.isEntity(propertyType)
						&& property.getAssociation() == null) {
					Association newAssociation = (Association) entity.getNearestPackage()
							.createPackagedElement(null, UMLPackage.Literals.ASSOCIATION);
					newAssociation.setIsDerived(property.isDerived());
					newAssociation.getMemberEnds().add(property);
					// automatically created owned end
					Property otherEnd;
					if (memberOwned) {
						otherEnd = ((Class) property.getType()).createOwnedAttribute(name, entity);
						newAssociation.getMemberEnds().add(otherEnd);	
					} else {
						otherEnd = newAssociation.createOwnedEnd(name, entity);	
					}
					
					otherEnd.setIsDerived(property.isDerived());
					otherEnd.setIsReadOnly(property.isReadOnly());
					otherEnd.setLower(0);
					return otherEnd;
				}
			}
			return null;
		}
	}

	public Class deepCopy(Class source) {
		Class copy = EcoreUtil.copy(source);
		return copy;
	}

	protected void copyStereotypes(Element source, Element copy) {
		source.getStereotypeApplications().forEach(sourceStereotypeApplication -> {
			Stereotype stereotype = UMLUtil.getStereotype(sourceStereotypeApplication);
			String stereotypeName = stereotype.getQualifiedName();
			Stereotype stereotypeCopy = StereotypeUtils.findStereotype(stereotypeName);
			copy.applyStereotype(stereotypeCopy);
			System.out.println("Applying " + stereotypeName + " to " + copy.eClass().getName());
			System.out.println("Source: " + source);
			System.out.println("Target: " + copy);
			stereotype.getAllAttributes().forEach(stereotypeProperty -> {
				String propertyName = stereotypeProperty.getName();
				Object value = source.getValue(stereotype, propertyName);
				copy.setValue(stereotypeCopy, propertyName, value);
			});
			System.out.println("Stereotype applied: " + copy.isStereotypeApplied(stereotypeCopy));
		});
		EList<Element> sourceChildren = source.getOwnedElements();
		EList<Element> copyChildren = copy.getOwnedElements();
		for (int i = 0; i < sourceChildren.size(); i++)
			copyStereotypes(sourceChildren.get(i), copyChildren.get(i));
	}
}
