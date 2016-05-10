package com.abstratt.mdd.core.target;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.uml2.uml.NamedElement;

import com.abstratt.mdd.core.IRepository;
import com.abstratt.mdd.core.target.IMapper.BinaryHolder;
import com.abstratt.mdd.core.target.IMapper.Kind;
import com.abstratt.mdd.core.target.IMapper.TextHolder;

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
    
    default boolean canMap(T element) {
    	return false;
    }

    default String mapFileName(T element) {
    	throw new UnsupportedOperationException();
    }
    
    public default Map<String, CharSequence> mapAll(IRepository repo) {
    	if (getKind() == Kind.Text)
    		return mapAllAsChars(repo);
    	throw new UnsupportedOperationException();
    }
    
    public default Map<String, byte[]> mapAllAsBytes(IRepository repo) {
    	Map<String, byte[]> result = new LinkedHashMap<>();
    	mapAll(repo).entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().toString().getBytes(StandardCharsets.UTF_8)));
		return result; 
    }
    
    public default Map<String, CharSequence> mapAllAsChars(IRepository repo) {
    	Map<String, CharSequence> result = new LinkedHashMap<>();
    	mapAllAsBytes(repo).entrySet().forEach(entry -> result.put(entry.getKey(), new String(entry.getValue(), StandardCharsets.UTF_8)));
		return result; 
    }
    
    public default Map<String, OutputHolder<?>> mapMultiple(IRepository repo) {
    	Map<String, OutputHolder<?>> result = new LinkedHashMap<>();
    	if (getKind() == Kind.Text)
    		mapAllAsChars(repo).entrySet().forEach(it -> result.put(it.getKey(), new TextHolder(it.getValue())));
    	else
    		mapAllAsBytes(repo).entrySet().forEach(it -> result.put(it.getKey(), new BinaryHolder(it.getValue())));
		return result;

    }
}
