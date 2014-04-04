package com.abstratt.mdd.core.target;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Operation;
import org.eclipse.uml2.uml.Type;

/**
 * Implementations of this interface define how to map action semantics to a
 * specific target platform.
 * 
 * @deprecated implement {@link ITopLevelMapper} or {@link IMapper} instead
 */
@Deprecated
public interface ILanguageMapper extends ITopLevelMapper {
    
	public String mapBehavior(Operation toMap);
	
	/**
	 * Returns null if nothing to produce. 
	 */
	public String map(Classifier toMap);
	
	public String mapFileName(Classifier toMap);

	public String mapTypeReference(Type type);
}
