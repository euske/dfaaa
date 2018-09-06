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


//  DFClassSpace
//
public class DFClassSpace extends DFVarSpace implements DFType {

    private DFTypeSpace _typeSpace;
    private DFTypeSpace _childSpace;
    private DFClassSpace _baseKlass;
    private DFClassSpace[] _baseIfaces;
    private DFParamType[] _paramTypes = null;
    private String _jarPath = null;
    private String _filePath = null;
    private boolean _loaded = true;
    private DFVarRef _this = null;

    private List<DFMethod> _methods =
        new ArrayList<DFMethod>();
    private Map<String, DFMethod> _ast2method =
        new HashMap<String, DFMethod>();

    public DFClassSpace(
        DFTypeSpace typeSpace, DFTypeSpace childSpace,
	DFVarSpace parent, String id, DFClassSpace baseKlass) {
        super(parent, id);
        _typeSpace = typeSpace;
        _childSpace = childSpace;
        _baseKlass = baseKlass;
    }

    public DFClassSpace(
        DFTypeSpace typeSpace, DFTypeSpace childSpace,
	DFVarSpace parent, String id) {
        super(parent, id);
        _typeSpace = typeSpace;
        _childSpace = childSpace;
        _this = this.addRef("#this", this);
    }

    @Override
    public String toString() {
        return ("<DFClassSpace("+this.getFullName()+")>");
    }

    public boolean equals(DFType type) {
        return (this == type);
    }

    public String getName()
    {
        return this.getFullName();
    }

    public int canConvertFrom(DFType type)
    {
        if (type instanceof DFNullType) return 0;
        if (!(type instanceof DFClassSpace)) return -1;
        return this.isSubclassOf((DFClassSpace)type);
    }

    public void setJarPath(String jarPath, String filePath) {
        _jarPath = jarPath;
        _filePath = filePath;
        _loaded = false;
    }

    public void setParamTypes(DFParamType[] paramTypes) {
        _paramTypes = paramTypes;
    }

    public DFTypeSpace getChildSpace() {
        return _childSpace;
    }

    public DFClassSpace getBase() {
        return _baseKlass;
    }

    public String getFullName() {
        return _typeSpace.getFullName()+"/"+super.getBaseName();
    }

    public int isSubclassOf(DFClassSpace klass) {
        if (this == klass) return 0;
        if (_baseKlass != null) {
            int dist = _baseKlass.isSubclassOf(klass);
            if (0 <= dist) return dist+1;
        }
        if (_baseIfaces != null) {
            for (DFClassSpace iface : _baseIfaces) {
                int dist = iface.isSubclassOf(klass);
                if (0 <= dist) return dist+1;
            }
        }
        return -1;
    }

    public DFVarRef lookupThis() {
        return _this;
    }

    protected DFVarRef lookupField(String id)
        throws VariableNotFound {
        try {
            return this.lookupRef("."+id);
        } catch (VariableNotFound e) {
            if (_baseKlass == null) throw e;
            return _baseKlass.lookupField(id);
        }
    }

    public DFVarRef lookupField(SimpleName name)
        throws VariableNotFound {
        return this.lookupField(name.getIdentifier());
    }

    private DFMethod lookupMethod1(SimpleName name, DFType[] argTypes) {
        String id = name.getIdentifier();
        int bestDist = -1;
        DFMethod bestMethod = null;
        for (DFMethod method : _methods) {
            int dist = method.canAccept(id, argTypes);
            if (dist < 0) continue;
            if (bestDist < 0 || dist < bestDist) {
                bestDist = dist;
                bestMethod = method;
            }
        }
        return bestMethod;
    }

    public DFMethod lookupMethod(SimpleName name, DFType[] argTypes) {
        DFClassSpace klass = this;
        while (klass != null) {
            DFMethod method = klass.lookupMethod1(name, argTypes);
            if (method != null) {
                return method;
            }
            klass = klass._baseKlass;
        }
        return null;
    }

    public DFMethod getMethodByAST(ASTNode ast) {
        return _ast2method.get(Utils.encodeASTNode(ast));
    }

