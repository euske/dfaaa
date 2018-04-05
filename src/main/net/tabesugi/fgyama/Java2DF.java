/**
 * Java2DF
 * Dataflow analyzer for Java
 */
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.w3c.dom.*;


// ProgNode: a DFNode that corresponds to an actual program point.
abstract class ProgNode extends DFNode {

    public ASTNode ast;

    public ProgNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast) {
	super(graph, scope, ref);
	this.ast = ast;
    }

    @Override
    public Element toXML(Document document) {
	Element elem = super.toXML(document);
	if (this.ast != null) {
	    Element east = document.createElement("ast");
	    east.setAttribute("type", Integer.toString(this.ast.getNodeType()));
	    east.setAttribute("start", Integer.toString(this.ast.getStartPosition()));
	    east.setAttribute("length", Integer.toString(this.ast.getLength()));
	    elem.appendChild(east);
	}
	return elem;
    }
}

// SingleAssignNode:
class SingleAssignNode extends ProgNode {

    public SingleAssignNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast) {
	super(graph, scope, ref, ast);
    }

    @Override
    public String getType() {
	return "assign";
    }
}

// ArrayAssignNode:
class ArrayAssignNode extends ProgNode {

    public ArrayAssignNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			   DFNode array, DFNode index) {
	super(graph, scope, ref, ast);
	this.accept(array, "array");
	this.accept(index, "index");
    }

    @Override
    public String getType() {
	return "arrayassign";
    }
}

// FieldAssignNode:
class FieldAssignNode extends ProgNode {

    public FieldAssignNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			   DFNode obj) {
	super(graph, scope, ref, ast);
	this.accept(obj, "obj");
    }

    @Override
    public String getType() {
	return "fieldassign";
    }
}

// VarRefNode: represnets a variable reference.
class VarRefNode extends ProgNode {

    public VarRefNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		      DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(value);
    }

    @Override
    public String getType() {
	return "ref";
    }
}

// ArrayAccessNode
class ArrayAccessNode extends ProgNode {

    public ArrayAccessNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			   DFNode array, DFNode index, DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(array, "array");
	this.accept(index, "index");
	this.accept(value);
    }

    @Override
    public String getType() {
	return "arrayaccess";
    }
}

// FieldAccessNode
class FieldAccessNode extends ProgNode {

    public FieldAccessNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			   DFNode obj, DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(obj, "obj");
	this.accept(value);
    }

    @Override
    public String getType() {
	return "fieldaccess";
    }
}

// PrefixNode
class PrefixNode extends ProgNode {

    public PrefixExpression.Operator op;

    public PrefixNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		      PrefixExpression.Operator op, DFNode value) {
	super(graph, scope, ref, ast);
	this.op = op;
	this.accept(value);
    }

    @Override
    public String getType() {
	return "prefix";
    }

    @Override
    public String getData() {
	return this.op.toString();
    }
}

// PostfixNode
class PostfixNode extends ProgNode {

    public PostfixExpression.Operator op;

    public PostfixNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		       PostfixExpression.Operator op, DFNode value) {
	super(graph, scope, ref, ast);
	this.op = op;
	this.accept(value);
    }

    @Override
    public String getType() {
	return "postfix";
    }

    @Override
    public String getData() {
	return this.op.toString();
    }
}

// InfixNode
class InfixNode extends ProgNode {

    public InfixExpression.Operator op;

    public InfixNode(DFGraph graph, DFScope scope, ASTNode ast,
		     InfixExpression.Operator op,
		     DFNode lvalue, DFNode rvalue) {
	super(graph, scope, null, ast);
	this.op = op;
	this.accept(lvalue, "L");
	this.accept(rvalue, "R");
    }

    @Override
    public String getType() {
	return "infix";
    }

    @Override
    public String getData() {
	return this.op.toString();
    }
}

// TypeCastNode
class TypeCastNode extends ProgNode {

    public Type type;

    public TypeCastNode(DFGraph graph, DFScope scope, ASTNode ast,
			Type type, DFNode value) {
	super(graph, scope, null, ast);
	this.type = type;
	this.accept(value);
    }

    @Override
    public String getType() {
	return "typecast";
    }

    @Override
    public String getData() {
        IBinding binding = this.type.resolveBinding();
        if (binding != null) {
            return binding.getKey();
        } else {
            return Utils.getTypeName(this.type);
        }
    }
}

// InstanceofNode
class InstanceofNode extends ProgNode {

    public Type type;

    public InstanceofNode(DFGraph graph, DFScope scope, ASTNode ast,
			  Type type, DFNode value) {
	super(graph, scope, null, ast);
	this.type = type;
	this.accept(value);
    }

    @Override
    public String getType() {
	return "instanceof";
    }

    @Override
    public String getData() {
        IBinding binding = this.type.resolveBinding();
        if (binding != null) {
            return binding.getKey();
        } else {
            return Utils.getTypeName(this.type);
        }
    }
}

// CaseNode
class CaseNode extends ProgNode {

    public List<DFNode> matches = new ArrayList<DFNode>();

    public CaseNode(DFGraph graph, DFScope scope, ASTNode ast,
		    DFNode value) {
	super(graph, scope, null, ast);
	this.accept(value);
    }

    @Override
    public String getType() {
	return "case";
    }

    @Override
    public String getData() {
	if (this.matches.isEmpty()) {
	    return "default";
	} else {
	    return "case("+this.matches.size()+")";
	}
    }

    public void addMatch(DFNode node) {
	String label = "match"+this.matches.size();
	this.accept(node, label);
	this.matches.add(node);
    }
}

// AssignOpNode
class AssignOpNode extends ProgNode {

    public Assignment.Operator op;

    public AssignOpNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			Assignment.Operator op,
			DFNode lvalue, DFNode rvalue) {
	super(graph, scope, ref, ast);
	this.op = op;
	this.accept(lvalue, "L");
	this.accept(rvalue, "R");
    }

    @Override
    public String getType() {
	return "assignop";
    }

    @Override
    public String getData() {
	return this.op.toString();
    }
}

// ArgNode: represnets a function argument.
class ArgNode extends ProgNode {

    public int index;

    public ArgNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		   int index) {
	super(graph, scope, ref, ast);
	this.index = index;
    }

    @Override
    public String getType() {
	return "arg";
    }

    @Override
    public String getData() {
	return Integer.toString(this.index);
    }
}

// ConstNode: represents a constant value.
class ConstNode extends ProgNode {

    public String value;

    public ConstNode(DFGraph graph, DFScope scope, ASTNode ast, String value) {
	super(graph, scope, null, ast);
	this.value = value;
    }

    @Override
    public String getType() {
	return "const";
    }

    @Override
    public String getData() {
	return this.value;
    }
}

// ArrayValueNode: represents an array.
class ArrayValueNode extends ProgNode {

    public List<DFNode> values = new ArrayList<DFNode>();

    public ArrayValueNode(DFGraph graph, DFScope scope, ASTNode ast) {
	super(graph, scope, null, ast);
    }

