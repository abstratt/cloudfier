package com.abstratt.kirra;

import com.abstratt.kirra.TypeRef.TypeKind;

public interface NameScope {
	TypeRef getTypeRef();
	TypeKind getTypeKind();
}
