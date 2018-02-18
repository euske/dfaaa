//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFRef
//  Place to store a value.
//
public class DFRef implements Comparable<DFRef> {

    private DFScope _scope;
    private String _name;

    public DFRef(DFScope scope, String name) {
	_scope = scope;
	_name = name;
    }

    @Override
    public String toString() {
	return ("<DFRef("+this.getName()+")>");
    }

    @Override
    public int compareTo(DFRef ref) {
	return _name.compareTo(ref._name);
    }

    public String getName() {
	return ((_scope == null)?
		_name :
		_scope.getName()+":"+_name);
    }
}