    @Override
    public String getType() {
	return "arrayvalue";
    }

    @Override
    public String getData() {
	return Integer.toString(this.values.size());
    }

    public void addValue(DFNode value) {
	String label = "value"+this.values.size();
	this.accept(value, label);
	this.values.add(value);
    }
}

// JoinNode
class JoinNode extends ProgNode {

    public boolean recvTrue = false;
    public boolean recvFalse = false;

    public JoinNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
                    DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(value, "cond");
    }

    @Override
    public String getType() {
	return "join";
    }

    @Override
    public void finish(DFComponent cpt) {
	if (!this.isClosed()) {
	    this.close(cpt.getValue(this.getRef()));
	}
    }

    public void recv(boolean cond, DFNode node) {
	if (cond) {
	    assert(!this.recvTrue);
	    this.recvTrue = true;
	    this.accept(node, "true");
	} else {
	    assert(!this.recvFalse);
	    this.recvFalse = true;
	    this.accept(node, "false");
	}
    }

    public boolean isClosed() {
	return (this.recvTrue && this.recvFalse);
    };

    public void close(DFNode node) {
	if (!this.recvTrue) {
	    assert(this.recvFalse);
	    this.recvTrue = true;
	    this.accept(node, "true");
	}
	if (!this.recvFalse) {
	    assert(this.recvTrue);
	    this.recvFalse = true;
	    this.accept(node, "false");
	}
    }
}

// LoopBeginNode
class LoopBeginNode extends ProgNode {

    public LoopBeginNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
			 DFNode enter) {
	super(graph, scope, ref, ast);
	this.accept(enter, "enter");
    }

    @Override
    public String getType() {
	return "begin";
    }

    public void setRepeat(DFNode repeat) {
	this.accept(repeat, "repeat");
    }

    public void setEnd(LoopEndNode end) {
	this.accept(end, "_end");
    }
}

// LoopEndNode
class LoopEndNode extends ProgNode {

    public LoopEndNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		       DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(value, "cond");
    }

    @Override
    public String getType() {
	return "end";
    }

    public void setBegin(LoopBeginNode begin) {
	this.accept(begin, "_begin");
    }
}

// LoopRepeatNode
class LoopRepeatNode extends ProgNode {

    public LoopRepeatNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast) {
	super(graph, scope, ref, ast);
    }

    @Override
    public String getType() {
	return "repeat";
    }

    public void setLoop(DFNode end) {
	this.accept(end, "_loop");
    }
}

// IterNode
class IterNode extends ProgNode {

    public IterNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		    DFNode value) {
	super(graph, scope, ref, ast);
	this.accept(value);
    }

    @Override
    public String getType() {
	return "iter";
    }
}

// CallNode
abstract class CallNode extends ProgNode {

    public List<DFNode> args;
    public DFNode exception;

    public CallNode(DFGraph graph, DFScope scope, DFRef ref, ASTNode ast,
		    DFNode obj) {
	super(graph, scope, ref, ast);
	this.args = new ArrayList<DFNode>();
        this.exception = null;
	if (obj != null) {
	    this.accept(obj, "obj");
	}
    }

    @Override
    public String getType() {
	return "call";
    }

    public void addArg(DFNode arg) {
	String label = "arg"+this.args.size();
	this.accept(arg, label);
	this.args.add(arg);
    }
}

// MethodCallNode
class MethodCallNode extends CallNode {

    public SimpleName name;

    public MethodCallNode(DFGraph graph, DFScope scope, ASTNode ast,
			  DFNode obj, SimpleName name) {
	super(graph, scope, null, ast, obj);
	this.name = name;
    }

    @Override
    public String getData() {
        IBinding binding = this.name.resolveBinding();
        if (binding != null) {
            return binding.getKey();
        } else {
            return this.name.getIdentifier();
        }
    }
}

// CreateObjectNode
class CreateObjectNode extends CallNode {

    public Type type;

    public CreateObjectNode(DFGraph graph, DFScope scope, ASTNode ast,
			    DFNode obj, Type type) {
	super(graph, scope, null, ast, obj);
	this.type = type;
    }

    @Override
    public String getType() {
	return "new";
    }

    @Override
    public String getData() {
        IBinding binding = this.type.resolveBinding();
        if (binding != null) {
            return binding.getKey();
        } else {
            return Utils.getTypeName(this.type);
        }
    }
}

// ReturnNode: represents a return value.
class ReturnNode extends ProgNode {

    public ReturnNode(DFGraph graph, DFScope scope, ASTNode ast, DFNode value) {
	super(graph, scope, scope.lookupReturn(), ast);
	this.accept(value);
    }

    @Override
    public String getType() {
	return "return";
    }
}

// ExceptionNode
class ExceptionNode extends ProgNode {

    public ExceptionNode(DFGraph graph, DFScope scope, ASTNode ast, DFNode value) {
	super(graph, scope, null, ast);
	this.accept(value);
    }

    @Override
    public String getType() {
	return "exception";
    }
}


//  NameResolutionChecker
//
class NameResolutionChecker extends ASTVisitor {

    private boolean _resolved = true;

    public boolean isResolved() {
	return _resolved;
    }

    public boolean visit(SimpleName node) {
	if (node.resolveBinding() == null) {
	    _resolved = false;
	}
	return false;
    }

    public static boolean canResolveNames(ASTNode node) {
	NameResolutionChecker checker = new NameResolutionChecker();
	node.accept(checker);
	return checker.isResolved();
    }
}


//  Java2DF
//
public class Java2DF extends ASTVisitor {

    /// General graph operations.

