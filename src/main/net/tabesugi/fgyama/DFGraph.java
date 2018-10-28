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
    private DFFrame _frame;
    private DFMethod _method;
    private boolean _init;
    private ASTNode _ast;

    private List<DFNode> _nodes =
        new ArrayList<DFNode>();

    // DFGraph for a method.
    public DFGraph(
        DFVarScope root, DFFrame frame, DFMethod method,
        boolean init, ASTNode ast) {
        _root = root;
        _frame = frame;
        _method = method;
        _init = init;
        _ast = ast;
    }

    @Override
    public String toString() {
        return ("<DFGraph("+_root.getFullName()+")>");
    }

    public Element toXML(Document document) {
        Element elem = document.createElement("graph");
        if (_method != null) {
            elem.setAttribute("name", _method.getSignature());
        } else {
            elem.setAttribute("name", _root.getFullName());
        }
        if (_ast != null) {
            Element east = document.createElement("ast");
            east.setAttribute("type", Integer.toString(_ast.getNodeType()));
            east.setAttribute("start", Integer.toString(_ast.getStartPosition()));
            east.setAttribute("length", Integer.toString(_ast.getLength()));
            elem.appendChild(east);
        }
        elem.setAttribute("init", Boolean.toString(_init));
        elem.setAttribute("ins", getNodeIds(_frame.getInputNodes()));
        elem.setAttribute("outs", getNodeIds(_frame.getOutputNodes()));
        DFNode[] nodes = new DFNode[_nodes.size()];
        _nodes.toArray(nodes);
        Arrays.sort(nodes);
        elem.appendChild(_root.toXML(document, nodes));
        return elem;
    }

    private static String getNodeIds(DFNode[] nodes) {
        List<String> nodeIds = new ArrayList<String>();
        for (DFNode node : nodes) {
            DFVarRef ref = node.getRef();
            if (ref != null && !ref.isLocal()) {
                nodeIds.add(node.getNodeId());
            }
        }
        String[] names = new String[nodeIds.size()];
        nodeIds.toArray(names);
        Arrays.sort(names);
        return Utils.join(" ", names);
    }

    public int addNode(DFNode node) {
        _nodes.add(node);
        return _nodes.size();
    }

    public void cleanup() {
        Set<DFNode> removed = new HashSet<DFNode>();
        for (DFNode node : _nodes) {
            if (node.getKind() == null && node.purge()) {
                removed.add(node);
            }
        }
        // Do not remove input/output nodes.
        for (DFNode node : _frame.getInputNodes()) {
            removed.remove(node);
        }
        for (DFNode node : _frame.getOutputNodes()) {
            removed.remove(node);
        }
        for (DFNode node : removed) {
            _nodes.remove(node);
        }
    }
}
