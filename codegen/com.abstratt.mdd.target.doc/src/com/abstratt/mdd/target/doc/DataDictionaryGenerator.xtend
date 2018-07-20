package com.abstratt.mdd.target.doc

import com.abstratt.kirra.mdd.core.KirraHelper
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.kirra.mdd.target.base.AbstractGenerator
import com.abstratt.mdd.core.IRepository
import com.abstratt.mdd.frontend.textuml.renderer.ActivityRenderer
import com.abstratt.mdd.modelrenderer.IndentedPrintWriter
import com.google.common.base.Function
import java.io.ByteArrayOutputStream
import java.util.Set
import org.apache.commons.lang3.StringUtils
import org.eclipse.emf.common.util.EList
import org.eclipse.uml2.uml.Activity
import org.eclipse.uml2.uml.Behavior
import org.eclipse.uml2.uml.CallEvent
import org.eclipse.uml2.uml.Class
import org.eclipse.uml2.uml.Classifier
import org.eclipse.uml2.uml.Constraint
import org.eclipse.uml2.uml.Element
import org.eclipse.uml2.uml.Enumeration
import org.eclipse.uml2.uml.Event
import org.eclipse.uml2.uml.NamedElement
import org.eclipse.uml2.uml.Package
import org.eclipse.uml2.uml.Property
import org.eclipse.uml2.uml.SignalEvent
import org.eclipse.uml2.uml.StateMachine
import org.eclipse.uml2.uml.Trigger
import org.eclipse.uml2.uml.Type

import static extension com.abstratt.kirra.mdd.core.KirraHelper.*
import static extension com.abstratt.mdd.core.util.ActivityUtils.*
import static extension com.abstratt.mdd.core.util.FeatureUtils.*
import static extension com.abstratt.mdd.core.util.MDDExtensionUtils.*
import static extension com.abstratt.mdd.core.util.StateMachineUtils.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier
import com.abstratt.mdd.frontend.textuml.renderer.TextUMLRenderingUtils
import org.eclipse.uml2.uml.ValueSpecification
import com.abstratt.mdd.frontend.textuml.renderer.ActivityGenerator

class DataDictionaryGenerator extends AbstractGenerator {
	private static final String YES = "\u2714"
	private static final String NO = "-"
	
	private boolean showDiagrams
    
    new(IRepository repository, boolean showDiagrams) {
        super(repository)
        this.showDiagrams = showDiagrams
    }

    new(IRepository repository) {
        this(repository, false)
    }

    
    def CharSequence generate() {
        val entities = this.entities
        val testClasses = repository.getTopLevelPackages(null).map[ownedTypes.filter[it.testClass]].flatten().map[it as Class].toSet
        val entityPackages = entities.map[package].toSet
        val applicationLabel = KirraHelper.getApplicationLabel(repository)
        val localStyle = Boolean.parseBoolean(repository.properties.computeIfAbsent("mdd.doc.localStylesheet", ["false"]).toString)
        '''
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          «generateBootstrapLinks(localStyle)»
          <title>«applicationLabel» - Data Dictionary</title>
        </head>
        <body>
        <div class="container-fluid">
        <h1>«applicationLabel»</h1>
        «generateRow[entityPackages.generatePackageIndex()]»
        «entityPackages.generateMany[ appPackage | generateRow[generateEntityIndex(appPackage)]]»
        <h2>Entities</h2>
        «entities.generateMany[ entity |
            '''
            «generateRow[generateEntity(entity)]»
            '''
        ]»
        «IF !enumerations.isEmpty»
        <h2>Enumerations</h2>
        «enumerations.generateMany[ enumeration |
            '''
            «generateRow[generateEnumeration(enumeration)]»
            '''
        ]»
        «ENDIF»
        «IF !stateMachines.isEmpty»
        <h2>State machines</h2>
        «stateMachines.generateMany[ stateMachine |
            '''
            «generateRow[generateStateMachine(stateMachine)]»
            '''
        ]»
        «ENDIF»
        «IF !testClasses.empty»
        <h2>Scenarios</h2>
        «testClasses.generateMany[ testClass |
            '''
            «generateRow[generateTestClass(testClass)]»
            '''
        ]»
        «ENDIF»
        </div>
        </body>
        </html>
        '''
    }
    
