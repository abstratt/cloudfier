package com.abstratt.mdd.internal.target.pojo;

import java.util.List;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.CreateLinkAction;
import org.eclipse.uml2.uml.LinkEndCreationData;
import org.eclipse.uml2.uml.LinkEndData;
import org.eclipse.uml2.uml.Property;

import com.abstratt.mdd.core.target.IActionMapper;
import com.abstratt.mdd.core.target.IMappingContext;
import com.abstratt.mdd.core.util.ActivityUtils;

public class CreateLinkActionMapping implements IActionMapper<CreateLinkAction> {

	/**
	 * link <association> (role1 := value1, role2 := value2);
	 * <p>
	 * Need to check ends' cardinality and navigability.
	 * </p>
	 * <ol>
	 * <li>if navigable, that end will have a reference to the other end.</li>
	 * <li>if is multiple, the end will add the reference to a collection, otherwise it will just assign it to an attribute.</li> 
	 * </ol>
	 * 
	 */
	public String map(CreateLinkAction cla, IMappingContext context) {
		StringBuffer result = new StringBuffer();
		final Association association = cla.association();
		//XXX ticket:75 - n-ary associations not supported
		assert association.isBinary();
		List<LinkEndData> endData = cla.getEndData();
		String[] values = new String[endData.size()];
		for (int i = 0; i < endData.size(); i++) {
			LinkEndCreationData thisEndData = (LinkEndCreationData) endData.get(i);
			Action  target = (Action) ActivityUtils.getSource(thisEndData.getValue()).getOwner();
	        values[i] = context.map(target);
		}
		
		for (int i = 0; i < endData.size(); i++) {
			LinkEndCreationData thisEndData = (LinkEndCreationData) endData.get(i);
			final Property thisEnd = thisEndData.getEnd();
			if (!thisEnd.isNavigable())
				continue;
			Property otherEnd = thisEnd.getOtherEnd();
	        result.append(values[i]);
	        result.append(".");
	        result.append(otherEnd.getName());
			if (otherEnd.isMultivalued()) {
				result.append(".add(");
				result.append(values[1 - i]);
				result.append(")");
			} else {
				result.append(" = ");
				result.append(values[1 - i]);
			}
			result.append("; ");
		}
		result.deleteCharAt(result.length() - 2);
		return result.toString();
	}
	
}
