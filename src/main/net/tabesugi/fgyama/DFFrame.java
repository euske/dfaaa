//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFFrame
//
public class DFFrame {

    private String _label;
    private DFFrame _parent;

    private Map<String, DFFrame> _ast2child =
        new HashMap<String, DFFrame>();
    private SortedSet<DFRef> _inputRefs =
        new TreeSet<DFRef>();
    private SortedSet<DFRef> _outputRefs =
        new TreeSet<DFRef>();
    private List<DFExit> _exits =
        new ArrayList<DFExit>();

    private SortedSet<DFNode> _inputNodes =
        new TreeSet<DFNode>();
    private SortedSet<DFNode> _outputNodes =
        new TreeSet<DFNode>();

    public static final String ANONYMOUS = "@ANONYMOUS";
    public static final String BREAKABLE = "@BREAKABLE";
    public static final String CATCHABLE = "@CATCHABLE";
    public static final String RETURNABLE = "@RETURNABLE";

    public DFFrame(String label) {
        this(label, null);
    }

    public DFFrame(String label, DFFrame parent) {
        assert label != null;
        _label = label;
        _parent = parent;
    }

    @Override
    public String toString() {
        return ("<DFFrame("+_label+")>");
    }

    private DFFrame addChild(String label, ASTNode ast) {
        DFFrame frame = new DFFrame(label, this);
        _ast2child.put(Utils.encodeASTNode(ast), frame);
        return frame;
    }

    public String getLabel() {
        return _label;
    }

    public DFFrame getParent() {
        return _parent;
    }

    public DFFrame getChildByAST(ASTNode ast) {
        String key = Utils.encodeASTNode(ast);
        assert _ast2child.containsKey(key);
        return _ast2child.get(key);
    }

    public DFFrame find(String label) {
        DFFrame frame = this;
        while (frame != null) {
            if (frame.getLabel().equals(label)) break;
            frame = frame.getParent();
        }
        return frame;
    }

    public boolean expandRefs(DFFrame childFrame) {
        boolean added = false;
        for (DFRef ref : childFrame._inputRefs) {
            if (ref.isLocal() || ref.isInternal()) continue;
            if (!_inputRefs.contains(ref)) {
                _inputRefs.add(ref);
                added = true;
            }
        }
        for (DFRef ref : childFrame._outputRefs) {
            if (ref.isLocal() || ref.isInternal()) continue;
            if (!_outputRefs.contains(ref)) {
                _outputRefs.add(ref);
                added = true;
            }
        }
        return added;
    }

    private void addInputRef(DFRef ref) {
        _inputRefs.add(ref);
    }

    private void addOutputRef(DFRef ref) {
        _outputRefs.add(ref);
    }

    private void expandLocalRefs(DFFrame childFrame) {
        _inputRefs.addAll(childFrame._inputRefs);
        _outputRefs.addAll(childFrame._outputRefs);
    }

    private void removeRefs(DFVarScope childScope) {
        for (DFRef ref : childScope.getRefs()) {
            _inputRefs.remove(ref);
            _outputRefs.remove(ref);
        }
    }

    public SortedSet<DFRef> getInputRefs() {
        return _inputRefs;
    }

    public SortedSet<DFRef> getOutputRefs() {
        return _outputRefs;
    }

    public DFExit[] getExits() {
        DFExit[] exits = new DFExit[_exits.size()];
        _exits.toArray(exits);
        return exits;
    }

    public void addExit(DFExit exit) {
        //Logger.info("DFFrame.addExit:", this, ":", exit);
        _exits.add(exit);
    }

    public void close(DFContext ctx) {
        for (DFExit exit : _exits) {
            if (exit.getFrame() == this) {
                //Logger.info("DFFrame.Exit:", this, ":", exit);
                DFNode node = exit.getNode();
                node.close(ctx.get(node.getRef()));
                ctx.set(node);
            }
        }
        for (DFRef ref : _inputRefs) {
            DFNode node = ctx.getFirst(ref);
            assert node != null;
            _inputNodes.add(node);
        }
        for (DFRef ref : _outputRefs) {
            DFNode node = ctx.get(ref);
            assert node != null;
            _outputNodes.add(node);
        }
    }

