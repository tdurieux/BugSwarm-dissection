////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.coding;

import java.util.Set;

import com.google.common.collect.Sets;
import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Checks that any combination of String literals with optional
 * assignment is on the left side of an equals() comparison.
 * </p>
 *
 * <p>
 * Rationale: Calling the equals() method on String literals
 * will avoid a potential NullPointerException.  Also, it is
 * pretty common to see null check right before equals comparisons
 * which is not necessary in the below example.
 *
 * For example:
 *
 * <pre>
 *  {@code
 *    String nullString = null;
 *    nullString.equals(&quot;My_Sweet_String&quot;);
 *  }
 * </pre>
 * should be refactored to
 *
 * <pre>
 *  {@code
 *    String nullString = null;
 *    &quot;My_Sweet_String&quot;.equals(nullString);
 *  }
 * </pre>
 *
 *
 * <p>
 * Limitations: If the equals method is overridden or
 * a covariant equals method is defined and the implementation
 * is incorrect (where s.equals(t) does not return the same result
 * as t.equals(s)) then rearranging the called on object and
 * parameter may have unexpected results
 *
 * <br>
 *
 * Java's Autoboxing feature has an affect
 * on how this check is implemented. Pre Java 5 all IDENT + IDENT
 * object concatenations would not cause a NullPointerException even
 * if null.  Those situations could have been included in this check.
 * They would simply act as if they surrounded by String.valueOf()
 * which would concatenate the String null.
 *
 * <p>
 * The following example will cause a
 * NullPointerException as a result of what autoboxing does.
 * <pre>
 * Integer i = null, j = null;
 * String number = "5"
 * number.equals(i + j);
 * </pre>
 *
 *
 * Since, it is difficult to determine what kind of Object is being
 * concatenated all ident concatenation is considered unsafe.
 *
 * @author Travis Schneeberger
 * version 1.0
 */
public class EqualsAvoidNullCheck extends Check {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_EQUALS_AVOID_NULL = "equals.avoid.null";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_EQUALS_IGNORE_CASE_AVOID_NULL = "equalsIgnoreCase.avoid.null";

    /** Whether to process equalsIgnoreCase() invocations. */
    private boolean ignoreEqualsIgnoreCase;

    /** stack of sets of field names, one for each class of a set of nested classes. */
    private FieldFrame currentFrame;

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.METHOD_CALL,
            TokenTypes.CLASS_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_TRY,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.PARAMETER_DEF,
            TokenTypes.CTOR_DEF,
            TokenTypes.SLIST,
            TokenTypes.ENUM_DEF,
            TokenTypes.ENUM_CONSTANT_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    /**
     * Whether to ignore checking {@code String.equalsIgnoreCase(String)}.
     * @param newValue whether to ignore checking
     *    {@code String.equalsIgnoreCase(String)}.
     */
    public void setIgnoreEqualsIgnoreCase(boolean newValue) {
        ignoreEqualsIgnoreCase = newValue;
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        currentFrame = new FieldFrame(null);
    }