    def CharSequence generatePackageIndex(Set<Package> packages) {
        '''
        
        <div>
        <h2>Packages</h2>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th>Namespace</th>
                    <th>Entities</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                «packages.generateMany[package_ | '''
                <tr>
                    <td>«generateLink(package_)»</a></td>
                    <td>
                    «package_.ownedTypes.filter[entity].sortBy[it.name].generateMany([ type | 
                    	'''
                    	«type.generateLink»
                    	'''
                    ],', ')»
                    </td>
                    <td>«IF package_.description.empty»-«ELSE»«package_.description»«ENDIF»</td>
                </tr>
                ''']»
            </tbody>
        </table>
        </div>
        '''
    }
	def CharSequence generateLink(NamedElement element) {
		generateLink(element, false)
	}
	def CharSequence generateLink(NamedElement element, boolean qualified) {
		if (element == null)
			'null'
		else if (element.nearestPackage.isLibrary) {
			element.name
		} else
			'''
				<a href="#«element.qualifiedName»">«getLabel(element)»«if (qualified) ''' («element.qualifiedName»)'''»</a>
			'''.toString.trim
	}
    
    def CharSequence generateRow(Function<Void, CharSequence> wrapped) {
        '''
        <div class="row">
        <div class="col-sm-9 col-md-12 main">
        «wrapped.apply(null)»
        </div>
        </div>
        '''
    }
    
    def generateBootstrapLinks(boolean local) {
    	val urlPrefix = if (local) "./" else "https://"  
        '''
        <script   src="«urlPrefix»code.jquery.com/jquery-1.12.3.min.js"   integrity="sha256-aaODHAgvwQW1bFOGXMeX+pC4PZIPsvn2h1sArYOhgXQ="   crossorigin="anonymous"></script>  
        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="«urlPrefix»maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">
        <!-- Optional theme -->
        <link rel="stylesheet" href="«urlPrefix»maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">
        <!-- Latest compiled and minified JavaScript -->
        <script src="«urlPrefix»maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>
        '''
    }
    
    def CharSequence generateEntityIndex(Package appPackage) {
        
        val entities = appPackage.ownedTypes.filter[entity]
        '''
        <div>
        <a name="«appPackage.qualifiedName»"></a>
        <h3>Package: «getLabel(appPackage)»</h3>
        <h5>(«appPackage.qualifiedName»)</h5>
        
        «IF !appPackage.description.empty»<blockquote>«appPackage.description»</blockquote>«ENDIF»
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th>Entity</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                «entities.generateMany[entity | '''
                <tr>
                    <td>«entity.generateLink»</a></td>
                    <td>«entity.description»</td>
                </tr>
                ''']»
            </tbody>
        </table>
        «IF showDiagrams»
        <h4>Class diagram</h4>
        <img src="«appPackage.toJavaPackage»-classes.png"></img>
        <h4>State diagram</h4>
        <img src="«appPackage.toJavaPackage»-state.png"></img>
        «ENDIF»
        </div>
        '''
    }
    
    def CharSequence generateEnumeration(Enumeration enumeration) {
    	'''
        «generateSectionHeader("Enumeration", enumeration)»
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="20%">Name</th>
                    <th width="80%">Description</th>
                </tr>
            </thead>
            <tbody>
                «enumeration.ownedLiterals.generateMany[literal | '''
                <tr>
                    <td>«literal.asLabel»</td>
                    <td>«IF literal.description.empty»-«ELSE»«literal.description»«ENDIF»</td>
                </tr>
                ''']»
            </tbody>
        </table>
    	'''
    }

