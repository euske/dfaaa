//  Java2DF
//
package net.tabesugi.dfaaa;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


// AssignNode: corresponds to a certain location in a memory.
public abstract class AssignNode extends ProgNode {

    public AssignNode(DFScope scope, DFRef ref, ASTNode ast) {
	super(scope, ref, ast);
    }

    @Override
    public String getType() {
	return "assign";
    }
}
