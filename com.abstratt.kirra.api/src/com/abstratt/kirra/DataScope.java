package com.abstratt.kirra;

import java.util.List;

public interface DataScope extends NameScope {
	List<Property> getProperties();
	Property getProperty(String name);
}
