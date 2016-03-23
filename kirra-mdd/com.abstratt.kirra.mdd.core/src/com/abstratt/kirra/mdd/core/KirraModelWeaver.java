package com.abstratt.kirra.mdd.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.query.conditions.eobjects.EObjectCondition;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.BehavioredClassifier;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Port;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.VisibilityKind;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.isv.IModelWeaver;
import com.abstratt.mdd.core.util.ConnectorUtils;
import com.abstratt.mdd.core.util.MDDExtensionUtils;
import com.abstratt.mdd.core.util.StereotypeUtils;

/**
 * A model weaver for turning plain UML models into Kirra-compatible models.
 */
public class KirraModelWeaver implements IModelWeaver {

	public void packageCreated(IRepository repository, Package created) {
		Profile kirraProfile = (Profile) repository.loadPackage(URI.createURI(KirraMDDCore.KIRRA_URI));
		Package types = repository.findPackage(IRepository.TYPES_NAMESPACE, null);
		Package extensions = repository.findPackage(IRepository.EXTENSIONS_NAMESPACE, null);
		created.applyProfile(kirraProfile);
		created.createPackageImport(types);
		created.createPackageImport(extensions);
		created.createPackageImport(kirraProfile);
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
		private Stereotype entityStereotype;
		private Stereotype serviceStereotype;
		private Stereotype actionStereotype;
		private Stereotype finderStereotype;
		private Stereotype essentialStereotype;
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
			entityStereotype = repository.findNamedElement("kirra::Entity", Literals.STEREOTYPE, null);
			serviceStereotype = repository.findNamedElement("kirra::Service", Literals.STEREOTYPE, null);
			actionStereotype = repository.findNamedElement("kirra::Action", Literals.STEREOTYPE, null);
			finderStereotype = repository.findNamedElement("kirra::Finder", Literals.STEREOTYPE, null);
			essentialStereotype = repository.findNamedElement("kirra::Essential", Literals.STEREOTYPE, null);

			baseObject = repository.findNamedElement("mdd_types::Object", Literals.CLASS, null);
			enabled = !(baseObject == null || roleStereotype == null || entityStereotype == null
					|| actionStereotype == null || finderStereotype == null || essentialStereotype == null);

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
			configureEntities();
			enhanceUserEntities();
			buildAssociations();
		}

		private void configureEntities() {
			// apply entity stereotype
			for (Class entity : entities)
				StereotypeUtils.safeApplyStereotype(entity, entityStereotype);
			for (Class entity : entities) {
				// apply operation stereotypes
				for (Operation operation : entity.getOperations()) {
					Type returnType = operation.getType();
					if (returnType != null && operation.isStatic() && operation.isQuery())
						StereotypeUtils.safeApplyStereotype(operation, finderStereotype);
					else if (!operation.isQuery() && VisibilityKind.PUBLIC_LITERAL == operation.getVisibility())
						StereotypeUtils.safeApplyStereotype(operation, actionStereotype);
				}
				for (Property property : entity.getAttributes())
					if (property.getName() != null && property.getType() != null && !property.getName().startsWith("_")
							&& property.getUpper() == 1 && property.getLower() == 1)
						// only properties that are required, single-valued and
						// do not start with '_' are marked as essential
						StereotypeUtils.safeApplyStereotype(property, essentialStereotype);
			}
		}

		private void enhanceUserEntities() {
			Class userTemplate = repository.findNamedElement("kirra_templates::UserTemplate", UMLPackage.Literals.CLASS, null);
			List<Class> roleEntities = entities.stream().filter(it -> MDDExtensionUtils.isRoleClass(it)).collect(Collectors.toList());
			if (roleEntities.isEmpty())
				return;
			Package roleClassPackage = roleEntities.get(0).getPackage();
			Class userClass = EcoreUtil.copy(userTemplate);
			userClass.setName("User");
			roleClassPackage.getPackagedElements().add(userClass);
			StereotypeUtils.safeApplyStereotype(userClass, entityStereotype);
			roleEntities.forEach(it -> {
				Property userInfoReference = it.createOwnedAttribute("user", userClass);
				userInfoReference.setIsReadOnly(true);
				userInfoReference.setLower(0);
			});
		}

		private void buildAssociations() {
			// ensure properties that refer to entities are part of associations
			// (just like references)
			for (Class entity : entities)
				for (Property property : entity.getAttributes())
					buildAssociationForAttribute(entity, property);
		}

		private void buildAssociationForAttribute(Class entity, Property property) {
			if (KirraHelper.isRegularProperty(property)) {
				Type propertyType = property.getType();
				if (propertyType != null && propertyType.isStereotypeApplied(entityStereotype)
						&& property.getAssociation() == null) {
					Association newAssociation = (Association) entity.getNearestPackage()
							.createPackagedElement(null, UMLPackage.Literals.ASSOCIATION);
					newAssociation.setIsDerived(property.isDerived());
					newAssociation.getMemberEnds().add(property);
					// automatically created owned end
					Property otherEnd = newAssociation.createOwnedEnd(null, entity);
					otherEnd.setIsDerived(property.isDerived());
					otherEnd.setLower(0);
					otherEnd.setIsNavigable(false);
				}
			}
		}
	}
}
