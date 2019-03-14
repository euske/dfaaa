//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFMethod
//
public class DFMethod implements Comparable<DFMethod> {

    private DFKlass _klass;
    private DFTypeSpace _methodSpace;
    private String _name;
    private DFCallStyle _callStyle;
    private DFMapType[] _mapTypes;
    private DFTypeFinder _finder;
    private DFMethodType _methodType;

    private SortedSet<DFMethod> _callers =
        new TreeSet<DFMethod>();

    private ASTNode _ast = null;

    private DFLocalVarScope _scope = null;
    private DFFrame _frame = null;

    // List of subclass' methods overriding this method.
    private List<DFMethod> _overrides = new ArrayList<DFMethod>();

    private class DFOverride implements Comparable<DFOverride> {

	public DFMethod method;
	public int level;

	public DFOverride(DFMethod method, int level) {
	    this.method = method;
	    this.level = level;
	}

	@Override
	public String toString() {
	    return ("<DFOverride: "+this.method+" ("+this.level+")>");
	}

	@Override
	public int compareTo(DFOverride override) {
	    if (this.level != override.level) {
		return override.level - this.level;
	    } else {
		return this.method.compareTo(override.method);
	    }
	}
    }

    public DFMethod(
        DFKlass klass, DFTypeSpace methodSpace,
        String name, DFCallStyle callStyle,
        DFMapType[] mapTypes, DFTypeFinder finder,
        DFMethodType methodType) {
        _klass = klass;
        _methodSpace = methodSpace;
        _name = name;
        _callStyle = callStyle;
        _mapTypes = mapTypes;
        _finder = finder;
        _methodType = methodType;
    }

    @Override
    public String toString() {
        return ("<DFMethod("+this.getSignature()+">");
    }

    @Override
    public int compareTo(DFMethod method) {
        return _name.compareTo(method._name);
    }

    public boolean equals(DFMethod method) {
        if (!_name.equals(method._name)) return false;
        return _methodType.equals(method._methodType);
    }

    public boolean isGeneric() {
        return (_mapTypes != null);
    }

    public String getSignature() {
        String name;
        if (_klass != null) {
            name = _klass.getTypeName()+"."+_name;
        } else {
            name = "!"+_name;
        }
        return name + _methodType.getTypeName();
    }

    public DFCallStyle getCallStyle() {
        return _callStyle;
    }

    public DFTypeSpace getMethodSpace() {
        return _methodSpace;
    }

    public DFType getReturnType() {
        return _methodType.getReturnType();
    }

    public int canAccept(String name, DFType[] argTypes) {
        if (name != null && !_name.equals(name)) return -1;
        return _methodType.canAccept(argTypes, null);
    }

    public void addOverride(DFMethod method) {
	//Logger.info("DFMethod.addOverride:", this, "<-", method);
        _overrides.add(method);
    }

    private void listOverrides(List<DFOverride> overrides, int prio) {
        overrides.add(new DFOverride(this, prio));
        for (DFMethod method : _overrides) {
            method.listOverrides(overrides, prio+1);
        }
    }

    public DFMethod[] getOverrides() {
        List<DFOverride> overrides = new ArrayList<DFOverride>();
        this.listOverrides(overrides, 0);
        DFOverride[] a = new DFOverride[overrides.size()];
        overrides.toArray(a);
        Arrays.sort(a);
        DFMethod[] methods = new DFMethod[a.length];
	for (int i = 0; i < a.length; i++) {
	    methods[i] = a[i].method;
	}
        return methods;
    }

    public void addCaller(DFMethod method) {
        _callers.add(method);
    }

    public SortedSet<DFMethod> getCallers() {
        return _callers;
    }

    public DFTypeFinder getFinder() {
        return _finder;
    }

    public void setTree(ASTNode ast) {
	_ast = ast;
    }
    public ASTNode getTree() {
        return _ast;
    }

    public void setScope(DFLocalVarScope scope) {
        _scope = scope;
    }
    public DFLocalVarScope getScope() {
        return _scope;
    }
    public DFFrame getFrame() {
        return _frame;
    }

    public void buildScope()
        throws UnsupportedSyntax, TypeNotFound {
	if (_ast == null) return;
	assert _scope != null;
	DFTypeFinder finder = this.getFinder();
	if (_ast instanceof MethodDeclaration) {
	    _scope.buildMethodDecl(finder, (MethodDeclaration)_ast);
	} else if (_ast instanceof Initializer) {
	    _scope.buildInitializer(finder, (Initializer)_ast);
	}  else {
	    throw new UnsupportedSyntax(_ast);
	}
        //_scope.dump();
    }

    public void buildFrame()
        throws UnsupportedSyntax, TypeNotFound {
	if (_ast == null) return;
	assert _scope != null;
	DFTypeFinder finder = this.getFinder();
        _frame = new DFFrame(DFFrame.RETURNABLE);
        try {
	    if (_ast instanceof MethodDeclaration) {
		_frame.buildMethodDecl(
                    finder, this, _scope, (MethodDeclaration)_ast);
	    } else if (_ast instanceof Initializer) {
		_frame.buildInitializer(
                    finder, this, _scope, (Initializer)_ast);
	    }  else {
		throw new UnsupportedSyntax(_ast);
	    }
        } catch (EntityNotFound e) {
            // XXX ignore EntityNotFound for now
            Logger.error("Entity not found:", e.name, "ast="+e.ast, "method="+this);
        }
    }
}
