//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.w3c.dom.*;


//  DFTypeSpace
//
public class DFTypeSpace {

    private DFTypeSpace _root;
    private String _name;
    private DFTypeSpace _parent;

    private List<DFTypeSpace> _children =
	new ArrayList<DFTypeSpace>();
    private Map<String, DFTypeSpace> _id2space =
	new HashMap<String, DFTypeSpace>();
    private Map<String, DFClassSpace> _id2klass =
	new HashMap<String, DFClassSpace>();

    public DFTypeSpace() {
        _root = this;
	_name = ".";
        _parent = null;
    }

    public DFTypeSpace(DFTypeSpace parent, String name) {
        _root = parent._root;
	_name = name;
	_parent = parent;
    }

    public DFTypeSpace(DFTypeSpace space) {
        _root = space._root;
        _name = space._name;
        _parent = space._parent;
        _children = new ArrayList<DFTypeSpace>(space._children);
        _id2space = new HashMap<String, DFTypeSpace>(space._id2space);
        _id2klass = new HashMap<String, DFClassSpace>(space._id2klass);
    }

    @Override
    public String toString() {
	return ("<DFTypeSpace("+this.getFullName()+")>");
    }

    public String getName() {
        return _name;
    }
    public String getFullName() {
        if (_parent == null) {
            return _name;
        } else if (_parent == _root) {
            return "."+_name;
        } else {
            return _parent.getFullName()+"."+_name;
        }
    }

    public DFTypeSpace addAnonChild() {
        String id = "anon"+_children.size();
        return this.addChild(id);
    }
    private DFTypeSpace addChild(String id) {
        Utils.logit("DFTypeSpace.addChild: "+this+": "+id);
        DFTypeSpace space = new DFTypeSpace(this, id);
        _children.add(space);
        _id2space.put(id, space);
        return space;
    }

    public DFTypeSpace lookupSpace(SimpleName name) {
        String id = name.getIdentifier();
        DFTypeSpace space = _id2space.get(id);
        if (space == null) {
            space = this.addChild(id);
        }
        return space;
    }

    public DFTypeSpace lookupSpace(Name name) {
        if (name.isQualifiedName()) {
	    QualifiedName qname = (QualifiedName)name;
	    DFTypeSpace parent = (_parent != null)? _parent : this;
	    parent = parent.lookupSpace(qname.getQualifier());
            return parent.lookupSpace(qname.getName());
        } else {
            return this.lookupSpace((SimpleName)name);
        }
    }

    public DFTypeSpace lookupSpace(PackageDeclaration pkgDecl) {
        if (pkgDecl == null) {
            return this;
        } else {
            return this.lookupSpace(pkgDecl.getName());
        }
    }

    private DFClassSpace getDefaultClass(String id) {
        DFClassSpace klass = _id2klass.get(id);
        if (klass != null) {
            return klass;
        } else {
            return this.addClass(id);
        }
    }

    private DFClassSpace getDefaultClass(Name name) {
        if (name.isQualifiedName()) {
	    QualifiedName qname = (QualifiedName)name;
            DFTypeSpace parent = this.lookupSpace(qname.getQualifier());
            return parent.getDefaultClass(qname.getName());
        } else {
	    SimpleName sname = (SimpleName)name;
            return this.getDefaultClass(sname.getIdentifier());
        }
    }

    private DFClassSpace getDefaultClass(DFType type) {
	if (type == null) {
	    return this.getDefaultClass("unknown");
	} else {
	    return this.getDefaultClass(type.getName());
	}
    }

    private DFClassSpace addClass(String id) {
	DFClassSpace klass = new DFClassSpace(this, id);
        return this.addClass(id, klass);
    }
    public DFClassSpace addClass(String id, DFClassSpace klass) {
        Utils.logit("DFTypeSpace.addClass: "+this+": "+klass);
        assert(!_id2klass.containsKey(id));
	_id2klass.put(id, klass);
	return klass;
    }