    def CharSequence generateStateMachine(StateMachine stateMachine) {
    	'''
        «generateSectionHeader('''State machine in <b>«stateMachine.context.asLabel»</b>''', stateMachine)»
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Name</th>
                    <th width="40%">Description</th>
                    <th width="50%">Transitions</th>                    
                </tr>
            </thead>
            <tbody>
                «stateMachine.states.generateMany[state | '''
                <tr>
                    <td>«state.asLabel»</td>
                    <td>«IF state.description.empty»-«ELSE»«state.description»«ENDIF»</td>
                    <td>
                        <ul>
                    «FOR o : state.outgoings»
                        <li>
                        <p><em>To:</em> <strong>«o.target.asLabel»</strong></p>
                        <p>
                        <em>When: </em>«o.triggers.generateMany[generateTrigger(it)]»
                        </p>
                        «IF o.guard != null»
                        <p>
                        <em>If: </em>
                        «generateConstraints("", #[o.guard])»
                        </p>
                        «ENDIF»
                    «ENDFOR»
                        </ul>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
    	'''
    }
	
	def CharSequence generateTrigger(Trigger trigger) {
		'''«generateEvent(trigger.event)»'''
	}
	
	def dispatch CharSequence generateEvent(Event event) {
		'''Unexpected event type'«event.eClass.name»' '''
	}
	def dispatch CharSequence generateEvent(CallEvent event) {
		'''action '«event.operation.asLabel»' is called'''
	}
	def dispatch CharSequence generateEvent(SignalEvent event) {
		'''signal '«event.signal.asLabel»' is received'''
	}
	


    def CharSequence generateEntity(Class entity) {
    	val entityRelationships = entity.entityRelationships.filter[it.public]
    	val entityProperties = entity.properties.filter[it.public]
    	val entityActions = entity.actions.filter[it.public]
    	val entityQueries = entity.queries.filter[it.public]
    	val stateMachine = entity.findStateProperties().head?.type as StateMachine
    	
        '''
        «generateSectionHeader("Entity", entity)»
        «IF !entityProperties.empty»
        <h4>Properties</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Property</th>
                    <th width="10%">Type</th>
                    <th width="10%">Required</th>
                    <th width="10%">Initializable</th>
                    <th width="10%">Editable</th>
                    <th width="50%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityProperties.generateMany[property | '''
                <tr>
                    <td>«property.asLabel»</td>
                    <td>«property.type.generateLink»</a></td>
                    <td>«if (property.required) YES else NO»</a></td>
                    <td>«if (property.initializable) YES else NO»</a></td>
                    <td>«if (property.editable) YES else NO»</a></td>
                    <td>
                    <table>
                    <tr><td>
                    «property.generateDescription»
                    «property.type.enumerateLiterals»
                    </td></tr>
                    «IF property.derived»
                    <tr><th>
                    Computed value
                    </th></tr>
                    <tr><td>
                    «generateBehavior([generateDerivationAsPseudoCode(property)], [generateDerivationAsTextUML(property)])»
                    </td></tr>
                    «ENDIF»
                    «generateConstraints("Invariants", property.findInvariantConstraints)»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityRelationships.empty»
        <h4>Relationships</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Relationship</th>
                    <th width="20%">Related Entity</th>
                    <th width="5%">Required</th>
                    <th width="5%">Multiple</th>
                    <th width="5%">Navigable</th>
                    <th width="55%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityRelationships.generateMany[relationship | '''
                <tr>
                    <td>«relationship.asLabel»</td>
                    <td>«relationship.type.generateLink»</a></td>
                    <td>«if (relationship.required) YES else NO»</a></td>
                    <td>«if (relationship.multiple) YES else NO»</a></td>
                    <td>«if (relationship.navigable) YES else NO»</a></td>
                    <td>
                    <table>
                    <tr><td>
                    «relationship.generateDescription»
                    </td</tr>
                    «generateConstraints("Invariants", relationship.findInvariantConstraints)»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityActions.empty»
        <h4>Actions</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Action</th>
                    <th width="20%">Parameters</th>
                    <th width="70%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityActions.generateMany[action | '''
                <tr>
                    <td>«action.asLabel»</td>
                    <td>«IF action.parameters.inputParameters.empty»-«ELSE»«action.parameters.inputParameters.generateMany(['''<p>«it.asLabel» («type.name»)</p>'''])»«ENDIF»</td>
                    <td>
                    <table>
                    <tr><td>
                    «action.generateDescription»
                    </td></tr>
                    «IF stateMachine != null && !stateMachine.findStatesForCalling(action).empty»
                    <tr><th>
                    Valid state(s)
                    </th></tr>
                    <tr><td>
                    «stateMachine.findStatesForCalling(action).map[state | state.name].join(', ')»
                    </td></tr>
                    «ENDIF»
                    «IF !action.methods.empty && action.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    «generateBehavior([generateActivityAsPseudoCode(action.methods.filter(Activity).head)], [generateActivityAsTextUML(action.methods.filter(Activity).head)])»
                    </td></tr>
                    «ENDIF»
                    «generateConstraints("Preconditions", action.preconditions)»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF !entityQueries.empty»
        <h4>Queries</h4>
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="10%">Query</th>
                    <th width="20%">Parameters</th>
                    <th width="70%">Description</th>
                </tr>
            </thead>
            <tbody>
                «entityQueries.generateMany[query | '''
                <tr>
                    <td>«query.asLabel»</td>
                    <td>«IF query.parameters.inputParameters.empty»-«ELSE»«query.parameters.inputParameters.generateMany(['''<p>«name» («type.name»)</p>'''])»«ENDIF»</td>
                    <td>
                    <table>
                    <tr><td>
                    «query.generateDescription»
                    </td></tr>
                    «IF !query.methods.empty && query.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    <pre>
«query.methods.generateMany[ behavior |
    generateActivityAsPseudoCode(behavior)
].toString.trim()»
                    </pre>
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        «IF entityProperties.empty && entityRelationships.empty && entityActions.empty && entityQueries.empty»-«ENDIF»
        '''
    }
				
	protected def CharSequence generateActivityAsPseudoCode(Behavior behavior) {
		new PseudoCodeActivityGenerator().generateActivity(behavior as Activity)
	}
	
	protected def CharSequence generateDerivationAsPseudoCode(Property property) {
		new PseudoCodeActivityGenerator().generateDerivation(property)
	}
	
	protected def CharSequence generateDerivationAsTextUML(Property attribute) {
        val baseValue = if (attribute.defaultValue != null)
            if (attribute.defaultValue.behaviorReference)
                (attribute.defaultValue.resolveBehaviorReference as Activity).generateActivityAsTextUML(true)
            else '''«attribute.defaultValue.generateValueAsTextUML»'''
        else '''«TextUMLRenderingUtils.generateDefaultValue(attribute.type)»'''
        return baseValue
    }
		
    protected def CharSequence generateActivityAsTextUML(Behavior behavior) {
    	behavior.generateActivityAsTextUML(false)
    }
	protected def CharSequence generateActivityAsTextUML(Behavior behavior, boolean asExpression) {
		val generator = new ActivityGenerator()
		return if (asExpression) 
			generator.generateActivityAsExpression(behavior as Activity)
		else
			generator.generateActivity(behavior as Activity)
	}
	
	protected def CharSequence generateValueAsTextUML(ValueSpecification value) {
		return TextUMLRenderingUtils.renderValue(value)
	}

	protected def CharSequence generateConstraintAsPseudoCode(Constraint constraint) {
		new PseudoCodeActivityGenerator().generateConstraint(constraint)
	}
	
	def CharSequence generateConstraints(String title, Iterable<Constraint> constraints) {
		var index = new AtomicInteger()
		var prefix = UUID.randomUUID.toString
	'''
	«FOR constraint : constraints»
	<tr><th>
	«title»
	</th></tr>
	<tr><td>
	«constraint.description»
	«generateBehavior([generateConstraintAsPseudoCode(constraint)], [generateConstraintAsTextUML(constraint)])»
	</td></tr>
	«ENDFOR»
	'''
    }
    
    def CharSequence generateActivity(String title, Iterable<Behavior> activities) {
		var index = new AtomicInteger()
		var prefix = UUID.randomUUID.toString
	'''
	«FOR activity : activities»
	<tr><th>
	«title»
	</th></tr>
	<tr><td>
	«activity.description»
	«generateBehavior([generateActivityAsPseudoCode(activity)], [generateActivityAsTextUML(activity)])»
	</td></tr>
	«ENDFOR»
	'''
    }
    
    def CharSequence generateBehavior(Supplier<CharSequence> pseudocode, Supplier<CharSequence> textuml) {
    	val String prefix = UUID.randomUUID.toString
    	'''
    <div class="panel-group" id="«prefix»-accordion" role="tablist" aria-multiselectable="true">
      <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="«prefix»-textuml-heading">
          <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#«prefix»-accordion" href="#«prefix»-textuml-content" aria-expanded="true">
            Specification
            </a>
          </h4>
        </div>
        <div id="«prefix»-textuml-content" class="panel-collapse collapse in" role="tabpanel">
          <div class="panel-body">
              <pre>
«textuml.get»
              </pre>              
          </div>
        </div>
        <div class="panel-heading" role="tab" id="«prefix»-pseudocode-heading">
          <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#«prefix»-accordion" href="#«prefix»-pseudocode-content">
            Pseudocode (experimental)
            </a>
          </h4>
          </div>
          <div id="«prefix»-pseudocode-content" class="panel-collapse collapse" role="tabpanel">
            <div class="panel-body">
                <pre>
  «pseudocode.get»
                </pre>              
            </div>
          </div>
        </div>
      </div>
    </div>
        '''
    }
	
	def generateConstraintAsTextUML(Constraint constraint) {
        val activity = constraint.specification.resolveBehaviorReference as Activity
		val generator = new ActivityGenerator()
		return generator.generateActivityAsExpression(activity)
	}
				
	def dispatch CharSequence enumerateLiterals(Enumeration enumeration)  
	'''
	<p>
	«enumeration.ownedLiterals.generateMany([it.asLabel], ", ")»
	</p>
	'''
	
	def dispatch CharSequence enumerateLiterals(Type enumeration) {
		''
	}
    
    def CharSequence generateTestClass(Class testClass) {
    	val testCases = testClass.operations.filter[testCase]
        '''
        «IF !testCases.empty»
        «generateSectionHeader("Test class", testClass)»
        <table class="table">
            <thead class="thead-inverse">
                <tr>
                    <th width="25%">Test case</th>
                    <th width="75%">Description</th>
                </tr>
            </thead>
            <tbody>
                «testCases.generateMany[testCase | '''
                <tr>
                    <td>«testCase.asLabel»</td>
                    <td>
                    <table>
                    <tr><td>
                    «testCase.generateDescription»
                    </td></tr>
                    «IF !testCase.methods.empty && testCase.methods.filter(Activity).exists[!it.findStatements.empty]»
                    <tr><th>
                    Behavior
                    </th></tr>
                    <tr><td>
                    «generateBehavior([generateActivityAsPseudoCode(testCase.methods.head)], [generateActivityAsTextUML(testCase.methods.head as Activity)])»
                    </td></tr>
                    «ENDIF»
                    </table>
                    </td>
                </tr>
                ''']»
            </tbody>
        </table>
        «ENDIF»
        '''
    }
	
	
	protected def CharSequence generateSectionHeader(String sectionName, Classifier classifier)
		'''
        <a name="«classifier.qualifiedName»"></a>
        <h3>«getLabel(classifier)»</h3>
        <h5>«sectionName» from <strong>«generateLink(classifier.nearestPackage, true)»</strong></h5>
        <hr>
        «IF !StringUtils.isBlank(classifier.description)»<blockquote>«classifier.description»</blockquote>«ENDIF»
        '''
    
    def CharSequence generateDescription(Element element) {
        val description = element.description
        if (description?.length > 0) description else '-' 
    }
    
    def String asLabel(NamedElement element) {
        KirraHelper.getLabel(element)
    }
    
}