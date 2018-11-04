//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFParamType
//
public class DFParamType extends DFKlass {

    public DFParamType(
        String name, DFTypeSpace typeSpace) {
        super(name, typeSpace, null, null, DFRootTypeSpace.getObjectKlass());
        assert typeSpace != null;
    }

    @Override
    public String toString() {
        return ("<DFParamType("+this.getFullName()+":"+_baseKlass+")>");
    }

    public int canConvertFrom(DFType type)
    {
        return _baseKlass.canConvertFrom(type);
    }

    public DFType parameterize(Map<DFParamType, DFType> typeMap) {
        if (typeMap.containsKey(this)) {
            return typeMap.get(this);
        } else {
            return this;
        }
    }

    @Override
    public String getTypeName() {
        return _name+":"+_baseKlass.getTypeName();
    }

    @SuppressWarnings("unchecked")
    public void build(DFTypeFinder finder, TypeParameter typeParam)
        throws TypeNotFound {
        try {
            List<Type> bounds = typeParam.typeBounds();
            if (0 < bounds.size()) {
                _baseIfaces = new DFKlass[bounds.size()-1];
                for (int i = 0; i < bounds.size(); i++) {
                    DFKlass klass = finder.resolveKlass(bounds.get(i));
                    //Logger.info("DFParamType.build: "+this+": "+klass);
                    if (i == 0) {
                        _baseKlass = klass;
                    } else {
                        _baseIfaces[i-1] = klass;
                    }
                    finder = klass.addFinders(finder);
                }
            }
        } catch (TypeNotFound e) {
            e.setAst(typeParam);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public void build(DFTypeFinder finder, DFKlass klass)
        throws TypeNotFound {
	_baseKlass = klass;
    }
}
