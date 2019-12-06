package com.syb.coffeelint.detectors;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Speed;
import com.syb.coffeelint.CIssueRegister;
import com.syb.coffeelint.type.Type;

import java.lang.reflect.Modifier;
import java.util.List;

import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.MethodDeclaration;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;

public class AnonymousClassDetector extends Detector implements Detector.JavaScanner {

    private static final String LOOPER_CLS = "android.os.Looper";

    public AnonymousClassDetector() {
    }

    private CIssueRegister register;

    @Override
    public void beforeCheckProject(Context context) {
        super.beforeCheckProject(context);
        register = CIssueRegister.getInstance();
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Type.getClassKeys();
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration declaration,
                           @NonNull Node node, @NonNull JavaParser.ResolvedClass cls) {
        if (!isInnerClass(declaration)) {
            return;
        }

        if (isStaticClass(declaration)) {
            return;
        }

        // Only flag handlers using the default looper
        ConstructorInvocation invocation = null;
        Node current = node;
        while (current != null) {
            if (current instanceof ConstructorInvocation) {
                invocation = (ConstructorInvocation) current;
                break;
            } else if (current instanceof MethodDeclaration ||
                    current instanceof ClassDeclaration) {
                break;
            }
            current = current.getParent();
        }

        if (invocation != null) {
            for (Expression expression : invocation.astArguments()) {
                JavaParser.TypeDescriptor type = context.getType(expression);
                if (type != null && type.matchesName(LOOPER_CLS)) {
                    return;
                }
            }
        } else if (hasLooperConstructorParameter(cls)) {
            // This is an inner class which takes a Looper parameter:
            // possibly used correctly from elsewhere
            return;
        }

        Type tEnum = Type.getEnumByKey(cls.getName());

        Location location;
        Node locationNode;
        if (node instanceof ClassDeclaration) {
            locationNode = node;
            location = context.getLocation(((ClassDeclaration) node).astName());
        } else if (node instanceof NormalTypeBody
                && node.getParent() instanceof ConstructorInvocation) {
            ConstructorInvocation parent = (ConstructorInvocation)node.getParent();
            locationNode = parent;
            location = context.getRangeLocation(parent, 0, parent.astTypeReference(), 0);
        } else {
            locationNode = node;
            location = context.getLocation(node);
        }

        if (tEnum != null && register.isIssueId(tEnum.issue.getId())) {
            context.report(tEnum.issue, locationNode, location, String.format(
                    tEnum.desc, cls.getName()));
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

    private static boolean hasLooperConstructorParameter(@NonNull JavaParser.ResolvedClass cls) {
        for (JavaParser.ResolvedMethod constructor : cls.getConstructors()) {
            for (int i = 0, n = constructor.getArgumentCount(); i < n; i++) {
                JavaParser.TypeDescriptor type = constructor.getArgumentType(i);
                if (type.matchesSignature(LOOPER_CLS)) {
                    return true;
                }
            }
        }
        return false;
    }

}