    @Override
    public void visitToken(final DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.VARIABLE_DEF:
            case TokenTypes.PARAMETER_DEF:
                currentFrame.addField(ast);
                break;
            case TokenTypes.METHOD_CALL:
                processMethodCall(ast);
                break;
            case TokenTypes.SLIST:
                processSlist(ast);
                break;
            default:
                processFrame(ast);
        }
    }

    @Override
    public void leaveToken(DetailAST ast) {
        if (ast.getType() != TokenTypes.VARIABLE_DEF
                && ast.getType() != TokenTypes.PARAMETER_DEF
                && ast.getType() != TokenTypes.METHOD_CALL
                && ast.getType() != TokenTypes.SLIST) {
            currentFrame = currentFrame.getParent();
        }
        else if (ast.getType() == TokenTypes.SLIST) {
            final int parentType = ast.getParent().getType();
            if (parentType == TokenTypes.SLIST || parentType == TokenTypes.STATIC_INIT) {
                currentFrame = currentFrame.getParent();
            }
        }
    }

    @Override
    public void finishTree(DetailAST ast) {
        traverseFieldFrameTree(currentFrame);
    }
    /**
     * Determine whether SLIST begins static or non-static block and add it as
     * a frame in this case.
     * @param ast SLIST ast.
     */
    private void processSlist(DetailAST ast) {
        final int parentType = ast.getParent().getType();
        if (parentType == TokenTypes.SLIST || parentType == TokenTypes.STATIC_INIT) {
            final FieldFrame frame = new FieldFrame(currentFrame);
            currentFrame.addChild(frame);
            currentFrame = frame;
        }
    }
    /**
     * Process CLASS_DEF, METHOD_DEF, LITERAL_IF, LITERAL_FOR, LITERAL_WHILE, LITERAL_DO,
     * LITERAL_CATCH, LITERAL_TRY, CTOR_DEF, ENUM_DEF, ENUM_CONSTANT_DEF.
     * @param ast processed ast.
     */
    private void processFrame(DetailAST ast) {
        final FieldFrame frame = new FieldFrame(currentFrame);
        final int astType = ast.getType();
        if (astType == TokenTypes.CLASS_DEF || astType == TokenTypes.ENUM_DEF
                || astType == TokenTypes.ENUM_CONSTANT_DEF) {
            frame.setClassOrEnumOrEnumConstDef(true);
            frame.setFrameName(ast.findFirstToken(TokenTypes.IDENT).getText());
        }
        currentFrame.addChild(frame);
        currentFrame = frame;
    }
    /**
     * Add the method call to the current frame if it should be processed.
     * @param methodCall METHOD_CALL ast.
     */
    private void processMethodCall(DetailAST methodCall) {
        final DetailAST dot = methodCall.getFirstChild();
        if (dot.getType() == TokenTypes.DOT) {
            final String methodName = dot.getLastChild().getText();
            if ("equals".equals(methodName)
                    || !ignoreEqualsIgnoreCase && "equalsIgnoreCase".equals(methodName)) {
                currentFrame.addMethodCall(methodCall);
            }
        }
    }

    /**
     * Traverse the tree of the field frames to check all equals method calls.
     * @param frame to check method calls in.
     */
    private void traverseFieldFrameTree(FieldFrame frame) {
        for (FieldFrame child: frame.getChildren()) {
            if (!child.getChildren().isEmpty()) {
                traverseFieldFrameTree(child);
            }
            for (DetailAST methodCall: child.getMethodCalls()) {
                checkMethodCall(methodCall, child);
            }
        }
    }

    /**
     * Check whether the method call should be violated.
     * @param methodCall method call to check.
     * @param methodsFrame the frame of the method call.
     */
    private void checkMethodCall(DetailAST methodCall, FieldFrame methodsFrame) {
        DetailAST objCalledOn = methodCall.getFirstChild().getFirstChild();
        if (objCalledOn.getType() == TokenTypes.DOT) {
            objCalledOn = objCalledOn.getLastChild();
            if (objCalledOn.getPreviousSibling().getType() == TokenTypes.DOT) {
                return;
            }
        }
        if (isStringLiteral(objCalledOn)) {
            return;
        }

        final DetailAST expr = methodCall.findFirstToken(TokenTypes.ELIST).getFirstChild();
        if (containsOneArgument(methodCall) && containsAllSafeTokens(expr)
                && calledOnStringField(objCalledOn, methodsFrame)) {
            final String methodName = methodCall.getFirstChild().getLastChild().getText();
            if ("equals".equals(methodName)) {
                log(methodCall.getLineNo(), methodCall.getColumnNo(),
                    MSG_EQUALS_AVOID_NULL);
            }
            else {
                log(methodCall.getLineNo(), methodCall.getColumnNo(),
                    MSG_EQUALS_IGNORE_CASE_AVOID_NULL);
            }
        }
    }

    /**
     * checks for calling equals on String literal and
     * anon object which cannot be null.
     * @param objCalledOn object AST
     * @return if it is string literal
     */
    private static boolean isStringLiteral(DetailAST objCalledOn) {
        return objCalledOn.getType() == TokenTypes.STRING_LITERAL
                || objCalledOn.getType() == TokenTypes.LITERAL_NEW;
    }

    /**
     * Verify that method call has one argument.
     *
     * @param methodCall METHOD_CALL DetailAST
     * @return true if method call has one argument.
     */
    private static boolean containsOneArgument(DetailAST methodCall) {
        final DetailAST elist = methodCall.findFirstToken(TokenTypes.ELIST);
        return elist.getChildCount() == 1;
    }

    /**
     * <p>
     * Looks for all "safe" Token combinations in the argument
     * expression branch.
     * </p>
     *
     * <p>
     * See class documentation for details on autoboxing's affect
     * on this method implementation.
     * </p>
     *
     * @param expr the argument expression
     * @return - true if any child matches the set of tokens, false if not
     */
    private static boolean containsAllSafeTokens(final DetailAST expr) {
        DetailAST arg = expr.getFirstChild();
        if (arg.branchContains(TokenTypes.METHOD_CALL)) {
            return false;
        }
        arg = skipVariableAssign(arg);

        //Plus assignment can have ill affects
        //do not want to recommend moving expression
        //See example:
        //String s = "SweetString";
        //s.equals(s += "SweetString"); //false
        //s = "SweetString";
        //(s += "SweetString").equals(s); //true
        //arg = skipVariablePlusAssign(arg);

        //must be just String literals to return true
        return !arg.branchContains(TokenTypes.PLUS_ASSIGN)
                && !arg.branchContains(TokenTypes.IDENT)
                && !arg.branchContains(TokenTypes.LITERAL_NULL);
    }

    /**
     * Skips over an inner assign portion of an argument expression.
     * @param currentAST current token in the argument expression
     * @return the next relevant token
     */
    private static DetailAST skipVariableAssign(final DetailAST currentAST) {
        if (currentAST.getType() == TokenTypes.ASSIGN
                && currentAST.getFirstChild().getType() == TokenTypes.IDENT) {
            return currentAST.getFirstChild().getNextSibling();
        }
        return currentAST;
    }

    /**
     * Determine, whether equals method is called on a field of String type.
     * @param objCalledOn object ast.
     * @param methodsFrame frame of the object.
     * @return true if the object is of String type.
     */
    private static boolean calledOnStringField(DetailAST objCalledOn, FieldFrame methodsFrame) {
        boolean result = false;
        final DetailAST previousSiblingAst = objCalledOn.getPreviousSibling();
        final String name = objCalledOn.getText();
        if (previousSiblingAst != null) {
            if (previousSiblingAst.getType() == TokenTypes.LITERAL_THIS) {
                final DetailAST field = getObjectFrame(methodsFrame).findField(name);
                result = "String".equals(getFieldType(field));
            }
            else {
                final String className = previousSiblingAst.getText();
                FieldFrame frame = getObjectFrame(methodsFrame);
                while (frame != null) {
                    if (className.equals(frame.getFrameName())) {
                        final DetailAST field = frame.findField(name);
                        result = "String".equals(getFieldType(field));
                        break;
                    }
                    frame = getObjectFrame(frame.getParent());
                }
            }
        }
        else {
            FieldFrame frame = methodsFrame;
            while (frame != null) {
                final DetailAST field = frame.findField(name);
                if (field != null && (frame.isClassOrEnumOrEnumConstDef()
                        || checkLineNo(field, objCalledOn))) {
                    result = "String".equals(getFieldType(field));
                    break;
                }
                frame = frame.getParent();
            }
        }
        return result;
    }

    /**
     * Get the nearest parent frame which is CLASS_DEF, ENUM_DEF or ENUM_CONST_DEF.
     * @param frame to start the search from.
     * @return the nearest parent frame which is CLASS_DEF, ENUM_DEF or ENUM_CONST_DEF.
     */
    private static FieldFrame getObjectFrame(FieldFrame frame) {
        FieldFrame objectFrame = frame;
        while (objectFrame != null) {
            if (objectFrame.isClassOrEnumOrEnumConstDef()) {
                break;
            }
            else {
                objectFrame = objectFrame.getParent();
            }
        }
        return objectFrame;
    }

    /**
     * Check whether the field is declared before the method call in case of
     * methods and initialization blocks.
     * @param field field to check.
     * @param objCalledOn object equals method called on.
     * @return true if the field is declared before the method call.
     */
    private static boolean checkLineNo(DetailAST field, DetailAST objCalledOn) {
        boolean result = false;
        if (field.getLineNo() < objCalledOn.getLineNo()
                || field.getLineNo() == objCalledOn.getLineNo()
                && field.getColumnNo() < objCalledOn.getColumnNo()) {
            result = true;
        }
        return result;
    }

    /**
     * Get field type.
     * @param field to get the type from.
     * @return type of the field.
     */
    private static String getFieldType(DetailAST field) {
        String fieldType = null;
        final DetailAST identAst = field.findFirstToken(TokenTypes.TYPE)
                .findFirstToken(TokenTypes.IDENT);
        if (identAst != null) {
            fieldType = identAst.getText();
        }
        return fieldType;
    }

    /**
     * Holds the names of fields of a type.
     */
    private static class FieldFrame {
        /** name of the class, enum or enum constant declaration. */
        private String frameName;

        /** parent frame. */
        private final FieldFrame parent;

        /** set of frame's children. */
        private final Set<FieldFrame> children = Sets.newHashSet();

        /** set of fields. */
        private final Set<DetailAST> fields = Sets.newHashSet();

        /** set of equals calls. */
        private final Set<DetailAST> methodCalls = Sets.newHashSet();

        /** whether the frame is CLASS_DEF, ENUM_DEF or ENUM_CONST_DEF. */
        private boolean classOrEnumOrEnumConstDef;

        /**
         * Creates new frame.
         * @param parent parent frame.
         */
        public FieldFrame(FieldFrame parent) {
            this.parent = parent;
        }

        /**
         * Set the frame name.
         * @param frameName value to set.
         */
        public void setFrameName(String frameName) {
            this.frameName = frameName;
        }

        /**
         * Getter for the frame name.
         * @return frame name.
         */
        public String getFrameName() {
            return frameName;
        }

        /**
         * Getter for the parent frame.
         * @return parent frame.
         */
        public FieldFrame getParent() {
            return parent;
        }

        /**
         * Getter for frame's children.
         * @return children of this frame.
         */
        public Set<FieldFrame> getChildren() {
            return children;
        }

        /**
         * Add child frame to this frame.
         * @param child frame to add.
         */
        public void addChild(FieldFrame child) {
            children.add(child);
        }

        /**
         * Add field to this FieldFrame.
         * @param field the ast of the field.
         */
        public void addField(DetailAST field) {
            fields.add(field);
        }

        /**
         * Sets isClassOrEnum.
         * @param value value to set.
         */
        public void setClassOrEnumOrEnumConstDef(boolean value) {
            this.classOrEnumOrEnumConstDef = value;
        }

        /**
         * Getter for classOrEnumOrEnumConstDef.
         * @return classOrEnumOrEnumConstDef.
         */
        public boolean isClassOrEnumOrEnumConstDef() {
            return classOrEnumOrEnumConstDef;
        }

        /**
         * Add method call to this frame.
         * @param methodCall METHOD_CALL ast.
         */
        public void addMethodCall(DetailAST methodCall) {
            methodCalls.add(methodCall);
        }

        /**
         * Determines whether this FieldFrame contains the field.
         * @param name name of the field to check.
         * @return true if this FieldFrame contains instance field field.
         */
        public DetailAST findField(String name) {
            for (DetailAST field: fields) {
                if (getFieldName(field).equals(name)) {
                    return field;
                }
            }
            return null;
        }

        /**
         * Getter for frame's method calls.
         * @return method calls of this frame.
         */
        public Set<DetailAST> getMethodCalls() {
            return methodCalls;
        }

        /**
         * Get the name of the field.
         * @param field to get the name from.
         * @return name of the field.
         */
        private static String getFieldName(DetailAST field) {
            return field.findFirstToken(TokenTypes.IDENT).getText();
        }
    }
}
