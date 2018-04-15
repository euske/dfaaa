//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.w3c.dom.*;


//  DFGraph
//
public class DFGraph {

    private DFVarScope _root;
    private String _name;

    private Map<Integer, DFNode> _nodes =
	new HashMap<Integer, DFNode>();

    public DFGraph(DFVarScope root, String name) {
	_root = root;
	_name = name;
    }

    @Override
    public String toString() {
	return ("<DFGraph("+_name+")>");
    }

    public Element toXML(Document document) {
	Element elem = document.createElement("graph");
        elem.setAttribute("name", _name);
	DFNode[] nodes = new DFNode[_nodes.size()];
	_nodes.values().toArray(nodes);
	Arrays.sort(nodes);
	elem.appendChild(_root.toXML(document, nodes));
	return elem;
    }

    public int addNode(DFNode node) {
	int id = _nodes.size()+1;
	_nodes.put(id, node);
	return id;
    }

    public void cleanup() {
	ArrayList<Integer> removed = new ArrayList<Integer>();
	for (DFNode node : _nodes.values()) {
	    if (node.getKind() == null && node.purge()) {
		removed.add(node.getId());
	    }
	}
	for (int id : removed) {
	    _nodes.remove(id);
	}
    }
}
