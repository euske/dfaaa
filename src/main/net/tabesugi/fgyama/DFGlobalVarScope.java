//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.w3c.dom.*;


//  DFGlobalVarScope
//
public class DFGlobalVarScope extends DFVarScope {

    private Map<String, DFRef> _id2elem =
        new HashMap<String, DFRef>();

    public DFGlobalVarScope() {
        super("GLOBAL");
    }

    @Override
    public String toString() {
        return ("<DFGlobalVarScope>");
    }

    public DFRef lookupArray(DFType type) {
        DFRef ref;
        DFType elemType = DFUnknownType.UNKNOWN;
        if (type instanceof DFArrayType) {
            elemType = ((DFArrayType)type).getElemType();
        }
        String id = elemType.getTypeName();
        ref = _id2elem.get(id);
        if (ref == null) {
            ref = new DFElemRef(elemType);
            _id2elem.put(id, ref);
        }
        return ref;
    }

    private class DFElemRef extends DFRef {

        public DFElemRef(DFType type) {
            super(type);
        }

        public String getFullName() {
            return "%"+this.getRefType().getTypeName();
        }
    }
}
