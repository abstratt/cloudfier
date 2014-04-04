package com.abstratt.mdd.core.target;

import java.util.Collection;
import java.util.Map;

import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;

/** Transforms a UML element into text. */
public interface ITopLevelMapper<T extends NamedElement> extends IMapper<T> {
    class MapperInfo {
        public MapperInfo(String fileName, String mapperName, int location) {
            this.fileName = fileName;
            this.mapperName = mapperName;
            this.location = location;
        }
        private String fileName;
        private String mapperName;
        private int location;
        public String getFileName() {
            return fileName;
        }
        public String getMapperName() {
            return mapperName;
        }
        public int getLocation() {
            return location;
        }
    }
    public String mapFileName(T element);
    public Collection<String> getChildMappers();
    public String applyChildMapper(String mapperName, Element element);
    public MapperInfo describeChildMapper(String mapperName);
    public Map<String, String> getFormalArgs(String mapperName);
	String getProperty(String key);
}
