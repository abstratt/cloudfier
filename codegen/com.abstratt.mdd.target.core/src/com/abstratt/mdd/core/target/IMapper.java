package com.abstratt.mdd.core.target;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.uml2.uml.Element;

/** Transforms a UML element into text. */
public interface IMapper<E extends Element> {
    public default CharSequence map(E toMap) {
    	return mapToChars(toMap);
    }
    
    public default <H extends OutputHolder<?>> H mapElement(E toMap) {
    	if (getKind() == Kind.Text)
    		return (H) new TextHolder(map(toMap));
		return (H) new BinaryHolder(mapToBytes(toMap));
    }

    public default byte[] mapToBytes(E toMap) {
    	throw new UnsupportedOperationException();
    }
    
    public default CharSequence mapToChars(E toMap) {
    	return ((TextHolder) mapElement(toMap)).get();
    }
    
    public default CharSequence mapAll(List<E> toMap) {
    	throw new UnsupportedOperationException();
    }

    default Kind getKind() {
    	return Kind.Text;
    }
    
    enum Kind {
    	Text,
    	Binary
    }
    

    class TextHolder extends OutputHolder<CharSequence> {
    	public TextHolder(CharSequence seq) {
    		super(seq);
    	}
    	@Override
    	public byte[] getBytes() {
    		return get().toString().getBytes(StandardCharsets.UTF_8);
    	}
    	@Override
    	public CharSequence getChars() {
    		return get();
    	}
    }
    
    class BinaryHolder extends OutputHolder<byte[]> {
    	public BinaryHolder(byte[] bytes) {
    		super(bytes);
    	}
    	@Override
    	public byte[] getBytes() {
    		return get();
    	}
    	@Override
    	public CharSequence getChars() {
    		return new String(get(), StandardCharsets.UTF_8);
    	}
    }
}