    /**
     * Combines two components into one.
     * A JoinNode is added to each variable.
     */
    public DFComponent processConditional
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt, ASTNode ast,
	 DFNode condValue, DFComponent trueCpt, DFComponent falseCpt) {

	// outRefs: all the references from both component.
	List<DFRef> outRefs = new ArrayList<DFRef>();
	if (trueCpt != null) {
	    for (DFRef ref : trueCpt.getInputRefs()) {
		DFNode src = trueCpt.getInput(ref);
		assert src != null;
		src.accept(cpt.getValue(ref));
	    }
	    outRefs.addAll(Arrays.asList(trueCpt.getOutputRefs()));
	}
	if (falseCpt != null) {
	    for (DFRef ref : falseCpt.getInputRefs()) {
		DFNode src = falseCpt.getInput(ref);
		assert src != null;
		src.accept(cpt.getValue(ref));
	    }
	    outRefs.addAll(Arrays.asList(falseCpt.getOutputRefs()));
	}

	// Attach a JoinNode to each variable.
	Set<DFRef> used = new HashSet<DFRef>();
	for (DFRef ref : outRefs) {
	    if (used.contains(ref)) continue;
	    used.add(ref);
	    JoinNode join = new JoinNode(graph, scope, ref, ast, condValue);
	    if (trueCpt != null) {
		DFNode dst = trueCpt.getOutput(ref);
		if (dst != null) {
		    join.recv(true, dst);
		}
	    }
	    if (falseCpt != null) {
		DFNode dst = falseCpt.getOutput(ref);
		if (dst != null) {
		    join.recv(false, dst);
		}
	    }
	    if (!join.isClosed()) {
		join.close(cpt.getValue(ref));
	    }
	    cpt.setOutput(join);
	}

	// Take care of exits.
	if (trueCpt != null) {
	    for (DFExit exit : trueCpt.getExits()) {
                DFNode node = exit.getNode();
		JoinNode join = new JoinNode(graph, scope, node.getRef(), null, condValue);
		join.recv(true, node);
		cpt.addExit(exit.wrap(join));
	    }
	}
	if (falseCpt != null) {
	    for (DFExit exit : falseCpt.getExits()) {
                DFNode node = exit.getNode();
		JoinNode join = new JoinNode(graph, scope, node.getRef(), null, condValue);
		join.recv(false, node);
		cpt.addExit(exit.wrap(join));
	    }
	}

	return cpt;
    }

    /**
     * Expands the graph for the loop variables.
     */
    public DFComponent processLoop
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt, ASTNode ast,
	 DFNode condValue, DFFrame loopFrame, DFComponent loopCpt,
	 boolean preTest)
	throws UnsupportedSyntax {

	// Add four nodes for each loop variable.
	Map<DFRef, LoopBeginNode> begins = new HashMap<DFRef, LoopBeginNode>();
	Map<DFRef, LoopRepeatNode> repeats = new HashMap<DFRef, LoopRepeatNode>();
	Map<DFRef, DFNode> ends = new HashMap<DFRef, DFNode>();
	DFRef[] loopRefs = loopFrame.getInsAndOuts();
	for (DFRef ref : loopRefs) {
	    DFNode src = cpt.getValue(ref);
	    LoopBeginNode begin = new LoopBeginNode(graph, scope, ref, ast, src);
	    LoopRepeatNode repeat = new LoopRepeatNode(graph, scope, ref, ast);
	    LoopEndNode end = new LoopEndNode(graph, scope, ref, ast, condValue);
	    begin.setEnd(end);
	    end.setBegin(begin);
	    begins.put(ref, begin);
	    ends.put(ref, end);
	    repeats.put(ref, repeat);
	}

	if (preTest) {  // Repeat -> [S] -> Begin -> End
	    // Connect the repeats to the loop inputs.
	    for (DFRef ref : loopCpt.getInputRefs()) {
		DFNode input = loopCpt.getInput(ref);
		DFNode src = repeats.get(ref);
		if (src == null) {
		    src = cpt.getValue(ref);
		}
		input.accept(src);
	    }
	    // Connect the loop outputs to the begins.
	    for (DFRef ref : loopCpt.getOutputRefs()) {
		DFNode output = loopCpt.getOutput(ref);
		LoopBeginNode begin = begins.get(ref);
		if (begin != null) {
		    begin.setRepeat(output);
		} else {
		    //assert !loopRefs.contains(ref);
		    cpt.setOutput(output);
		}
	    }
	    // Connect the beings and ends.
	    for (DFRef ref : loopRefs) {
		LoopBeginNode begin = begins.get(ref);
		DFNode end = ends.get(ref);
		end.accept(begin);
	    }

	} else {  // Begin -> [S] -> End -> Repeat
	    // Connect the begins to the loop inputs.
	    for (DFRef ref : loopCpt.getInputRefs()) {
		DFNode input = loopCpt.getInput(ref);
		DFNode src = begins.get(ref);
		if (src == null) {
		    src = cpt.getValue(ref);
		}
		input.accept(src);
	    }
	    // Connect the loop outputs to the ends.
	    for (DFRef ref : loopCpt.getOutputRefs()) {
		DFNode output = loopCpt.getOutput(ref);
		DFNode dst = ends.get(ref);
		if (dst != null) {
		    dst.accept(output);
		} else {
		    //assert !loopRefs.contains(ref);
		    cpt.setOutput(output);
		}
	    }
	    // Connect the repeats and begins.
	    for (DFRef ref : loopRefs) {
		LoopRepeatNode repeat = repeats.get(ref);
		LoopBeginNode begin = begins.get(ref);
		begin.setRepeat(repeat);
	    }
	}

	// Redirect the continue statements.
	for (DFExit exit : loopCpt.getExits()) {
	    if (exit.isCont() && exit.getFrame() == loopFrame) {
		DFNode node = exit.getNode();
		DFNode end = ends.get(node.getRef());
		if (end == null) {
		    end = cpt.getValue(node.getRef());
		}
		if (node instanceof JoinNode) {
		    ((JoinNode)node).close(end);
		}
		ends.put(node.getRef(), node);
	    } else {
		cpt.addExit(exit);
	    }
	}

	// Closing the loop.
	for (DFRef ref : loopRefs) {
	    DFNode end = ends.get(ref);
	    LoopRepeatNode repeat = repeats.get(ref);
	    cpt.setOutput(end);
	    repeat.setLoop(end);
	}

	return cpt;
    }

    /**
     * Creates a new variable node.
     */
    public DFComponent processVariableDeclaration
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 List<VariableDeclarationFragment> frags)
	throws UnsupportedSyntax {

	for (VariableDeclarationFragment frag : frags) {
	    DFRef ref = scope.lookupVar(frag.getName());
	    Expression init = frag.getInitializer();
	    if (init != null) {
		cpt = processExpression(graph, scope, frame, cpt, init);
		DFNode assign = new SingleAssignNode(graph, scope, ref, frag);
		assign.accept(cpt.getRValue());
		cpt.setOutput(assign);
	    }
	}
	return cpt;
    }

    /**
     * Creates an assignment node.
     */
    @SuppressWarnings("unchecked")
    public DFComponent processAssignment
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 Expression expr)
	throws UnsupportedSyntax {

	if (expr instanceof Name) {
	    Name name = (Name)expr;
	    if (name.isSimpleName()) {
		DFRef ref = scope.lookupVar((SimpleName)name);
		cpt.setLValue(new SingleAssignNode(graph, scope, ref, expr));
	    } else {
		// QualifiedName == FieldAccess
		QualifiedName qn = (QualifiedName)name;
		SimpleName fieldName = qn.getName();
		cpt = processExpression(graph, scope, frame, cpt, qn.getQualifier());
		DFNode obj = cpt.getRValue();
		DFRef ref = scope.lookupField(fieldName);
		cpt.setLValue(new FieldAssignNode(graph, scope, ref, expr, obj));
	    }

	} else if (expr instanceof ArrayAccess) {
	    ArrayAccess aa = (ArrayAccess)expr;
	    cpt = processExpression(graph, scope, frame, cpt, aa.getArray());
	    DFNode array = cpt.getRValue();
	    cpt = processExpression(graph, scope, frame, cpt, aa.getIndex());
	    DFNode index = cpt.getRValue();
	    DFRef ref = scope.lookupArray();
	    cpt.setLValue(new ArrayAssignNode(graph, scope, ref, expr, array, index));

	} else if (expr instanceof FieldAccess) {
	    FieldAccess fa = (FieldAccess)expr;
	    SimpleName fieldName = fa.getName();
	    cpt = processExpression(graph, scope, frame, cpt, fa.getExpression());
	    DFNode obj = cpt.getRValue();
	    DFRef ref = scope.lookupField(fieldName);
	    cpt.setLValue(new FieldAssignNode(graph, scope, ref, expr, obj));

	} else {
	    throw new UnsupportedSyntax(expr);
	}

	return cpt;
    }

    /**
     * Creates a value node.
     */
    @SuppressWarnings("unchecked")
    public DFComponent processExpression
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 Expression expr)
	throws UnsupportedSyntax {

	if (expr instanceof Annotation) {

	} else if (expr instanceof Name) {
	    Name name = (Name)expr;
	    if (name.isSimpleName()) {
		DFRef ref = scope.lookupVar((SimpleName)name);
		cpt.setRValue(new VarRefNode(graph, scope, ref, expr, cpt.getValue(ref)));
	    } else {
		// QualifiedName == FieldAccess
		QualifiedName qn = (QualifiedName)name;
		SimpleName fieldName = qn.getName();
		cpt = processExpression(graph, scope, frame, cpt, qn.getQualifier());
		DFNode obj = cpt.getRValue();
		DFRef ref = scope.lookupField(fieldName);
		cpt.setRValue(new FieldAccessNode(graph, scope, ref, qn,
						  cpt.getValue(ref), obj));
	    }

	} else if (expr instanceof ThisExpression) {
	    DFRef ref = scope.lookupThis();
	    cpt.setRValue(new VarRefNode(graph, scope, ref, expr, cpt.getValue(ref)));

	} else if (expr instanceof BooleanLiteral) {
	    boolean value = ((BooleanLiteral)expr).booleanValue();
	    cpt.setRValue(new ConstNode(graph, scope, expr, Boolean.toString(value)));

	} else if (expr instanceof CharacterLiteral) {
	    char value = ((CharacterLiteral)expr).charValue();
	    cpt.setRValue(new ConstNode(graph, scope, expr, Character.toString(value)));

	} else if (expr instanceof NullLiteral) {
	    cpt.setRValue(new ConstNode(graph, scope, expr, "null"));

	} else if (expr instanceof NumberLiteral) {
	    String value = ((NumberLiteral)expr).getToken();
	    cpt.setRValue(new ConstNode(graph, scope, expr, value));

	} else if (expr instanceof StringLiteral) {
	    String value = ((StringLiteral)expr).getLiteralValue();
	    cpt.setRValue(new ConstNode(graph, scope, expr, value));

	} else if (expr instanceof TypeLiteral) {
	    Type value = ((TypeLiteral)expr).getType();
	    cpt.setRValue(new ConstNode(graph, scope, expr, Utils.getTypeName(value)));

	} else if (expr instanceof PrefixExpression) {
	    PrefixExpression prefix = (PrefixExpression)expr;
	    PrefixExpression.Operator op = prefix.getOperator();
	    Expression operand = prefix.getOperand();
	    cpt = processExpression(graph, scope, frame, cpt, operand);
	    if (op == PrefixExpression.Operator.INCREMENT ||
		op == PrefixExpression.Operator.DECREMENT) {
		cpt = processAssignment(graph, scope, frame, cpt, operand);
		DFNode assign = cpt.getLValue();
		DFNode value = new PrefixNode(graph, scope, assign.getRef(), expr,
                                              op, cpt.getRValue());
		assign.accept(value);
		cpt.setOutput(assign);
		cpt.setRValue(value);
	    } else {
		cpt.setRValue(new PrefixNode(graph, scope, null, expr,
                                             op, cpt.getRValue()));
	    }

	} else if (expr instanceof PostfixExpression) {
	    PostfixExpression postfix = (PostfixExpression)expr;
	    PostfixExpression.Operator op = postfix.getOperator();
	    Expression operand = postfix.getOperand();
	    cpt = processAssignment(graph, scope, frame, cpt, operand);
	    if (op == PostfixExpression.Operator.INCREMENT ||
		op == PostfixExpression.Operator.DECREMENT) {
		DFNode assign = cpt.getLValue();
		cpt = processExpression(graph, scope, frame, cpt, operand);
		assign.accept(new PostfixNode(graph, scope, assign.getRef(), expr,
                                              op, cpt.getRValue()));
		cpt.setOutput(assign);
	    }

	} else if (expr instanceof InfixExpression) {
	    InfixExpression infix = (InfixExpression)expr;
	    InfixExpression.Operator op = infix.getOperator();
	    cpt = processExpression(graph, scope, frame, cpt, infix.getLeftOperand());
	    DFNode lvalue = cpt.getRValue();
	    cpt = processExpression(graph, scope, frame, cpt, infix.getRightOperand());
	    DFNode rvalue = cpt.getRValue();
	    cpt.setRValue(new InfixNode(graph, scope, expr, op, lvalue, rvalue));

	} else if (expr instanceof ParenthesizedExpression) {
	    ParenthesizedExpression paren = (ParenthesizedExpression)expr;
	    cpt = processExpression(graph, scope, frame, cpt, paren.getExpression());

	} else if (expr instanceof Assignment) {
	    Assignment assn = (Assignment)expr;
	    Assignment.Operator op = assn.getOperator();
	    cpt = processAssignment(graph, scope, frame, cpt, assn.getLeftHandSide());
	    DFNode assign = cpt.getLValue();
	    cpt = processExpression(graph, scope, frame, cpt, assn.getRightHandSide());
	    DFNode rvalue = cpt.getRValue();
	    DFNode lvalue = cpt.getValue(assign.getRef());
	    assign.accept(new AssignOpNode(graph, scope, assign.getRef(), assn,
                                           op, lvalue, rvalue));
	    cpt.setOutput(assign);
	    cpt.setRValue(assign);

	} else if (expr instanceof VariableDeclarationExpression) {
	    VariableDeclarationExpression decl = (VariableDeclarationExpression)expr;
	    cpt = processVariableDeclaration
		(graph, scope, frame, cpt, decl.fragments());

	} else if (expr instanceof MethodInvocation) {
	    MethodInvocation invoke = (MethodInvocation)expr;
	    Expression expr1 = invoke.getExpression();
	    DFNode obj = null;
	    if (expr1 != null) {
		cpt = processExpression(graph, scope, frame, cpt, expr1);
		obj = cpt.getRValue();
	    }
	    SimpleName methodName = invoke.getName();
	    MethodCallNode call = new MethodCallNode
		(graph, scope, invoke, obj, methodName);
	    for (Expression arg : (List<Expression>) invoke.arguments()) {
		cpt = processExpression(graph, scope, frame, cpt, arg);
		call.addArg(cpt.getRValue());
	    }
	    cpt.setRValue(call);
            if (call.exception != null) {
		DFFrame dstFrame = frame.find(DFFrame.TRY);
		cpt.addExit(new DFExit(call.exception, dstFrame));
            }

	} else if (expr instanceof SuperMethodInvocation) {
	    SuperMethodInvocation si = (SuperMethodInvocation)expr;
	    SimpleName methodName = si.getName();
	    DFNode obj = cpt.getValue(scope.lookupSuper());
	    MethodCallNode call = new MethodCallNode
		(graph, scope, si, obj, methodName);
	    for (Expression arg : (List<Expression>) si.arguments()) {
		cpt = processExpression(graph, scope, frame, cpt, arg);
		call.addArg(cpt.getRValue());
	    }
	    cpt.setRValue(call);

	} else if (expr instanceof ArrayCreation) {
	    ArrayCreation ac = (ArrayCreation)expr;
	    for (Expression dim : (List<Expression>) ac.dimensions()) {
		// XXX cpt.getRValue() is not used (for now).
		cpt = processExpression(graph, scope, frame, cpt, dim);
	    }
	    ArrayInitializer init = ac.getInitializer();
	    if (init != null) {
		cpt = processExpression(graph, scope, frame, cpt, init);
	    } else {
		cpt.setRValue(new ArrayValueNode(graph, scope, ac));
	    }

	} else if (expr instanceof ArrayInitializer) {
	    ArrayInitializer init = (ArrayInitializer)expr;
	    ArrayValueNode arr = new ArrayValueNode(graph, scope, init);
	    for (Expression expr1 : (List<Expression>) init.expressions()) {
		cpt = processExpression(graph, scope, frame, cpt, expr1);
		arr.addValue(cpt.getRValue());
	    }
	    cpt.setRValue(arr);
	    // XXX array ref is not used.

	} else if (expr instanceof ArrayAccess) {
	    ArrayAccess aa = (ArrayAccess)expr;
	    DFRef ref = scope.lookupArray();
	    cpt = processExpression(graph, scope, frame, cpt, aa.getArray());
	    DFNode array = cpt.getRValue();
	    cpt = processExpression(graph, scope, frame, cpt, aa.getIndex());
	    DFNode index = cpt.getRValue();
	    cpt.setRValue(new ArrayAccessNode(graph, scope, ref, aa,
					      array, index, cpt.getValue(ref)));

	} else if (expr instanceof FieldAccess) {
	    FieldAccess fa = (FieldAccess)expr;
	    SimpleName fieldName = fa.getName();
	    cpt = processExpression(graph, scope, frame, cpt, fa.getExpression());
	    DFNode obj = cpt.getRValue();
	    DFRef ref = scope.lookupField(fieldName);
	    cpt.setRValue(new FieldAccessNode(graph, scope, ref, fa,
					      cpt.getValue(ref), obj));

	} else if (expr instanceof SuperFieldAccess) {
	    SuperFieldAccess sfa = (SuperFieldAccess)expr;
	    SimpleName fieldName = sfa.getName();
	    DFNode obj = cpt.getValue(scope.lookupSuper());
	    DFRef ref = scope.lookupField(fieldName);
	    cpt.setRValue(new FieldAccessNode(graph, scope, ref, sfa,
					      cpt.getValue(ref), obj));

	} else if (expr instanceof CastExpression) {
	    CastExpression cast = (CastExpression)expr;
	    Type type = cast.getType();
	    cpt = processExpression(graph, scope, frame, cpt, cast.getExpression());
	    cpt.setRValue(new TypeCastNode(graph, scope, cast, type, cpt.getRValue()));

	} else if (expr instanceof ClassInstanceCreation) {
	    ClassInstanceCreation cstr = (ClassInstanceCreation)expr;
	    Type instType = cstr.getType();
	    Expression expr1 = cstr.getExpression();
	    DFNode obj = null;
	    if (expr1 != null) {
		cpt = processExpression(graph, scope, frame, cpt, expr1);
		obj = cpt.getRValue();
	    }
	    CreateObjectNode call = new CreateObjectNode(graph, scope, cstr,
                                                         obj, instType);
	    for (Expression arg : (List<Expression>) cstr.arguments()) {
		cpt = processExpression(graph, scope, frame, cpt, arg);
		call.addArg(cpt.getRValue());
	    }
	    cpt.setRValue(call);
	    // Ignore getAnonymousClassDeclaration() here.
	    // It will eventually be picked up as MethodDeclaration.

	} else if (expr instanceof ConditionalExpression) {
	    ConditionalExpression cond = (ConditionalExpression)expr;
	    cpt = processExpression(graph, scope, frame, cpt, cond.getExpression());
	    DFNode condValue = cpt.getRValue();
	    cpt = processExpression(graph, scope, frame, cpt, cond.getThenExpression());
	    DFNode trueValue = cpt.getRValue();
	    cpt = processExpression(graph, scope, frame, cpt, cond.getElseExpression());
	    DFNode falseValue = cpt.getRValue();
	    JoinNode join = new JoinNode(graph, scope, null, expr, condValue);
	    join.recv(true, trueValue);
	    join.recv(false, falseValue);
	    cpt.setRValue(join);

	} else if (expr instanceof InstanceofExpression) {
	    InstanceofExpression instof = (InstanceofExpression)expr;
	    Type type = instof.getRightOperand();
	    cpt = processExpression(graph, scope, frame, cpt, instof.getLeftOperand());
	    cpt.setRValue(new InstanceofNode(graph, scope, instof, type, cpt.getRValue()));

	} else {
	    // LambdaExpression
	    // MethodReference
	    //  CreationReference
	    //  ExpressionMethodReference
	    //  SuperMethodReference
	    //  TypeMethodReference

	    throw new UnsupportedSyntax(expr);
	}

	return cpt;
    }

    /// Statement processors.
    @SuppressWarnings("unchecked")
    public DFComponent processBlock
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 Block block)
	throws UnsupportedSyntax {
	DFScope childScope = scope.getChildByAST(block);
	for (Statement cstmt : (List<Statement>) block.statements()) {
	    cpt = processStatement(graph, childScope, frame, cpt, cstmt);
	}
	return cpt;
    }

    @SuppressWarnings("unchecked")
    public DFComponent processVariableDeclarationStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 VariableDeclarationStatement varStmt)
	throws UnsupportedSyntax {
	return processVariableDeclaration
	    (graph, scope, frame, cpt, varStmt.fragments());
    }

    public DFComponent processExpressionStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 ExpressionStatement exprStmt)
	throws UnsupportedSyntax {
	Expression expr = exprStmt.getExpression();
	return processExpression(graph, scope, frame, cpt, expr);
    }

    public DFComponent processIfStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 IfStatement ifStmt)
	throws UnsupportedSyntax {
	Expression expr = ifStmt.getExpression();
	cpt = processExpression(graph, scope, frame, cpt, expr);
	DFNode condValue = cpt.getRValue();

	Statement thenStmt = ifStmt.getThenStatement();
	DFComponent thenCpt = new DFComponent(graph, scope);
	thenCpt = processStatement(graph, scope, frame, thenCpt, thenStmt);

	Statement elseStmt = ifStmt.getElseStatement();
	DFComponent elseCpt = null;
	if (elseStmt != null) {
	    elseCpt = new DFComponent(graph, scope);
	    elseCpt = processStatement(graph, scope, frame, elseCpt, elseStmt);
	}
	return processConditional(graph, scope, frame, cpt, ifStmt,
				  condValue, thenCpt, elseCpt);
    }

    private DFComponent processCaseStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 ASTNode apt, DFNode caseNode, DFComponent caseCpt) {

	for (DFRef ref : caseCpt.getInputRefs()) {
	    DFNode src = caseCpt.getInput(ref);
	    src.accept(cpt.getValue(ref));
	}

	for (DFRef ref : caseCpt.getOutputRefs()) {
	    DFNode dst = caseCpt.getOutput(ref);
	    JoinNode join = new JoinNode(graph, scope, ref, apt, caseNode);
	    join.recv(true, dst);
	    join.close(cpt.getValue(ref));
	    cpt.setOutput(join);
	}

	return cpt;
    }

    @SuppressWarnings("unchecked")
    public DFComponent processSwitchStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 SwitchStatement switchStmt)
	throws UnsupportedSyntax {
	DFScope switchScope = scope.getChildByAST(switchStmt);
	DFFrame switchFrame = frame.getChildByAST(switchStmt);
	cpt = processExpression(graph, scope, frame, cpt, switchStmt.getExpression());
	DFNode switchValue = cpt.getRValue();

	SwitchCase switchCase = null;
	CaseNode caseNode = null;
	DFComponent caseCpt = null;
	for (Statement stmt : (List<Statement>) switchStmt.statements()) {
	    if (stmt instanceof SwitchCase) {
		if (caseCpt != null) {
		    // switchCase, caseNode and caseCpt must be non-null.
		    cpt = processCaseStatement(graph, switchScope, switchFrame, cpt,
					       switchCase, caseNode, caseCpt);
		}
		switchCase = (SwitchCase)stmt;
		caseNode = new CaseNode(graph, switchScope, stmt, switchValue);
		caseCpt = new DFComponent(graph, switchScope);
		Expression expr = switchCase.getExpression();
		if (expr != null) {
		    cpt = processExpression(graph, switchScope, frame, cpt, expr);
		    caseNode.addMatch(cpt.getRValue());
		} else {
		    // "default" case.
		}
	    } else {
		if (caseCpt == null) {
		    // no "case" statement.
		    throw new UnsupportedSyntax(stmt);
		}
		caseCpt = processStatement(graph, switchScope, switchFrame, caseCpt, stmt);
	    }
	}
	if (caseCpt != null) {
	    cpt = processCaseStatement(graph, switchScope, switchFrame, cpt,
				       switchCase, caseNode, caseCpt);
	}
	cpt.endFrame(switchFrame);
	return cpt;
    }

    public DFComponent processWhileStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 WhileStatement whileStmt)
	throws UnsupportedSyntax {
	DFScope loopScope = scope.getChildByAST(whileStmt);
	DFFrame loopFrame = frame.getChildByAST(whileStmt);
	DFComponent loopCpt = new DFComponent(graph, loopScope);
	loopCpt = processExpression(graph, loopScope, frame, loopCpt,
				    whileStmt.getExpression());
	DFNode condValue = loopCpt.getRValue();
	loopCpt = processStatement(graph, loopScope, loopFrame, loopCpt,
				   whileStmt.getBody());
	cpt = processLoop(graph, loopScope, frame, cpt, whileStmt,
			  condValue, loopFrame, loopCpt, true);
	cpt.endFrame(loopFrame);
	return cpt;
    }

    public DFComponent processDoStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 DoStatement doStmt)
	throws UnsupportedSyntax {
	DFScope loopScope = scope.getChildByAST(doStmt);
	DFFrame loopFrame = frame.getChildByAST(doStmt);
	DFComponent loopCpt = new DFComponent(graph, loopScope);
	loopCpt = processStatement(graph, loopScope, loopFrame, loopCpt,
				   doStmt.getBody());
	loopCpt = processExpression(graph, loopScope, loopFrame, loopCpt,
				    doStmt.getExpression());
	DFNode condValue = loopCpt.getRValue();
	cpt = processLoop(graph, loopScope, frame, cpt, doStmt,
			  condValue, loopFrame, loopCpt, false);
	cpt.endFrame(loopFrame);
	return cpt;
    }

    @SuppressWarnings("unchecked")
    public DFComponent processForStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 ForStatement forStmt)
	throws UnsupportedSyntax {
	DFScope loopScope = scope.getChildByAST(forStmt);
	DFFrame loopFrame = frame.getChildByAST(forStmt);
	DFComponent loopCpt = new DFComponent(graph, loopScope);
	for (Expression init : (List<Expression>) forStmt.initializers()) {
	    cpt = processExpression(graph, loopScope, frame, cpt, init);
	}
	Expression expr = forStmt.getExpression();
	DFNode condValue;
	if (expr != null) {
	    loopCpt = processExpression(graph, loopScope, loopFrame, loopCpt, expr);
	    condValue = loopCpt.getRValue();
	} else {
	    condValue = new ConstNode(graph, loopScope, null, "true");
	}
	loopCpt = processStatement(graph, loopScope, loopFrame, loopCpt,
				   forStmt.getBody());
	for (Expression update : (List<Expression>) forStmt.updaters()) {
	    loopCpt = processExpression(graph, loopScope, loopFrame, loopCpt, update);
	}
	cpt = processLoop(graph, loopScope, frame, cpt, forStmt,
			  condValue, loopFrame, loopCpt, true);
	cpt.endFrame(loopFrame);
	return cpt;
    }

    @SuppressWarnings("unchecked")
    public DFComponent processEnhancedForStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 EnhancedForStatement eForStmt)
	throws UnsupportedSyntax {
	DFScope loopScope = scope.getChildByAST(eForStmt);
	DFFrame loopFrame = frame.getChildByAST(eForStmt);
	DFComponent loopCpt = new DFComponent(graph, loopScope);
	Expression expr = eForStmt.getExpression();
	loopCpt = processExpression(graph, loopScope, frame, loopCpt, expr);
	SingleVariableDeclaration decl = eForStmt.getParameter();
	DFRef ref = loopScope.lookupVar(decl.getName());
	DFNode iterValue = new IterNode(graph, loopScope, ref, expr, loopCpt.getRValue());
	SingleAssignNode assign = new SingleAssignNode(graph, loopScope, ref, expr);
	assign.accept(iterValue);
	cpt.setOutput(assign);
	loopCpt = processStatement(graph, loopScope, loopFrame, loopCpt,
				   eForStmt.getBody());
	cpt = processLoop(graph, loopScope, frame, cpt, eForStmt,
			  iterValue, loopFrame, loopCpt, true);
	cpt.endFrame(loopFrame);
	return cpt;
    }

    @SuppressWarnings("unchecked")
    public DFComponent processStatement
	(DFGraph graph, DFScope scope, DFFrame frame, DFComponent cpt,
	 Statement stmt)
	throws UnsupportedSyntax {

	if (stmt instanceof AssertStatement) {
	    // XXX Ignore asserts.

	} else if (stmt instanceof Block) {
	    cpt = processBlock
		(graph, scope, frame, cpt, (Block)stmt);

	} else if (stmt instanceof EmptyStatement) {

	} else if (stmt instanceof VariableDeclarationStatement) {
	    cpt = processVariableDeclarationStatement
		(graph, scope, frame, cpt, (VariableDeclarationStatement)stmt);

	} else if (stmt instanceof ExpressionStatement) {
	    cpt = processExpressionStatement
		(graph, scope, frame, cpt, (ExpressionStatement)stmt);

	} else if (stmt instanceof IfStatement) {
	    cpt = processIfStatement
		(graph, scope, frame, cpt, (IfStatement)stmt);

	} else if (stmt instanceof SwitchStatement) {
	    cpt = processSwitchStatement
		(graph, scope, frame, cpt, (SwitchStatement)stmt);

	} else if (stmt instanceof SwitchCase) {
	    // Invalid "case" placement.
	    throw new UnsupportedSyntax(stmt);

	} else if (stmt instanceof WhileStatement) {
	    cpt = processWhileStatement
		(graph, scope, frame, cpt, (WhileStatement)stmt);

	} else if (stmt instanceof DoStatement) {
	    cpt = processDoStatement
		(graph, scope, frame, cpt, (DoStatement)stmt);

	} else if (stmt instanceof ForStatement) {
	    cpt = processForStatement
		(graph, scope, frame, cpt, (ForStatement)stmt);

	} else if (stmt instanceof EnhancedForStatement) {
	    cpt = processEnhancedForStatement
		(graph, scope, frame, cpt, (EnhancedForStatement)stmt);

	} else if (stmt instanceof ReturnStatement) {
            ReturnStatement rtrnStmt = (ReturnStatement)stmt;
	    DFFrame dstFrame = frame.find(DFFrame.METHOD);
            Expression expr = rtrnStmt.getExpression();
            if (expr != null) {
                cpt = processExpression(graph, scope, frame, cpt, expr);
                ReturnNode rtrn = new ReturnNode(graph, scope, rtrnStmt, cpt.getRValue());
                cpt.addExit(new DFExit(rtrn, dstFrame));
            }
	    for (DFFrame frm = frame; frm != null; frm = frm.getParent()) {
		for (DFRef ref : frm.getOutputs()) {
		    cpt.addExit(new DFExit(cpt.getValue(ref), dstFrame));
		}
		if (frm == dstFrame) break;
	    }

	} else if (stmt instanceof BreakStatement) {
	    BreakStatement breakStmt = (BreakStatement)stmt;
	    SimpleName labelName = breakStmt.getLabel();
	    String dstLabel = (labelName == null)? null : labelName.getIdentifier();
	    DFFrame dstFrame = frame.find(dstLabel);
	    for (DFFrame frm = frame; frm != null; frm = frm.getParent()) {
		for (DFRef ref : frm.getOutputs()) {
		    cpt.addExit(new DFExit(cpt.getValue(ref), dstFrame));
		}
		if (frm == dstFrame) break;
	    }

	} else if (stmt instanceof ContinueStatement) {
	    ContinueStatement contStmt = (ContinueStatement)stmt;
	    SimpleName labelName = contStmt.getLabel();
	    String dstLabel = (labelName == null)? null : labelName.getIdentifier();
	    DFFrame dstFrame = frame.find(dstLabel);
	    for (DFFrame frm = frame; frm != null; frm = frm.getParent()) {
		for (DFRef ref : frm.getOutputs()) {
		    cpt.addExit(new DFExit(cpt.getValue(ref), dstFrame, true));
		}
		if (frm == dstFrame) break;
	    }

	} else if (stmt instanceof LabeledStatement) {
	    LabeledStatement labeledStmt = (LabeledStatement)stmt;
	    DFFrame labeledFrame = frame.getChildByAST(labeledStmt);
	    cpt = processStatement(graph, scope, labeledFrame, cpt,
				   labeledStmt.getBody());

	} else if (stmt instanceof SynchronizedStatement) {
	    SynchronizedStatement syncStmt = (SynchronizedStatement)stmt;
	    cpt = processStatement(graph, scope, frame, cpt,
				   syncStmt.getBody());

	} else if (stmt instanceof TryStatement) {
	    // XXX Ignore catch statements (for now).
	    TryStatement tryStmt = (TryStatement)stmt;
	    DFFrame tryFrame = frame.getChildByAST(tryStmt);
	    cpt = processStatement(graph, scope, tryFrame, cpt,
				   tryStmt.getBody());
	    Block finBlock = tryStmt.getFinally();
	    if (finBlock != null) {
		cpt = processStatement(graph, scope, frame, cpt, finBlock);
	    }

	} else if (stmt instanceof ThrowStatement) {
	    ThrowStatement throwStmt = (ThrowStatement)stmt;
	    cpt = processExpression(graph, scope, frame, cpt, throwStmt.getExpression());
            ExceptionNode exception = new ExceptionNode(graph, scope, stmt, cpt.getRValue());
	    DFFrame dstFrame = frame.find(DFFrame.TRY);
	    cpt.addExit(new DFExit(exception, dstFrame));
	    for (DFFrame frm = frame; frm != null; frm = frm.getParent()) {
		for (DFRef ref : frm.getOutputs()) {
		    cpt.addExit(new DFExit(cpt.getValue(ref), dstFrame, true));
		}
		if (frm == dstFrame) break;
	    }

	} else if (stmt instanceof ConstructorInvocation) {
	    // XXX Ignore all side effects.
	    ConstructorInvocation ci = (ConstructorInvocation)stmt;
	    for (Expression arg : (List<Expression>) ci.arguments()) {
		cpt = processExpression(graph, scope, frame, cpt, arg);
	    }

	} else if (stmt instanceof SuperConstructorInvocation) {
	    // XXX Ignore all side effects.
	    SuperConstructorInvocation sci = (SuperConstructorInvocation)stmt;
	    for (Expression arg : (List<Expression>) sci.arguments()) {
		cpt = processExpression(graph, scope, frame, cpt, arg);
	    }

	} else if (stmt instanceof TypeDeclarationStatement) {
	    // Ignore TypeDeclarationStatement because
	    // it was eventually picked up as MethodDeclaration.

	} else {
	    throw new UnsupportedSyntax(stmt);
	}

	return cpt;
    }

    /**
     * Creates a graph for an entire method.
     */
    @SuppressWarnings("unchecked")
    public DFComponent buildMethodDeclaration
	(DFGraph graph, DFScope scope, MethodDeclaration method)
	throws UnsupportedSyntax {

	DFComponent cpt = new DFComponent(graph, scope);
	// XXX Ignore isContructor().
	// XXX Ignore getReturnType2().
	// XXX Ignore isVarargs().
	int i = 0;
	for (SingleVariableDeclaration decl :
		 (List<SingleVariableDeclaration>) method.parameters()) {
	    // XXX Ignore modifiers and dimensions.
	    Type paramType = decl.getType();
	    DFRef ref = scope.addVar(decl.getName(), paramType);
	    DFNode param = new ArgNode(graph, scope, ref, decl, i++);
	    DFNode assign = new SingleAssignNode(graph, scope, ref, decl);
	    assign.accept(param);
	    cpt.setOutput(assign);
	}
	return cpt;
    }

    /// Top-level functions.

    /**
     * Performs dataflow analysis for a given method.
     */
    public DFGraph getMethodGraph(DFScope scope, MethodDeclaration method)
	throws UnsupportedSyntax {
	SimpleName funcName = method.getName();
	Block funcBlock = method.getBody();

	DFGraph graph = new DFGraph(funcName);

	// Setup an initial scope.
	DFFrame frame = new DFFrame(DFFrame.METHOD);
        graph.setRoot(scope);

	DFComponent cpt = buildMethodDeclaration(graph, scope, method);
	scope.build(frame, funcBlock);
	//scope.dump();
	//frame.dump();

	// Process the function body.
	cpt = processStatement(graph, scope, frame, cpt, funcBlock);

	cpt.endFrame(frame);
	return graph;
    }

    /// ASTVisitor methods.

    public Exporter exporter;
    public String[] classPath;
    public String[] srcPath;
    public boolean resolve;

    public Java2DF(Exporter exporter, String[] classPath, String[] srcPath,
		   boolean resolve) {
	this.exporter = exporter;
	this.classPath = classPath;
	this.srcPath = srcPath;
	this.resolve = resolve;
    }

    public DFGraph processMethodDeclaration(DFScope scope, MethodDeclaration method)
        throws UnsupportedSyntax {
	// Ignore method prototypes.
	if (method.getBody() == null) return null;
        scope = scope.getChildByName(method.getName());
	String funcName = method.getName().getIdentifier();
        try {
            DFGraph graph = getMethodGraph(scope, method);
            if (graph != null) {
                Utils.logit("Success: "+funcName);
                // Remove redundant nodes.
                graph.cleanup();
            }
            return graph;
        } catch (UnsupportedSyntax e) {
            //e.printStackTrace();
            e.name = funcName;
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public void processTypeDeclaration(DFScope scope, TypeDeclaration typedecl)
        throws IOException {
        scope = scope.getChildByName(typedecl.getName());
        // XXX superclass
        for (BodyDeclaration body : (List<BodyDeclaration>) typedecl.bodyDeclarations()) {
            if (body instanceof TypeDeclaration) {
                processTypeDeclaration(scope, (TypeDeclaration)body);
            } else if (body instanceof FieldDeclaration) {
                // XXX
                // XXX static
            } else if (body instanceof MethodDeclaration) {
                try {
                    DFGraph graph = processMethodDeclaration(scope, (MethodDeclaration)body);
                    if (this.exporter != null && graph != null) {
                        this.exporter.writeGraph(graph);
                    }
                } catch (UnsupportedSyntax e) {
                    String astName = e.ast.getClass().getName();
                    Utils.logit("Fail: "+e.name+" (Unsupported: "+astName+") "+e.ast);
                    if (this.exporter != null) {
                        this.exporter.writeError(e.name, astName);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void processFile(String path)
	throws IOException {
	Utils.logit("Parsing: "+path);
	String src = Utils.readFile(path);
	Map<String, String> options = JavaCore.getOptions();
	JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
	ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setUnitName(path);
	parser.setSource(src.toCharArray());
	parser.setKind(ASTParser.K_COMPILATION_UNIT);
	parser.setResolveBindings(this.resolve);
	parser.setEnvironment(this.classPath, this.srcPath, null, this.resolve);
	parser.setCompilerOptions(options);
	CompilationUnit cu = (CompilationUnit)parser.createAST(null);
        PackageDeclaration pkg = cu.getPackage();
        DFScope scope = new DFScope();
        if (pkg != null) {
            scope = scope.getChildByName(pkg.getName());
        }
        for (TypeDeclaration typedecl : (List<TypeDeclaration>) cu.types()) {
            processTypeDeclaration(scope, typedecl);
        }
	scope.dump();
    }

    /**
     * Provides a command line interface.
     *
     * Usage: java Java2DF [-o output] input.java ...
     */
    public static void main(String[] args)
	throws IOException {

	// Parse the options.
	String[] classpath = null;
	String[] srcpath = null;
	String tmppath = null;
	boolean resolve = false;
	List<String> files = new ArrayList<String>();
	OutputStream output = System.out;

	for (int i = 0; i < args.length; i++) {
	    String arg = args[i];
	    if (arg.equals("--")) {
		for (; i < args.length; i++) {
		    files.add(args[i]);
		}
	    } else if (arg.equals("-o")) {
		String path = args[++i];
		try {
		    output = new FileOutputStream(path);
		    Utils.logit("Exporting: "+path);
		} catch (IOException e) {
		    System.err.println("Cannot open output file: "+path);
		}
	    } else if (arg.equals("-C")) {
		classpath = args[++i].split(";");
	    } else if (arg.equals("-R")) {
		tmppath = args[++i];
	    } else if (arg.startsWith("-")) {
		;
	    } else {
		files.add(arg);
	    }
	}

	// Preprocess files.
        Map<String, String> srcmap = null;
	if (tmppath != null) {
            srcmap = new HashMap<String, String>();
	    for (int i = 0; i < files.size(); i++) {
		String src = files.get(i);
                String dst;
		try {
		    String name = PackageNameExtractor.getCanonicalName(src);
                    if (name == null) {
			Utils.logit("Error: no toplevel name: "+src);
			continue;
		    }
                    dst = tmppath + "/" + name.replace(".", "/") + ".java";
		} catch (IOException e) {
		    System.err.println("Cannot open input file: "+src);
                    continue;
		}
                Utils.logit("Copying: "+src+" -> "+dst);
                try {
                    File srcfile = new File(src);
                    File dstfile = new File(dst);
                    dstfile.getParentFile().mkdirs();
                    Utils.copyFile(srcfile, dstfile);
                    srcmap.put(src, dst);
		} catch (IOException e) {
		    System.err.println("Cannot copy: "+e);
                    continue;
                }
	    }
	    srcpath = new String[] { tmppath };
	    resolve = true;
	}

	// Process files.
	XmlExporter exporter = new XmlExporter();
	for (String path : files) {
            String src = (srcmap != null)? srcmap.get(path) : path;
	    if (src == null) continue;
	    try {
		Java2DF converter =
		    new Java2DF(exporter, classpath, srcpath, resolve);
		exporter.startFile(path);
		converter.processFile(src);
		exporter.endFile();
	    } catch (IOException e) {
		System.err.println("Cannot open input file: "+src);
	    }
	}
	exporter.close();
	Utils.printXml(output, exporter.document);
	output.close();

	// Cleanup files.
	if (srcmap != null) {
	    for (String dst : srcmap.values()) {
                Utils.logit("Deleting: "+dst);
		File file = new File(dst);
		file.delete();
	    }
	}
    }
}
