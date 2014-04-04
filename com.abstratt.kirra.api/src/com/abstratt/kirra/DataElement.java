package com.abstratt.kirra;

public abstract class DataElement extends TypedElement<DataScope> {
	private static final long serialVersionUID = 1L;
	private boolean showInMasterTable;
	private boolean showInChildTable;
	protected boolean derived;
	private boolean initializable;
	private boolean editable;
	public boolean isShowInMasterTable() { 
		return showInMasterTable;
	}
	public void setShowInMasterTable(boolean showInMasterTable) {
		this.showInMasterTable = showInMasterTable;
	}
	public boolean isShowInChildTable() {
		return showInChildTable;
	}
	public void setShowInChildTable(boolean showInChildTable) {
		this.showInChildTable = showInChildTable;
	}
	public boolean isDerived() {
		return derived;
	}
	public void setDerived(boolean derived) {
		this.derived = derived;
	}
	
	@Override
	public String toString() {
		return this.owner + "->" + super.toString();
	}

	public boolean isInitializable() {
		return initializable;
	}
	public void setInitializable(boolean initializable) {
		this.initializable = initializable;
	}
	public boolean isEditable() {
		return editable;
	}
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
}
