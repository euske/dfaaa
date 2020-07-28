//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFTypeSpace
//
public class DFTypeSpace {

    private String _name;
    private DFTypeSpace _outerSpace;

    private Map<String, DFTypeSpace> _id2space =
        new HashMap<String, DFTypeSpace>();
    private Map<String, DFKlass> _id2klass =
        new ConsistentHashMap<String, DFKlass>();
    private Map<String, DFMapType> _id2maptype =
        new ConsistentHashMap<String, DFMapType>();

    public DFTypeSpace(String name, DFTypeSpace outerSpace) {
        _name = name;
        _outerSpace = outerSpace;
    }

    public DFTypeSpace(String name) {
        this(name, null);
    }

    @Override
    public String toString() {
        return ("<DFTypeSpace("+this.getSpaceName()+")>");
    }

    public String getSpaceName() {
        if (_outerSpace == null) {
            return _name+"/";
        } else {
            return _outerSpace.getSpaceName()+_name+"/";
        }
    }

    public DFTypeSpace lookupSpace(String id) {
        int i = id.lastIndexOf('.');
        if (0 <= i) {
            DFTypeSpace space = this.lookupSpace(id.substring(0, i));
            return space.lookupSpace(id.substring(i+1));
        }
        DFKlass klass = _id2klass.get(id);
        if (klass != null) return klass;
        DFTypeSpace space = _id2space.get(id);
        if (space == null) {
            space = new DFTypeSpace(id, this);
            _id2space.put(id, space);
            //Logger.info("DFTypeSpace.addChild:", this, ":", id);
        }
        return space;
    }

    public DFKlass addKlass(String id, DFKlass klass) {
        assert id.indexOf('.') < 0;
        //assert !_id2klass.containsKey(id);
        _id2klass.put(id, klass);
        //Logger.info("DFTypeSpace.addKlass:", this, ":", id);
        return klass;
    }

    public DFKlass getKlass(Name name) {
        return this.getKlass(name.getFullyQualifiedName());
    }

    public DFKlass getKlass(String id) {
        //Logger.info("DFTypeSpace.getKlass:", this, ":", id);
        int i = id.lastIndexOf('.');
        if (0 <= i) {
            DFTypeSpace space = this.lookupSpace(id.substring(0, i));
            return space.getKlass(id.substring(i+1));
        }
        return _id2klass.get(id);
    }

    @SuppressWarnings("unchecked")
    public DFMapType[] createMapTypes(List<TypeParameter> tps) {
        if (tps.isEmpty()) return null;
        DFMapType[] mapTypes = new DFMapType[tps.size()];
        for (int i = 0; i < tps.size(); i++) {
            TypeParameter tp = tps.get(i);
            String id = tp.getName().getIdentifier();
            DFMapType mapType = _id2maptype.get(id);
            if (mapType == null) {
                mapType = new DFMapType(id, this, tp.typeBounds());
                _id2maptype.put(id, mapType);
            }
            mapTypes[i] = mapType;
        }
        return mapTypes;
    }

    public static String getConcreteName(Map<String, DFType> paramTypes) {
        String[] keys = new String[paramTypes.size()];
        paramTypes.keySet().toArray(keys);
        Arrays.sort(keys);
        StringBuilder b = new StringBuilder();
        for (String k : keys) {
            if (0 < b.length()) {
                b.append(",");
            }
            DFType type = paramTypes.get(k);
            if (type instanceof DFMapType) {
                type = ((DFMapType)type).getBaseKlass();
            }
            b.append(type.getTypeName());
        }
        return "<"+b.toString()+">";
    }

    protected Collection<DFKlass> getInnerKlasses() {
        return _id2klass.values();
    }

    // dump: for debugging.
    public void dump() {
        dump(System.err, "");
    }
    public void dump(PrintStream out, String indent) {
        out.println(indent+this.getSpaceName()+" {");
        String i2 = indent + "  ";
        this.dumpContents(out, i2);
        for (DFTypeSpace space : _id2space.values()) {
            space.dump(out, i2);
        }
        out.println(indent+"}");
    }

    protected void dumpContents(PrintStream out, String indent) {
        for (Map.Entry<String,DFKlass> e : _id2klass.entrySet()) {
            out.println(indent+"defined: "+e.getKey()+" "+e.getValue());
        }
    }
}