    private DFVarRef addField(
        SimpleName name, boolean isStatic, DFType type) {
        return this.addField(name.getIdentifier(), isStatic, type);
    }

    public DFVarRef addField(
        String id, boolean isStatic, DFType type) {
        DFVarRef ref = this.addRef("."+id, type);
        //Logger.info("DFClassSpace.addField: "+ref);
        return ref;
    }

    private DFMethod addMethod(
        DFTypeSpace methodSpace, SimpleName name, boolean isStatic,
        DFType[] argTypes, DFType returnType) {
        String id = name.getIdentifier();
        DFMethod method = new DFMethod(
            this, methodSpace, id, isStatic, argTypes, returnType);
        return this.addMethod(method);
    }

    private DFMethod addMethod(
        DFTypeSpace methodSpace, String id, boolean isStatic,
        DFType[] argTypes, DFType returnType) {
        DFMethod method = new DFMethod(
            this, methodSpace, id, isStatic, argTypes, returnType);
        return this.addMethod(method);
    }

    private DFMethod addMethod(DFMethod method) {
        //Logger.info("DFClassSpace.addMethod: "+method);
        _methods.add(method);
        return method;
    }

    public void addOverrides() {
        for (DFMethod method : _methods) {
            if (_baseKlass != null) {
                _baseKlass.overrideMethod(method);
            }
            if (_baseIfaces != null) {
                for (DFClassSpace iface : _baseIfaces) {
                    iface.overrideMethod(method);
                }
            }
        }
    }

