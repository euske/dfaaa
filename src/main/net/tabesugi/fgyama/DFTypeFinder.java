//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.w3c.dom.*;


//  DFTypeFinder
//
public class DFTypeFinder {

    private DFTypeFinder _next = null;
    private DFTypeSpace _space;

    public DFTypeFinder(DFTypeSpace space) {
        _space = space;
    }

    public DFTypeFinder(DFTypeFinder next, DFTypeSpace space) {
        _next = next;
        _space = space;
    }

    @Override
    public String toString() {
        return ("<DFTypeFinder: "+_space+" "+_next+">");
    }

    public DFClassSpace lookupClass(Name name)
        throws EntityNotFound {
        return this.lookupClass(name.getFullyQualifiedName());
    }

    public DFClassSpace lookupClass(String name)
        throws EntityNotFound {
        DFClassSpace klass;
        try {
            klass = _space.getClass(name);
        } catch (EntityNotFound e) {
            if (_next != null) {
                klass = _next.lookupClass(name);
            } else {
                throw new EntityNotFound(name);
            }
        }
        klass.load(this);
        return klass;
    }

    public DFClassSpace resolveClass(Type type)
        throws EntityNotFound {
        return this.resolveClass(resolve(type));
    }

    public DFClassSpace resolveClass(DFType type)
        throws EntityNotFound {
	if (type == null) {
	    // treat unknown class as Object.
	    return DFTypeSpace.OBJECT_CLASS;
        } else if (type instanceof DFArrayType) {
            return DFTypeSpace.ARRAY_CLASS;
	} else if (type instanceof DFClassType) {
            return ((DFClassType)type).getKlass();
        } else {
            throw new EntityNotFound(type.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public DFType resolve(Type type)
        throws EntityNotFound {
	if (type instanceof PrimitiveType) {
            PrimitiveType ptype = (PrimitiveType)type;
            return new DFBasicType(ptype.getPrimitiveTypeCode());
	} else if (type instanceof ArrayType) {
            ArrayType atype = (ArrayType)type;
	    DFType elemType = this.resolve(atype.getElementType());
	    int ndims = atype.getDimensions();
	    return new DFArrayType(elemType, ndims);
	} else if (type instanceof SimpleType) {
            SimpleType stype = (SimpleType)type;
            DFClassSpace klass = this.lookupClass(stype.getName());
            return new DFClassType(klass);
	} else if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            List<Type> args = (List<Type>) ptype.typeArguments();
            DFType baseType = this.resolve(ptype.getType());
            DFType[] argTypes = new DFType[args.size()];
            for (int i = 0; i < args.size(); i++) {
                argTypes[i] = this.resolve(args.get(i));
            }
            assert(baseType instanceof DFClassType);
            // XXX make DFCompoundType
            DFClassSpace baseKlass = ((DFClassType)baseType).getKlass();
            return new DFClassType(baseKlass, argTypes);
        } else {
            // ???
            throw new EntityNotFound(type.toString());
        }
    }

    public DFType resolve(org.apache.bcel.generic.Type type)
        throws EntityNotFound {
        if (type.equals(org.apache.bcel.generic.BasicType.BOOLEAN)) {
            return DFBasicType.BOOLEAN;
        } else if (type.equals(org.apache.bcel.generic.BasicType.BYTE)) {
            return DFBasicType.BYTE;
        } else if (type.equals(org.apache.bcel.generic.BasicType.CHAR)) {
            return DFBasicType.CHAR;
        } else if (type.equals(org.apache.bcel.generic.BasicType.DOUBLE)) {
            return DFBasicType.DOUBLE;
        } else if (type.equals(org.apache.bcel.generic.BasicType.FLOAT)) {
            return DFBasicType.FLOAT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.INT)) {
            return DFBasicType.INT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.LONG)) {
            return DFBasicType.LONG;
        } else if (type.equals(org.apache.bcel.generic.BasicType.SHORT)) {
            return DFBasicType.SHORT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.VOID)) {
            return DFBasicType.VOID;
        } else if (type instanceof org.apache.bcel.generic.ArrayType) {
            org.apache.bcel.generic.ArrayType atype =
                (org.apache.bcel.generic.ArrayType)type;
            DFType elemType = this.resolve(atype.getElementType());
            int ndims = atype.getDimensions();
            return new DFArrayType(elemType, ndims);
        } else if (type instanceof org.apache.bcel.generic.ObjectType) {
            org.apache.bcel.generic.ObjectType otype =
		(org.apache.bcel.generic.ObjectType)type;
            String className = otype.getClassName();
            DFClassSpace klass = this.lookupClass(className);
            return new DFClassType(klass);
        } else {
            // ???
            throw new EntityNotFound(type.toString());
	}
    }

    @SuppressWarnings("unchecked")
    public DFType[] resolveList(MethodDeclaration decl)
        throws EntityNotFound {
        List<DFType> types = new ArrayList<DFType>();
        for (SingleVariableDeclaration varDecl :
                 (List<SingleVariableDeclaration>) decl.parameters()) {
            types.add(this.resolve(varDecl.getType()));
        }
        DFType[] argTypes = new DFType[types.size()];
        types.toArray(argTypes);
        return argTypes;
    }
}