    public SortedSet<DFNode> getInputNodes() {
        SortedSet<DFNode> nodes = new TreeSet<DFNode>();
        for (DFNode node : _inputNodes) {
            DFRef ref = node.getRef();
            if (!ref.isLocal() || ref.isInternal()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    public SortedSet<DFNode> getOutputNodes() {
        SortedSet<DFNode> nodes = new TreeSet<DFNode>();
        for (DFNode node : _outputNodes) {
            DFRef ref = node.getRef();
            if (!ref.isLocal() || ref.isInternal()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    public void buildMethodDecl(
        DFTypeFinder finder, DFMethod method, DFVarScope scope,
        MethodDeclaration methodDecl)
        throws UnsupportedSyntax, EntityNotFound {
        int i = 0;
        for (SingleVariableDeclaration decl :
                 (List<SingleVariableDeclaration>) methodDecl.parameters()) {
            // XXX Ignore modifiers.
            DFRef ref = scope.lookupArgument(i);
            this.addInputRef(ref);
            i++;
        }
        this.buildStmt(finder, method, scope, methodDecl.getBody());
    }

    public void buildInitializer(
        DFTypeFinder finder, DFMethod method, DFVarScope scope,
        Initializer initializer)
        throws UnsupportedSyntax, EntityNotFound {
        this.buildStmt(finder, method, scope, initializer.getBody());
    }

    @SuppressWarnings("unchecked")
    private void buildStmt(
        DFTypeFinder finder, DFMethod method, DFVarScope scope,
        Statement stmt)
        throws UnsupportedSyntax, EntityNotFound {
        assert stmt != null;

        if (stmt instanceof AssertStatement) {

        } else if (stmt instanceof Block) {
            DFVarScope childScope = scope.getChildByAST(stmt);
            Block block = (Block)stmt;
            for (Statement cstmt :
                     (List<Statement>) block.statements()) {
                this.buildStmt(finder, method, childScope, cstmt);
            }
            this.removeRefs(childScope);

        } else if (stmt instanceof EmptyStatement) {

        } else if (stmt instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement varStmt =
                (VariableDeclarationStatement)stmt;
            for (VariableDeclarationFragment frag :
                     (List<VariableDeclarationFragment>) varStmt.fragments()) {
                DFRef ref = scope.lookupVar(frag.getName());
                this.addOutputRef(ref);
                Expression init = frag.getInitializer();
                if (init != null) {
                    this.buildExpr(finder, method, scope, init);
                }
            }

        } else if (stmt instanceof ExpressionStatement) {
            ExpressionStatement exprStmt = (ExpressionStatement)stmt;
            this.buildExpr(finder, method, scope, exprStmt.getExpression());

        } else if (stmt instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement)stmt;
            this.buildExpr(finder, method, scope, ifStmt.getExpression());
            Statement thenStmt = ifStmt.getThenStatement();
            DFFrame thenFrame = this.addChild(DFFrame.ANONYMOUS, thenStmt);
            thenFrame.buildStmt(finder, method, scope, thenStmt);
            this.expandLocalRefs(thenFrame);
            Statement elseStmt = ifStmt.getElseStatement();
            if (elseStmt != null) {
                DFFrame elseFrame = this.addChild(DFFrame.ANONYMOUS, elseStmt);
                elseFrame.buildStmt(finder, method, scope, elseStmt);
                this.expandLocalRefs(elseFrame);
            }

        } else if (stmt instanceof SwitchStatement) {
            SwitchStatement switchStmt = (SwitchStatement)stmt;
            DFType type = this.buildExpr(
                finder, method, scope, switchStmt.getExpression());
            DFKlass enumKlass = null;
            if (type instanceof DFKlass &&
                ((DFKlass)type).isEnum()) {
                enumKlass = finder.resolveKlass(type);
                enumKlass.load();
            }
            DFVarScope childScope = scope.getChildByAST(stmt);
            DFFrame childFrame = this.addChild(DFFrame.BREAKABLE, stmt);
            for (Statement cstmt :
                     (List<Statement>) switchStmt.statements()) {
                if (cstmt instanceof SwitchCase) {
                    SwitchCase switchCase = (SwitchCase)cstmt;
                    Expression expr = switchCase.getExpression();
                    if (expr != null) {
                        if (enumKlass != null && expr instanceof SimpleName) {
                            // special treatment for enum.
                            DFRef ref = enumKlass.lookupField((SimpleName)expr);
                            this.addInputRef(ref);
                        } else {
                            childFrame.buildExpr(finder, method, childScope, expr);
                        }
                    }
                } else {
                    childFrame.buildStmt(finder, method, childScope, cstmt);
                }
            }
            childFrame.removeRefs(childScope);
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof WhileStatement) {
            WhileStatement whileStmt = (WhileStatement)stmt;
            DFVarScope childScope = scope.getChildByAST(stmt);
            DFFrame childFrame = this.addChild(DFFrame.BREAKABLE, stmt);
            childFrame.buildExpr(finder, method, scope, whileStmt.getExpression());
            childFrame.buildStmt(finder, method, childScope, whileStmt.getBody());
            childFrame.removeRefs(childScope);
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof DoStatement) {
            DoStatement doStmt = (DoStatement)stmt;
            DFVarScope childScope = scope.getChildByAST(stmt);
            DFFrame childFrame = this.addChild(DFFrame.BREAKABLE, stmt);
            childFrame.buildStmt(finder, method, childScope, doStmt.getBody());
            childFrame.buildExpr(finder, method, scope, doStmt.getExpression());
            childFrame.removeRefs(childScope);
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof ForStatement) {
            ForStatement forStmt = (ForStatement)stmt;
            DFVarScope childScope = scope.getChildByAST(stmt);
            for (Expression init : (List<Expression>) forStmt.initializers()) {
                this.buildExpr(finder, method, childScope, init);
            }
            DFFrame childFrame = this.addChild(DFFrame.BREAKABLE, stmt);
            Expression expr = forStmt.getExpression();
            if (expr != null) {
                childFrame.buildExpr(finder, method, childScope, expr);
            }
            childFrame.buildStmt(finder, method, childScope, forStmt.getBody());
            for (Expression update : (List<Expression>) forStmt.updaters()) {
                childFrame.buildExpr(finder, method, childScope, update);
            }
            childFrame.removeRefs(childScope);
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof EnhancedForStatement) {
            EnhancedForStatement eForStmt = (EnhancedForStatement)stmt;
            this.buildExpr(finder, method, scope, eForStmt.getExpression());
            DFVarScope childScope = scope.getChildByAST(stmt);
            DFFrame childFrame = this.addChild(DFFrame.BREAKABLE, stmt);
            childFrame.buildStmt(finder, method, childScope, eForStmt.getBody());
            childFrame.removeRefs(childScope);
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof ReturnStatement) {
            ReturnStatement rtrnStmt = (ReturnStatement)stmt;
            Expression expr = rtrnStmt.getExpression();
            if (expr != null) {
                this.buildExpr(finder, method, scope, expr);
            }
            this.addOutputRef(scope.lookupReturn());

        } else if (stmt instanceof BreakStatement) {

        } else if (stmt instanceof ContinueStatement) {

        } else if (stmt instanceof LabeledStatement) {
            LabeledStatement labeledStmt = (LabeledStatement)stmt;
            SimpleName labelName = labeledStmt.getLabel();
            String label = labelName.getIdentifier();
            DFFrame childFrame = this.addChild(label, stmt);
            childFrame.buildStmt(finder, method, scope, labeledStmt.getBody());
            this.expandLocalRefs(childFrame);

        } else if (stmt instanceof SynchronizedStatement) {
            SynchronizedStatement syncStmt = (SynchronizedStatement)stmt;
            this.buildExpr(finder, method, scope, syncStmt.getExpression());
            this.buildStmt(finder, method, scope, syncStmt.getBody());

        } else if (stmt instanceof TryStatement) {
            TryStatement tryStmt = (TryStatement)stmt;
            DFVarScope tryScope = scope.getChildByAST(stmt);
            DFFrame tryFrame = this.addChild(DFFrame.CATCHABLE, stmt);
            tryFrame.buildStmt(finder, method, tryScope, tryStmt.getBody());
            tryFrame.removeRefs(tryScope);
            this.expandLocalRefs(tryFrame);
            for (CatchClause cc :
                     (List<CatchClause>) tryStmt.catchClauses()) {
                DFVarScope catchScope = scope.getChildByAST(cc);
                DFFrame catchFrame = this.addChild(DFFrame.ANONYMOUS, cc);
                catchFrame.buildStmt(finder, method, catchScope, cc.getBody());
                catchFrame.removeRefs(catchScope);
                this.expandLocalRefs(catchFrame);
            }
            Block finBlock = tryStmt.getFinally();
            if (finBlock != null) {
                this.buildStmt(finder, method, scope, finBlock);
            }

        } else if (stmt instanceof ThrowStatement) {
            ThrowStatement throwStmt = (ThrowStatement)stmt;
            this.buildExpr(finder, method, scope, throwStmt.getExpression());
            this.addOutputRef(scope.lookupException());

        } else if (stmt instanceof ConstructorInvocation) {
            ConstructorInvocation ci = (ConstructorInvocation)stmt;
            for (Expression arg : (List<Expression>) ci.arguments()) {
                this.buildExpr(finder, method, scope, arg);
            }

        } else if (stmt instanceof SuperConstructorInvocation) {
            SuperConstructorInvocation sci = (SuperConstructorInvocation)stmt;
            for (Expression arg : (List<Expression>) sci.arguments()) {
                this.buildExpr(finder, method, scope, arg);
            }

        } else if (stmt instanceof TypeDeclarationStatement) {

        } else {
            throw new UnsupportedSyntax(stmt);

        }
    }

    @SuppressWarnings("unchecked")
    private DFType buildExpr(
        DFTypeFinder finder, DFMethod method, DFVarScope scope,
        Expression expr)
        throws UnsupportedSyntax, EntityNotFound {
        assert expr != null;

        try {
	    if (expr instanceof Annotation) {
		return null;

	    } else if (expr instanceof Name) {
		Name name = (Name)expr;
		DFRef ref;
		if (name.isSimpleName()) {
		    ref = scope.lookupVar((SimpleName)name);
		} else {
		    QualifiedName qname = (QualifiedName)name;
		    DFKlass klass;
		    try {
			// Try assuming it's a variable access.
			DFType type = this.buildExpr(
                            finder, method, scope, qname.getQualifier());
			if (type == null) return type;
			klass = finder.resolveKlass(type);
		    } catch (EntityNotFound e) {
			// Turned out it's a class variable.
			klass = finder.lookupKlass(qname.getQualifier());
		    }
                    klass.load();
		    SimpleName fieldName = qname.getName();
		    ref = klass.lookupField(fieldName);
		}
		this.addInputRef(ref);
		return ref.getRefType();

	    } else if (expr instanceof ThisExpression) {
		ThisExpression thisExpr = (ThisExpression)expr;
		Name name = thisExpr.getQualifier();
		DFRef ref;
		if (name != null) {
		    DFKlass klass = finder.lookupKlass(name);
                    klass.load();
		    ref = klass.getKlassScope().lookupThis();
		} else {
		    ref = scope.lookupThis();
		}
		this.addInputRef(ref);
		return ref.getRefType();

	    } else if (expr instanceof BooleanLiteral) {
		return DFBasicType.BOOLEAN;

	    } else if (expr instanceof CharacterLiteral) {
		return DFBasicType.CHAR;

	    } else if (expr instanceof NullLiteral) {
		return DFNullType.NULL;

	    } else if (expr instanceof NumberLiteral) {
		return DFBasicType.INT;

	    } else if (expr instanceof StringLiteral) {
		return DFBuiltinTypes.getStringKlass();

	    } else if (expr instanceof TypeLiteral) {
		return DFBuiltinTypes.getClassKlass();

	    } else if (expr instanceof PrefixExpression) {
		PrefixExpression prefix = (PrefixExpression)expr;
		PrefixExpression.Operator op = prefix.getOperator();
		Expression operand = prefix.getOperand();
		if (op == PrefixExpression.Operator.INCREMENT ||
		    op == PrefixExpression.Operator.DECREMENT) {
		    this.buildAssignment(finder, method, scope, operand);
		}
		return this.buildExpr(finder, method, scope, operand);

	    } else if (expr instanceof PostfixExpression) {
		PostfixExpression postfix = (PostfixExpression)expr;
		PostfixExpression.Operator op = postfix.getOperator();
		Expression operand = postfix.getOperand();
		if (op == PostfixExpression.Operator.INCREMENT ||
		    op == PostfixExpression.Operator.DECREMENT) {
		    this.buildAssignment(finder, method, scope, operand);
		}
		return this.buildExpr(finder, method, scope, operand);

	    } else if (expr instanceof InfixExpression) {
		InfixExpression infix = (InfixExpression)expr;
		InfixExpression.Operator op = infix.getOperator();
		DFType left = this.buildExpr(
                    finder, method, scope, infix.getLeftOperand());
		DFType right = this.buildExpr(
                    finder, method, scope, infix.getRightOperand());
		return DFType.inferInfixType(left, op, right);

	    } else if (expr instanceof ParenthesizedExpression) {
		ParenthesizedExpression paren = (ParenthesizedExpression)expr;
		return this.buildExpr(finder, method, scope, paren.getExpression());

	    } else if (expr instanceof Assignment) {
		Assignment assn = (Assignment)expr;
		Assignment.Operator op = assn.getOperator();
		if (op != Assignment.Operator.ASSIGN) {
		    this.buildExpr(finder, method, scope, assn.getLeftHandSide());
		}
		this.buildAssignment(finder, method, scope, assn.getLeftHandSide());
		return this.buildExpr(finder, method, scope, assn.getRightHandSide());

	    } else if (expr instanceof VariableDeclarationExpression) {
		VariableDeclarationExpression decl =
		    (VariableDeclarationExpression)expr;
		for (VariableDeclarationFragment frag :
			 (List<VariableDeclarationFragment>) decl.fragments()) {
		    DFRef ref = scope.lookupVar(frag.getName());
		    this.addOutputRef(ref);
		    Expression init = frag.getInitializer();
		    if (init != null) {
			this.buildExpr(finder, method, scope, init);
		    }
		}
		return null; // XXX what do?

	    } else if (expr instanceof MethodInvocation) {
		MethodInvocation invoke = (MethodInvocation)expr;
		Expression expr1 = invoke.getExpression();
		DFKlass klass = null;
		if (expr1 == null) {
		    DFRef ref = scope.lookupThis();
		    this.addInputRef(ref);
		    klass = finder.resolveKlass(ref.getRefType());
		} else {
		    if (expr1 instanceof Name) {
			try {
			    klass = finder.lookupKlass((Name)expr1);
			} catch (TypeNotFound e) {
			}
		    }
		    if (klass == null) {
			DFType type = this.buildExpr(finder, method, scope, expr1);
			if (type == null) return type;
			klass = finder.resolveKlass(type);
		    }
		}
                klass.load();
		List<DFType> typeList = new ArrayList<DFType>();
		for (Expression arg : (List<Expression>) invoke.arguments()) {
		    DFType type = this.buildExpr(finder, method, scope, arg);
		    if (type == null) return type;
		    typeList.add(type);
		}
		DFType[] argTypes = new DFType[typeList.size()];
		typeList.toArray(argTypes);
		try {
		    DFMethod method1 = klass.lookupMethod(
			invoke.getName(), argTypes);
		    method1.addCaller(method);
		    return method1.getReturnType();
		} catch (MethodNotFound e) {
		    return null;
		}

	    } else if (expr instanceof SuperMethodInvocation) {
		SuperMethodInvocation sinvoke = (SuperMethodInvocation)expr;
		List<DFType> typeList = new ArrayList<DFType>();
		for (Expression arg : (List<Expression>) sinvoke.arguments()) {
		    DFType type = this.buildExpr(finder, method, scope, arg);
		    if (type == null) return type;
		    typeList.add(type);
		}
		DFType[] argTypes = new DFType[typeList.size()];
		typeList.toArray(argTypes);
		DFKlass klass = finder.resolveKlass(
                    scope.lookupThis().getRefType());
                klass.load();
		DFKlass baseKlass = klass.getBaseKlass();
                baseKlass.load();
		try {
		    DFMethod method1 = baseKlass.lookupMethod(
			sinvoke.getName(), argTypes);
		    method1.addCaller(method);
		    return method1.getReturnType();
		} catch (MethodNotFound e) {
		    return null;
		}

	    } else if (expr instanceof ArrayCreation) {
		ArrayCreation ac = (ArrayCreation)expr;
		for (Expression dim : (List<Expression>) ac.dimensions()) {
		    this.buildExpr(finder, method, scope, dim);
		}
		ArrayInitializer init = ac.getInitializer();
		if (init != null) {
		    this.buildExpr(finder, method, scope, init);
		}
		return finder.resolve(ac.getType().getElementType());

	    } else if (expr instanceof ArrayInitializer) {
		ArrayInitializer init = (ArrayInitializer)expr;
		DFType type = null;
		for (Expression expr1 : (List<Expression>) init.expressions()) {
		    type = this.buildExpr(finder, method, scope, expr1);
		}
		return type;

	    } else if (expr instanceof ArrayAccess) {
		ArrayAccess aa = (ArrayAccess)expr;
		this.buildExpr(finder, method, scope, aa.getIndex());
		DFType type = this.buildExpr(finder, method, scope, aa.getArray());
		if (type instanceof DFArrayType) {
		    DFRef ref = scope.lookupArray(type);
		    this.addInputRef(ref);
		    type = ((DFArrayType)type).getElemType();
		}
		return type;

	    } else if (expr instanceof FieldAccess) {
		FieldAccess fa = (FieldAccess)expr;
		Expression expr1 = fa.getExpression();
		DFKlass klass = null;
		if (expr1 instanceof Name) {
		    try {
			klass = finder.lookupKlass((Name)expr1);
		    } catch (TypeNotFound e) {
		    }
		}
		if (klass == null) {
		    DFType type = this.buildExpr(finder, method, scope, expr1);
		    if (type == null) return type;
		    klass = finder.resolveKlass(type);
		}
                klass.load();
		SimpleName fieldName = fa.getName();
		DFRef ref = klass.lookupField(fieldName);
		this.addInputRef(ref);
		return ref.getRefType();

	    } else if (expr instanceof SuperFieldAccess) {
		SuperFieldAccess sfa = (SuperFieldAccess)expr;
		SimpleName fieldName = sfa.getName();
		DFKlass klass = finder.resolveKlass(
		    scope.lookupThis().getRefType()).getBaseKlass();
                klass.load();
		DFRef ref = klass.lookupField(fieldName);
		this.addInputRef(ref);
		return ref.getRefType();

	    } else if (expr instanceof CastExpression) {
		CastExpression cast = (CastExpression)expr;
		this.buildExpr(finder, method, scope, cast.getExpression());
		return finder.resolve(cast.getType());

	    } else if (expr instanceof ClassInstanceCreation) {
		ClassInstanceCreation cstr = (ClassInstanceCreation)expr;
		AnonymousClassDeclaration anonDecl =
		    cstr.getAnonymousClassDeclaration();
		DFType instType;
		if (anonDecl != null) {
		    String id = "anon"+Utils.encodeASTNode(anonDecl);
		    DFTypeSpace methodSpace = method.getMethodSpace();
		    instType = methodSpace.getKlass(id);
		} else {
		    instType = finder.resolve(cstr.getType());
		}
		Expression expr1 = cstr.getExpression();
		if (expr1 != null) {
		    this.buildExpr(finder, method, scope, expr1);
		}
		for (Expression arg : (List<Expression>) cstr.arguments()) {
		    this.buildExpr(finder, method, scope, arg);
		}
		return instType;

	    } else if (expr instanceof ConditionalExpression) {
		ConditionalExpression cond = (ConditionalExpression)expr;
		this.buildExpr(finder, method, scope, cond.getExpression());
		this.buildExpr(finder, method, scope, cond.getThenExpression());
		return this.buildExpr(finder, method, scope, cond.getElseExpression());

	    } else if (expr instanceof InstanceofExpression) {
		return DFBasicType.BOOLEAN;

	    } else if (expr instanceof LambdaExpression) {
		LambdaExpression lambda = (LambdaExpression)expr;
		String id = "lambda";
		ASTNode body = lambda.getBody();
		DFTypeSpace anonSpace = new DFTypeSpace(null, id);
		DFKlass klass = finder.resolveKlass(
		    scope.lookupThis().getRefType());
		DFKlass anonKlass = new DFKlass(id, anonSpace, klass, scope);
		if (body instanceof Statement) {
		    // XXX TODO Statement lambda
		} else if (body instanceof Expression) {
		    // XXX TODO Expresssion lambda
		} else {
		    throw new UnsupportedSyntax(body);
		}
		return anonKlass;

	    } else if (expr instanceof MethodReference) {
		// MethodReference
		//  CreationReference
		//  ExpressionMethodReference
		//  SuperMethodReference
		//  TypeMethodReference
		throw new UnsupportedSyntax(expr);

	    } else {
		// ???
		throw new UnsupportedSyntax(expr);
	    }
        } catch (EntityNotFound e) {
            e.setAst(expr);
            throw e;
        }
    }

    private void buildAssignment(
        DFTypeFinder finder, DFMethod method, DFVarScope scope,
        Expression expr)
        throws UnsupportedSyntax, EntityNotFound {
        assert expr != null;

        if (expr instanceof Name) {
            Name name = (Name)expr;
            DFRef ref;
            if (name.isSimpleName()) {
                ref = scope.lookupVar((SimpleName)name);
            } else {
                QualifiedName qname = (QualifiedName)name;
                DFKlass klass;
                try {
                    // Try assuming it's a variable access.
                    DFType type = this.buildExpr(
                        finder, method, scope, qname.getQualifier());
                    if (type == null) return;
                    klass = finder.resolveKlass(type);
                } catch (EntityNotFound e) {
                    // Turned out it's a class variable.
                    klass = finder.lookupKlass(qname.getQualifier());
                }
                klass.load();
                SimpleName fieldName = qname.getName();
                ref = klass.lookupField(fieldName);
            }
            this.addOutputRef(ref);

        } else if (expr instanceof ArrayAccess) {
            ArrayAccess aa = (ArrayAccess)expr;
            DFType type = this.buildExpr(finder, method, scope, aa.getArray());
            this.buildExpr(finder, method, scope, aa.getIndex());
            if (type instanceof DFArrayType) {
                DFRef ref = scope.lookupArray(type);
                this.addOutputRef(ref);
            }

        } else if (expr instanceof FieldAccess) {
            FieldAccess fa = (FieldAccess)expr;
            Expression expr1 = fa.getExpression();
            DFKlass klass = null;
            if (expr1 instanceof Name) {
                try {
                    klass = finder.lookupKlass((Name)expr1);
                } catch (TypeNotFound e) {
                }
            }
            if (klass == null) {
                DFType type = this.buildExpr(finder, method, scope, expr1);
                if (type == null) return;
                klass = finder.resolveKlass(type);
            }
            klass.load();
            SimpleName fieldName = fa.getName();
            DFRef ref = klass.lookupField(fieldName);
            this.addOutputRef(ref);

        } else if (expr instanceof SuperFieldAccess) {
            SuperFieldAccess sfa = (SuperFieldAccess)expr;
            SimpleName fieldName = sfa.getName();
            DFKlass klass = finder.resolveKlass(
                scope.lookupThis().getRefType()).getBaseKlass();
            klass.load();
            DFRef ref = klass.lookupField(fieldName);
            this.addOutputRef(ref);

        } else {
            throw new UnsupportedSyntax(expr);
        }
    }

    // dump: for debugging.
    public void dump() {
        dump(System.err, "");
    }
    public void dump(PrintStream out, String indent) {
        out.println(indent+_label+" {");
        String i2 = indent + "  ";
        StringBuilder inputs = new StringBuilder();
        for (DFRef ref : this.getInputRefs()) {
            inputs.append(" "+ref);
        }
        out.println(i2+"inputs:"+inputs);
        StringBuilder outputs = new StringBuilder();
        for (DFRef ref : this.getOutputRefs()) {
            outputs.append(" "+ref);
        }
        out.println(i2+"outputs:"+outputs);
        for (DFFrame frame : _ast2child.values()) {
            frame.dump(out, i2);
        }
        out.println(indent+"}");
    }
}