    private void overrideMethod(DFMethod method1) {
        for (DFMethod method0 : _methods) {
            if (method0.equals(method1)) {
                method0.addOverride(method1);
                //Logger.info("DFClassSpace.overrideMethod: "+method0+" : "+method1);
                break;
            }
        }
        if (_baseKlass != null) {
            _baseKlass.overrideMethod(method1);
        }
        if (_baseIfaces != null) {
            for (DFClassSpace iface : _baseIfaces) {
                iface.overrideMethod(method1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isStatic(BodyDeclaration body) {
        for (IExtendedModifier imod :
                 (List<IExtendedModifier>) body.modifiers()) {
            if (imod.isModifier()) {
                if (((Modifier)imod).isStatic()) return true;
            }
        }
        return false;
    }

    public void load(DFTypeFinder finder) throws TypeNotFound {
        if (_loaded) return;
        _loaded = true;
        try {
            JarFile jarfile = new JarFile(_jarPath);
            try {
                JarEntry je = jarfile.getJarEntry(_filePath);
                InputStream strm = jarfile.getInputStream(je);
                JavaClass jklass = new ClassParser(strm, _filePath).parse();
                this.load(finder, jklass);
            } finally {
                jarfile.close();
            }
        } catch (IOException e) {
            Logger.error("Error: Not found: "+_jarPath+"/"+_filePath);
            throw new TypeNotFound(this.getFullName());
        }
    }

    private void load(DFTypeFinder finder, JavaClass jklass)
        throws TypeNotFound {
        String superClass = jklass.getSuperclassName();
        if (superClass != null && !superClass.equals(jklass.getClassName())) {
            _baseKlass = finder.lookupClass(superClass);
        }
        for (Field fld : jklass.getFields()) {
            if (fld.isPrivate()) continue;
            DFType type = finder.resolve(fld.getType());
            this.addRef("."+fld.getName(), type);
        }
        for (Method meth : jklass.getMethods()) {
            if (meth.isPrivate()) continue;
            org.apache.bcel.generic.Type[] args = meth.getArgumentTypes();
            DFType[] argTypes = new DFType[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = finder.resolve(args[i]);
            }
            DFType returnType = finder.resolve(meth.getReturnType());
            this.addMethod(null, meth.getName(), meth.isStatic(),
                           argTypes, returnType);
        }
    }

    public DFTypeFinder addFinders(DFTypeFinder finder) {
        if (_baseKlass != null) {
            finder = _baseKlass.addFinders(finder);
        }
        finder = new DFTypeFinder(finder, _childSpace);
        return finder;
    }

    @SuppressWarnings("unchecked")
    public void build(DFTypeFinder finder, TypeDeclaration typeDecl)
        throws UnsupportedSyntax, TypeNotFound {
        //Logger.info("DFClassSpace.build: "+this+": "+typeDecl.getName());
        // Get superclass.
        try {
            finder = new DFTypeFinder(finder, _childSpace);
            Type superClass = typeDecl.getSuperclassType();
            if (superClass != null) {
                _baseKlass = finder.resolveClass(superClass);
                //Logger.info("DFClassSpace.build: "+this+" extends "+_baseKlass);
                finder = _baseKlass.addFinders(finder);
            }
            // Get interfaces.
            List<Type> ifaces = typeDecl.superInterfaceTypes();
            _baseIfaces = new DFClassSpace[ifaces.size()];
            for (int i = 0; i < ifaces.size(); i++) {
                _baseIfaces[i] = finder.resolveClass(ifaces.get(i));
            }
            // Get type parameters.
            List<TypeParameter> tps = typeDecl.typeParameters();
            for (int i = 0; i < _paramTypes.length; i++) {
                DFParamType pt = _paramTypes[i];
                List<Type> bounds = tps.get(i).typeBounds();
                DFClassSpace[] bases = new DFClassSpace[bounds.size()];
                for (int j = 0; j < bases.length; j++) {
                    bases[j] = finder.resolveClass(bounds.get(j));
                }
                pt.setBases(bases);
            }
            // Lookup child classes.
            for (BodyDeclaration body :
                     (List<BodyDeclaration>) typeDecl.bodyDeclarations()) {
                this.build(finder, body);
            }
        } catch (TypeNotFound e) {
            e.setAst(typeDecl);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public void build(DFTypeFinder finder, BodyDeclaration body)
        throws UnsupportedSyntax, TypeNotFound {
        if (body instanceof TypeDeclaration) {
            TypeDeclaration decl = (TypeDeclaration)body;
            DFClassSpace klass = _childSpace.getClass(decl.getName());
            klass.build(finder, decl);

        } else if (body instanceof FieldDeclaration) {
            FieldDeclaration decl = (FieldDeclaration)body;
            DFType type = finder.resolve(decl.getType());
            for (VariableDeclarationFragment frag :
                     (List<VariableDeclarationFragment>) decl.fragments()) {
                this.addField(frag.getName(), isStatic(decl), type);
            }

        } else if (body instanceof MethodDeclaration) {
            MethodDeclaration decl = (MethodDeclaration)body;
            List<TypeParameter> tps = decl.typeParameters();
            DFTypeFinder finder2 = finder;
            DFTypeSpace methodSpace = null;
            if (0 < tps.size()) {
                methodSpace = new DFTypeSpace("MethodDecl");
                finder2 = new DFTypeFinder(finder, methodSpace);
                DFParamType[] paramTypes = DFParamType.createParamTypes(
                    methodSpace, tps);
                for (int i = 0; i < paramTypes.length; i++) {
                    DFParamType pt = paramTypes[i];
                    List<Type> bounds = tps.get(i).typeBounds();
                    DFClassSpace[] bases = new DFClassSpace[bounds.size()];
                    for (int j = 0; j < bases.length; j++) {
                        bases[j] = finder.resolveClass(bounds.get(j));
                    }
                    pt.setBases(bases);
                    methodSpace.addParamType(pt);
                }
            }
            DFType[] argTypes = finder2.resolveList(decl);
            DFType returnType;
            if (decl.isConstructor()) {
                returnType = this;
                // XXX treat method name specially.
            } else {
                returnType = finder2.resolve(decl.getReturnType2());
            }
            DFMethod method = this.addMethod(
                methodSpace, decl.getName(), isStatic(decl),
                argTypes, returnType);
            _ast2method.put(Utils.encodeASTNode(decl), method);

        } else if (body instanceof Initializer) {

        } else {
            throw new UnsupportedSyntax(body);
        }
    }

    // dumpContents (for debugging)
    public void dumpContents(PrintStream out, String indent) {
        super.dumpContents(out, indent);
        for (DFMethod method : _methods) {
            out.println(indent+"defined: "+method);
        }
    }
}
