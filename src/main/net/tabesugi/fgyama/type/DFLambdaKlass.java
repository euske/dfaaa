//  Java2DF
//
package net.tabesugi.fgyama;
import java.io.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


//  DFLambdaKlass
//
class DFLambdaKlass extends DFSourceKlass {

    private class FunctionalMethod extends DFMethod {

        public FunctionalMethod(String id) {
            super(DFLambdaKlass.this, CallStyle.Lambda, false,
                  id, id, DFLambdaKlass.this._lambdaScope);
        }

        @Override
        public boolean isTransparent() {
            return true;
        }
    }

    private class LambdaScope extends DFVarScope {

        private Map<String, CapturedRef> _id2captured =
            new HashMap<String, CapturedRef>();

        public LambdaScope(DFVarScope outer, String id) {
            super(outer, id);
        }

        @Override
        public DFRef lookupVar(String id)
            throws VariableNotFound {
            DFRef ref = _id2captured.get(id);
            if (ref != null) return ref;
            ref = super.lookupVar(id);
            if (ref != null) {
                // replace ref with a captured variable.
                CapturedRef captured = new CapturedRef(ref, id);
                DFLambdaKlass.this.addCapturedRef(captured);
                _id2captured.put(id, captured);
                ref = captured;
            }
            return ref;
        }
    }

    public class CapturedRef extends DFRef {

        private DFRef _original;
        private String _name;

        public CapturedRef(DFRef original, String name) {
            super(original.getRefType());
            _original = original;
            _name = name;
        }

        public DFRef getOriginal() {
            return _original;
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public String getFullName() {
            return "@"+DFLambdaKlass.this.getTypeName()+"/="+_name;
        }
    }

    private final String FUNC_NAME = "#f";

    private LambdaScope _lambdaScope;
    private FunctionalMethod _funcMethod;

    private List<CapturedRef> _captured =
        new ArrayList<CapturedRef>();

    public DFLambdaKlass(
        String filePath, LambdaExpression lambda,
        DFTypeSpace outerSpace, DFVarScope outerScope, DFSourceKlass outerKlass)
        throws InvalidSyntax {
        super(filePath, lambda, Utils.encodeASTNode(lambda),
              outerSpace, outerScope, outerKlass);
        _lambdaScope = new LambdaScope(outerScope, Utils.encodeASTNode(lambda));
        _funcMethod = new FunctionalMethod(FUNC_NAME);
        ASTNode body = lambda.getBody();
        if (body instanceof Statement) {
            this.buildTypeFromStmt((Statement)body, _funcMethod, _funcMethod.getScope());
        } else if (body instanceof Expression) {
            this.buildTypeFromExpr((Expression)body, _funcMethod, _funcMethod.getScope());
        } else {
            throw new InvalidSyntax(body);
        }
        assert _funcMethod != null;
    }

    @Override
    public String toString() {
        return ("<DFLambdaKlass("+this.getTypeName()+")>");
    }

    @Override
    public int isSubclassOf(DFKlass klass, Map<DFMapType, DFKlass> typeMap) {
        if (klass instanceof DFSourceKlass &&
            ((DFSourceKlass)klass).isFuncInterface()) return 0;
        return -1;
    }

    @Override
    public boolean isDefined() {
        return (_funcMethod != null && _funcMethod.getFuncType() != null);
    }

    @Override
    public DFMethod getFuncMethod() {
        return _funcMethod;
    }

    @Override
    public void setBaseKlass(DFKlass klass) {
        super.setBaseKlass(klass);
        assert _funcMethod != null;
        assert _funcMethod.getFuncType() == null;
        DFMethod funcMethod = klass.getFuncMethod();
        // BaseKlass does not have a function method.
        // This happens when baseKlass type is undefined.
        if (funcMethod == null) return;
        _funcMethod.setFuncType(funcMethod.getFuncType());
    }

    @Override
    protected void buildMembersFromAST(DFTypeFinder finder, ASTNode ast)
        throws InvalidSyntax {
        // _baseKlass is left undefined.
        assert _funcMethod != null;
        this.addMethod(_funcMethod, FUNC_NAME);
        _funcMethod.setBaseFinder(finder);
        _funcMethod.setTree(ast);
    }

    private void addCapturedRef(CapturedRef ref) {
        _captured.add(ref);
    }

    public List<CapturedRef> getCapturedRefs() {
        return _captured;
    }
}
