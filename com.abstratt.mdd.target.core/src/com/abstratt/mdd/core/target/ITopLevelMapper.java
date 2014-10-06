package com.abstratt.mdd.core.target;

import org.eclipse.uml2.uml.NamedElement;

/** Transforms a UML element into text. */
public interface ITopLevelMapper<T extends NamedElement> extends IMapper<T> {
    class MapperInfo {
        private String fileName;
        private String mapperName;
        private int location;

        public MapperInfo(String fileName, String mapperName, int location) {
            this.fileName = fileName;
            this.mapperName = mapperName;
            this.location = location;
        }

        public String getFileName() {
            return fileName;
        }

        public int getLocation() {
            return location;
        }

        public String getMapperName() {
            return mapperName;
        }
    }

//    public String applyChildMapper(String mapperName, Element element);

//    public MapperInfo describeChildMapper(String mapperName);

//    public Collection<String> getChildMappers();

//    public Map<String, String> getFormalArgs(String mapperName);

    public String mapFileName(T element);

//    String getProperty(String key);
}