    public DFClassSpace lookupClass(SimpleName name) {
        String id = name.getIdentifier();
        DFClassSpace klass = _id2klass.get(id);
        if (klass != null) {
            return klass;
        } else if (_parent != null) {
            return _parent.lookupClass(name);
        } else {
            return null;
        }
    }

    public DFClassSpace lookupClass(Name name) {
	String fullname = name.getFullyQualifiedName();
	DFClassSpace klass = _root.loadClass(fullname);
	if (klass == null) {
	    if (name.isQualifiedName()) {
		QualifiedName qname = (QualifiedName)name;
		DFTypeSpace parent = this.lookupSpace(qname.getQualifier());
		klass = parent.lookupClass(qname.getName());
	    } else {
		klass = this.lookupClass((SimpleName)name);
	    }
	}
	return klass;
    }

    public DFClassSpace resolveClass(DFType type) {
        if (type instanceof DFClassType) {
            return ((DFClassType)type).getKlass();
        }
        return _root.getDefaultClass(type);
    }
    public DFClassSpace resolveClass(Type type) {
        return resolveClass(resolve(type));
    }

    @SuppressWarnings("unchecked")
    public DFType resolve(Type type) {
	if (type instanceof PrimitiveType) {
            PrimitiveType ptype = (PrimitiveType)type;
            return new DFBasicType(ptype.getPrimitiveTypeCode());
	} else if (type instanceof SimpleType) {
            SimpleType stype = (SimpleType)type;
            DFClassSpace klass = this.lookupClass(stype.getName());
            if (klass == null) {
                klass = _root.getDefaultClass(stype.getName());
            }
            return new DFClassType(klass);
	} else if (type instanceof ArrayType) {
            ArrayType atype = (ArrayType)type;
	    DFType elemType = this.resolve(atype.getElementType());
	    int ndims = atype.getDimensions();
	    return new DFArrayType(elemType, ndims);
	} else if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            List<Type> args = (List<Type>) ptype.typeArguments();
            DFType baseType = this.resolve(ptype.getType());
            DFType[] argTypes = new DFType[args.size()];
            for (int i = 0; i < args.size(); i++) {
                argTypes[i] = this.resolve(args.get(i));
            }
            if (baseType instanceof DFClassType) {
                // XXX make DFCompoundType
                return new DFClassType(
                    ((DFClassType)baseType).getKlass(), argTypes);
            }
        }
        return null;
    }

    public DFType resolve(org.apache.bcel.generic.Type type) {
        if (type.equals(org.apache.bcel.generic.BasicType.BOOLEAN)) {
            return DFType.BOOLEAN;
        } else if (type.equals(org.apache.bcel.generic.BasicType.BYTE)) {
            return DFType.BYTE;
        } else if (type.equals(org.apache.bcel.generic.BasicType.CHAR)) {
            return DFType.CHAR;
        } else if (type.equals(org.apache.bcel.generic.BasicType.DOUBLE)) {
            return DFType.DOUBLE;
        } else if (type.equals(org.apache.bcel.generic.BasicType.FLOAT)) {
            return DFType.FLOAT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.INT)) {
            return DFType.INT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.LONG)) {
            return DFType.LONG;
        } else if (type.equals(org.apache.bcel.generic.BasicType.SHORT)) {
            return DFType.SHORT;
        } else if (type.equals(org.apache.bcel.generic.BasicType.VOID)) {
            return DFType.VOID;
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
            DFClassSpace klass = _root.loadClass(className);
            return new DFClassType(klass);
        } else {
	    return null;
	}
    }

    private DFClassSpace loadClass(String fullname) {
        assert(this == _root);
        DFClassSpace klass = _id2klass.get(fullname);
        if (klass == null) {
            try {
                JavaClass jklass = Repository.lookupClass(fullname);
                klass = new DFClassSpace(this, fullname);
                _id2klass.put(fullname, klass);
                klass.build(jklass);
            } catch (ClassNotFoundException e) {
	    }
	}
        return klass;
    }

    @SuppressWarnings("unchecked")
    public void build(CompilationUnit cunit)
	throws UnsupportedSyntax {
        for (TypeDeclaration typeDecl :
                 (List<TypeDeclaration>) cunit.types()) {
            this.build(typeDecl);
        }
    }

    @SuppressWarnings("unchecked")
    public void build(TypeDeclaration typeDecl)
	throws UnsupportedSyntax {
        Utils.logit("DFTypeSpace.build: "+this+": "+typeDecl.getName());
        this.addClass(typeDecl.getName().getIdentifier());
        DFTypeSpace child = this.lookupSpace(typeDecl.getName());
        for (BodyDeclaration body :
                 (List<BodyDeclaration>) typeDecl.bodyDeclarations()) {
            build(child, body);
        }
    }

    public void build(DFTypeSpace child, BodyDeclaration body)
	throws UnsupportedSyntax {
        if (body instanceof TypeDeclaration) {
            child.build((TypeDeclaration)body);
        } else if (body instanceof FieldDeclaration) {
            ;
        } else if (body instanceof MethodDeclaration) {
            ;
        } else {
            throw new UnsupportedSyntax(body);
        }
    }

    private void importNames(ImportDeclaration importDecl) {
        // XXX support static import
        assert(!importDecl.isStatic());
        Name name = importDecl.getName();
        if (importDecl.isOnDemand()) {
            DFTypeSpace typeSpace = _root.lookupSpace(name);
            for (String id : typeSpace._id2klass.keySet()) {
                DFTypeSpace space = typeSpace._id2space.get(id);
                _id2space.put(id, space);
                DFClassSpace klass = typeSpace._id2klass.get(id);
                _id2klass.put(id, klass);
            }
        } else {
            assert(name.isQualifiedName());
            DFClassSpace klass = _root.lookupClass(name);
            if (klass == null) {
                Utils.logit("Fail: could not import: "+name);
            } else {
                QualifiedName qname = (QualifiedName)name;
                this.addClass(qname.getName().getIdentifier(), klass);
            }
        }
    }
    public DFTypeSpace extend(List<ImportDeclaration> imports) {
        // Make a copy as we're polluting the oririnal TypeSpace.
        DFTypeSpace typeSpace = new DFTypeSpace(this);
	for (ImportDeclaration importDecl : imports) {
            typeSpace.importNames(importDecl);
        }
        return typeSpace;
    }

    public void loadJarFile(String path)
	throws IOException {
        Utils.logit("Loading: "+path);
	JarFile jarfile = new JarFile(path);
	for (Enumeration<JarEntry> es = jarfile.entries(); es.hasMoreElements(); ) {
	    JarEntry je = es.nextElement();
	    InputStream strm = jarfile.getInputStream(je);
	    String name = je.getName();
            if (name.endsWith(".class")) {
                JavaClass jklass = new ClassParser(strm, name).parse();
                Repository.addClass(jklass);
                String packageName = jklass.getPackageName();
                if (packageName.equals("java.lang")) {
                    String fullName = jklass.getClassName();
                    String[] names = fullName.split("\\.");
                    assert(names.length == 2);
                    String id = names[names.length-1];
                    DFClassSpace klass = _root.loadClass(fullName);
                    _id2klass.put(id, klass);
                    Utils.logit("Added: "+fullName+" -> "+id);
                }
            }
	}
    }

    // dump: for debugging.
    public void dump() {
	dump(System.err, "");
    }
    public void dump(PrintStream out, String indent) {
	out.println(indent+_name+" {");
	String i2 = indent + "  ";
	for (DFClassSpace klass : _id2klass.values()) {
	    out.println(i2+"defined: "+klass);
	}
	for (DFTypeSpace space : _children) {
	    space.dump(out, i2);
	}
	out.println(indent+"}");
    }
}
