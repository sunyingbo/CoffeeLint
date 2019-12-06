package com.syb.coffeelint.detectors;

import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.syb.coffeelint.CIssueRegister;
import com.syb.coffeelint.type.Type;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;
import lombok.ast.Try;
import lombok.ast.TypeReference;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableReference;

import static com.android.SdkConstants.SUPPORT_LIB_ARTIFACT;

public class CComDetector extends Detector implements Detector.JavaScanner {

    private static final String INTEGER = "Integer";
    private static final String BOOLEAN = "Boolean";
    private static final String BYTE = "Byte";
    private static final String LONG = "Long";

    private CIssueRegister register;

    @Override
    public void beforeCheckProject(Context context) {
        super.beforeCheckProject(context);
        register = CIssueRegister.getInstance();
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Type.getMethodKeys();
    }

    @Override
    public List<String> getApplicableConstructorTypes() {
        return Type.getConstructorKeys();
    }

    @Override
    public void visitConstructor(JavaContext context, AstVisitor visitor, ConstructorInvocation node, JavaParser.ResolvedMethod constructor) {
        super.visitConstructor(context, visitor, node, constructor);
        JavaParser.ResolvedNode resolvedType = context.resolve(node.astTypeReference());
        JavaParser.ResolvedClass resolvedClass = (JavaParser.ResolvedClass) resolvedType;
        String name = resolvedClass.getName();
        Type tEnum = Type.getEnumByKey(name);
        if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
            TypeReference reference = node.astTypeReference();
            if (name.equals(Type.valueOf("MAP").keys.get(0))) {
                checkHashMap(context, tEnum, node, reference);
            } else if (name.equals(Type.valueOf("MAP").keys.get(1))) {
                checkSparseArray(context, tEnum, node, reference);
            } else {
                context.report(tEnum.issue, node, context.getLocation(node), tEnum.desc);
            }
        }
    }

    @Override
    public void visitMethod(JavaContext context, AstVisitor visitor, MethodInvocation node) {
        super.visitMethod(context, visitor, node);
        String methodName = node.astName().astValue();
        Type tEnum = Type.getEnumByKey(methodName);
        if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
            if (!methodName.equals(Type.valueOf("SERVICE").keys.get(0))
                    || (methodName.equals(Type.valueOf("SERVICE").keys.get(0))
                    && !hasTryCatch(node))) {
                context.report(tEnum.issue, node, context.getLocation(node), tEnum.desc);
            }
        }
    }

    private boolean hasTryCatch(lombok.ast.Node scope) {
        while (scope != null) {
            Class<? extends lombok.ast.Node> type = scope.getClass();
            if (type == MethodDeclaration.class) {
                return false;
            } else if (type == Try.class) {
                return true;
            }
            scope = scope.getParent();
        }
        return false;
    }

    private void checkHashMap(JavaContext context, Type type, ConstructorInvocation node, TypeReference reference) {
        // reference.hasTypeArguments returns false where it should not
        Node definition = node.getParent().getParent();
        if (definition instanceof VariableDefinition) {
            TypeReference typeReference = ((VariableDefinition) definition).astTypeReference();
            StrictListAccessor<TypeReference, TypeReference> types = typeReference.getTypeArguments();
            if (types != null && types.size() == 2) {
                TypeReference first = types.first();
                String firstTypeName = first.getTypeName();
                String lastTypeName = types.last().getTypeName();
                checkCore(context, type, node, firstTypeName, lastTypeName);
            }
        }
    }

    private void checkCore(JavaContext context, Type type, ConstructorInvocation node, String typeName, String valueType) {
        int minSdk = context.getMainProject().getMinSdk();
        if (typeName.equals(INTEGER) || typeName.equals(BYTE)) {
            if (valueType.equals(INTEGER)) {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseIntArray()",
                                "new HashMap<Integer, Integer>"));
            } else if (valueType.equals(LONG) && minSdk >= 18) {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseLongArray()",
                                "new HashMap<Integer, Long>"));
            } else if (valueType.equals(BOOLEAN)) {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseBooleanArray()",
                                "new HashMap<Integer, Boolean>"));
            } else {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseArray<E>()",
                                "new HashMap<Integer, E>"));
            }
        } else if (typeName.equals(LONG) && (minSdk >= 16 ||
                Boolean.TRUE == context.getMainProject().dependsOn(
                        SUPPORT_LIB_ARTIFACT))) {
            boolean useBuiltin = minSdk >= 16;
            String message = useBuiltin ?
                    String.format(type.desc, "new LongSparseArray()",
                            "new HashMap<Long, E>") :
                    String.format(type.desc, "new android.support.v4.util.LongSparseArray()",
                            "new HashMap<Long, E>");
            context.report(type.issue, node, context.getLocation(node),
                    message);
        }
    }

    private void checkSparseArray(JavaContext context, Type type, ConstructorInvocation node, TypeReference reference) {
        // reference.hasTypeArguments returns false where it should not
        StrictListAccessor<TypeReference, TypeReference> types = reference.getTypeArguments();
        if (types != null && types.size() == 1) {
            TypeReference first = types.first();
            String valueType = first.getTypeName();
            if (valueType.equals(INTEGER)) {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseIntArray()",
                                "new SparseArray<Integer>()"));
            } else if (valueType.equals(BOOLEAN)) {
                context.report(type.issue, node, context.getLocation(node),
                        String.format(type.desc, "new SparseBooleanArray()",
                                "new SparseArray<Boolean>()"));
            }
        }
    }

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Arrays.asList(
                Select.class,
                VariableDefinition.class,
                VariableReference.class,
                ClassDeclaration.class
        );
    }

    @Override
    public AstVisitor createJavaVisitor(JavaContext context) {
        return new CCJavaVisitor(context);
    }

    private final class CCJavaVisitor extends ForwardingAstVisitor {

        private JavaContext mContext;

        private CCJavaVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitSelect(Select node) {
            String key = node.toString();
            boolean report = false;
            if (key.equals(Type.valueOf("LOG").keys.get(1))) {
                report = true;
            } else {
                Expression operand = node.astOperand();
                if (operand != null) {
                    key = Type.valueOf("STATICINFO").keys.get(0);
                    if (operand.toString().contains(key)) {
                        report = true;
                    }
                }
            }
            if (report) {
                Type tEnum = Type.getEnumByKey(key);
                if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
                    mContext.report(tEnum.issue, node, mContext.getLocation(node),
                            tEnum.desc);
                    return true;
                }
            }
            return super.visitSelect(node);
        }

        @Override
        public boolean visitVariableReference(VariableReference node) {
            JavaParser.ResolvedNode resolve = mContext.resolve(node);
            if (resolve instanceof JavaParser.ResolvedClass) {
                JavaParser.ResolvedClass clazz = (JavaParser.ResolvedClass) resolve;
                String key = Type.valueOf("LOG").keys.get(0);
                Type tEnum = Type.getEnumByKey(key);
                if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
                    if (clazz.getName().equals(key)) {
                        mContext.report(tEnum.issue, node, mContext.getLocation(node),
                                tEnum.desc);
                        return true;
                    }
                }
            }
            return super.visitVariableReference(node);
        }

        private List<String> constantsList = new ArrayList<>();

        {
            for (int i = 2; i < Type.valueOf("UNFINAL").keys.size(); i++) {
                constantsList.add(Type.valueOf("UNFINAL").keys.get(i));
            }
        }

        @Override
        public boolean visitVariableDefinition(VariableDefinition node) {
            String name = node.toString();
            String key = Type.valueOf("UNFINAL").keys.get(0);
            if (name.contains(key)) {
                name = name.substring(name.indexOf(key), name.length());
                Type tEnum = Type.getEnumByKey(key);
                if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
                    if (constantsList.contains(name)) {
                        String modifiers = node.astModifiers().toString();
                        if (!"".equals(modifiers) && modifiers.contains(Type.valueOf("UNFINAL").keys.get(1))) {
                            mContext.report(tEnum.issue, node, mContext.getLocation(node),
                                    String.format(tEnum.desc, name));
                            return true;
                        }
                    }
                }
            }
            return super.visitVariableDefinition(node);
        }

        @Override
        public boolean visitClassDeclaration(ClassDeclaration node) {
            if (!isInnerClass(node)) {
                return false;
            }

            if (isStaticClass(node)) {
                return false;
            }

            StrictListAccessor<TypeReference, ClassDeclaration> typeReferences = node.astImplementing();
            if (typeReferences != null) {
                for (TypeReference typeReference : typeReferences) {
                    String typeName = typeReference.getTypeName();
                    if (typeName.toLowerCase().contains("clicklistener")
                            || typeName.toLowerCase().contains("serializable")) {
                        return false;
                    }
                }
            }

            TypeReference typeReference = node.astExtending();
            if (typeReference != null) {
                String typeName = typeReference.getTypeName();
                if (typeName.toLowerCase().equals("baseadapter")) {
                    return false;
                }
            }

            Location location = mContext.getLocation(node.astName());

            mContext.report(Type.valueOf("INNERCLASS").issue, node, location, Type.valueOf("INNERCLASS").desc);

            return super.visitClassDeclaration(node);
        }

    }

    private static boolean isInnerClass(@Nullable ClassDeclaration node) {
        return node == null || // null class declarations means anonymous inner class
                JavaContext.getParentOfType(node, ClassDeclaration.class, true) != null;
    }

    private static boolean isStaticClass(@Nullable ClassDeclaration node) {
        if (node == null) {
            // A null class declaration means anonymous inner class, and these can't be static
            return false;
        }

        int flags = node.astModifiers().getEffectiveModifierFlags();
        return (flags & Modifier.STATIC) != 0;
    }

}
