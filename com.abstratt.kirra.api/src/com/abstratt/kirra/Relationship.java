package com.abstratt.kirra;

public class Relationship extends DataElement {
	private static final long serialVersionUID = 1L;
	public enum Style {
		/** This relationship refers to the parent object (typically one non-null at most).*/
		PARENT, 
		/** This relationship refers to one or more child objects.*/
		CHILD,
		/** This relationship refers to one or more related objects which are not children of this class.*/
		LINK;
	}
	private Style style;
	private boolean visible;
	private boolean primary;
	private String opposite;
	private boolean navigable;
	private String associationName;
	
	public boolean isPrimary() {
		return primary;
	}
	
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
	
	public void setAssociationName(String associationName) {
		this.associationName = associationName;
	}
	public String getAssociationName() {
		return associationName;
	}
	public boolean isNavigable() {
		return navigable;
	}

	public void setNavigable(boolean navigable) {
		this.navigable = navigable;
	}

	public Style getStyle() {
		return style;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void setOpposite(String opposite) {
		this.opposite = opposite;
	}
	
	/**
	 * Returns the name of the opposite relationship, if available.
	 * 
	 * @return the name of the opposite relationship, or <code>null</code>
	 */
	public String getOpposite() {
		return opposite;
	}
}
