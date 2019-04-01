/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.php.api.tree;

import org.sonar.plugins.php.api.tree.declaration.ClassFieldDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.ClassTree;
import org.sonar.plugins.php.api.tree.declaration.FunctionDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.NamespacedNameTree;
import org.sonar.plugins.php.api.tree.declaration.UseDeclarationTree;
import org.sonar.plugins.php.api.tree.declaration.UseDeclarationsTree;
import org.sonar.plugins.php.api.tree.declaration.VariableDeclarationTree;
import org.sonar.plugins.php.api.tree.expression.ArrayAccessExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ArrayInitialiserBracketTree;
import org.sonar.plugins.php.api.tree.expression.ArrayInitialiserFunctionTree;
import org.sonar.plugins.php.api.tree.expression.ArrayPairTree;
import org.sonar.plugins.php.api.tree.expression.AssignmentExpressionTree;
import org.sonar.plugins.php.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.php.api.tree.expression.CastExpressionTree;
import org.sonar.plugins.php.api.tree.expression.CompoundVariableTree;
import org.sonar.plugins.php.api.tree.expression.ComputedVariableTree;
import org.sonar.plugins.php.api.tree.expression.ConditionalExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ExitTree;
import org.sonar.plugins.php.api.tree.expression.ExpandableStringLiteralTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.FunctionExpressionTree;
import org.sonar.plugins.php.api.tree.expression.LexicalVariablesTree;
import org.sonar.plugins.php.api.tree.expression.ListExpressionTree;
import org.sonar.plugins.php.api.tree.expression.LiteralTree;
import org.sonar.plugins.php.api.tree.expression.MemberAccessExpressionTree;
import org.sonar.plugins.php.api.tree.expression.NewExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ParenthesisedExpressionTree;
import org.sonar.plugins.php.api.tree.expression.ReferenceVariableTree;
import org.sonar.plugins.php.api.tree.expression.SpreadArgumentTree;
import org.sonar.plugins.php.api.tree.expression.UnaryExpressionTree;
import org.sonar.plugins.php.api.tree.expression.VariableIdentifierTree;
import org.sonar.plugins.php.api.tree.expression.VariableVariableTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.tree.statement.BlockTree;
import org.sonar.plugins.php.api.tree.statement.BreakStatementTree;
import org.sonar.plugins.php.api.tree.statement.CaseClauseTree;
import org.sonar.plugins.php.api.tree.statement.CatchBlockTree;
import org.sonar.plugins.php.api.tree.statement.ContinueStatementTree;
import org.sonar.plugins.php.api.tree.statement.DeclareStatementTree;
import org.sonar.plugins.php.api.tree.statement.DefaultClauseTree;
import org.sonar.plugins.php.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.php.api.tree.statement.EchoStatementTree;
import org.sonar.plugins.php.api.tree.statement.ElseClauseTree;
import org.sonar.plugins.php.api.tree.statement.ElseifClauseTree;
import org.sonar.plugins.php.api.tree.statement.ExpressionStatementTree;
import org.sonar.plugins.php.api.tree.statement.ForEachStatementTree;
import org.sonar.plugins.php.api.tree.statement.ForStatementTree;
import org.sonar.plugins.php.api.tree.statement.GlobalStatementTree;
import org.sonar.plugins.php.api.tree.statement.GotoStatementTree;
import org.sonar.plugins.php.api.tree.statement.IfStatementTree;
import org.sonar.plugins.php.api.tree.statement.LabelTree;
import org.sonar.plugins.php.api.tree.statement.ReturnStatementTree;
import org.sonar.plugins.php.api.tree.statement.SwitchStatementTree;
import org.sonar.plugins.php.api.tree.statement.ThrowStatementTree;
import org.sonar.plugins.php.api.tree.statement.TraitAdaptationStatementTree;
import org.sonar.plugins.php.api.tree.statement.TraitAliasTree;
import org.sonar.plugins.php.api.tree.statement.TraitMethodReferenceTree;
import org.sonar.plugins.php.api.tree.statement.TraitPrecedenceTree;
import org.sonar.plugins.php.api.tree.statement.TraitUseStatementTree;
import org.sonar.plugins.php.api.tree.statement.TryStatementTree;
import org.sonar.plugins.php.api.tree.statement.WhileStatementTree;
import org.sonar.plugins.php.api.tree.statement.YieldStatementTree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import com.google.common.annotations.Beta;
import com.sonar.sslr.api.AstNodeType;

/**
 * Common interface for all nodes in an abstract syntax tree.
 */
@Beta
public interface Tree {

  boolean is(Kind... kind);

  public enum Kind implements AstNodeType, GrammarRuleKey {

    /**
     * {@link ClassTree}
     */
    CLASS_DECLARATION(ClassTree.class),

    /**
     * {@link ClassTree}
     */
    INTERFACE_DECLARATION(ClassTree.class),

    /**
    * {@link ClassTree}
    */
    TRAIT_DECLARATION(ClassTree.class),

    /**
     * {@link MethodDeclarationTree}
     */
    METHOD_DECLARATION(MethodDeclarationTree.class),

    /**
     * {@link FunctionDeclarationTree}
     */
    FUNCTION_DECLARATION(FunctionDeclarationTree.class),

    /**
     * {@link ClassFieldDeclarationTree}
     */
    CLASS_FIELD_DECLARATION(ClassFieldDeclarationTree.class),

    /**
     * {@link ClassFieldDeclarationTree}
     */
    CLASS_CONSTANT_FIELD_DECLARATION(ClassFieldDeclarationTree.class),

    /**
     * {@link VariableDeclarationTree}
     */
    VARIABLE_DECLARATION(VariableDeclarationTree.class),

    /**
     * {@link UseDeclarationsTree}
     */
    USE_DECLARATIONS(UseDeclarationsTree.class),

    /**
     * {@link UseDeclarationTree}
     */
    USE_DECLARATION(UseDeclarationTree.class),

    /**
     * {@link ParenthesisedExpressionTree}
     */
    PARENTHESISED_EXPRESSION(ParenthesisedExpressionTree.class),

    /**
     * {@link ParenthesisedExpressionTree}
     */
    YIELD_EXPRESSION(ParenthesisedExpressionTree.class),

    /**
     * {@link VariableVariableTree}
     */
    VARIABLE_VARIABLE(VariableVariableTree.class),

    /**
     * {@link ComputedVariableTree}
     */
    COMPUTED_VARIABLE_NAME(ComputedVariableTree.class),

    /**
     * {@link CompoundVariableTree}
     */
    COMPOUND_VARIABLE_NAME(CompoundVariableTree.class),

    /**
     * {@link ArrayAccessExpressionTree}
     */
    ARRAY_ACCESS(ArrayAccessExpressionTree.class),

    /**
     * {@link VariableIdentifierTree}
     */
    VARIABLE_IDENTIFIER(VariableIdentifierTree.class),

    /**
     * {@link ReferenceVariableTree}
     */
    REFERENCE_VARIABLE(ReferenceVariableTree.class),

    /**
     * {@link MemberAccessExpressionTree}
     * {@code =>}
     */
    OBJECT_MEMBER_ACCESS(MemberAccessExpressionTree.class),

    /**
     * {@link MemberAccessExpressionTree}
     * {@code ::}
     */
    CLASS_MEMBER_ACCESS(MemberAccessExpressionTree.class),

    /**
     * {@link FunctionCallTree}
     */
    FUNCTION_CALL(FunctionCallTree.class),

    /**
     * {@link SpreadArgumentTree}
     */
    SPREAD_ARGUMENT(SpreadArgumentTree.class),

    /**
     * {@link NamespacedNameTree}
     */
    NAMESPACED_NAME(NamespacedNameTree.class),

    /**
     * {@link ArrayPairTree}
     */
    ARRAY_PAIR(ArrayPairTree.class),

    /**
     * {@link ArrayInitialiserFunctionTree}
     */
    ARRAY_INITIALISER_FUNCTION(ArrayInitialiserFunctionTree.class),

    /**
     * {@link ArrayInitialiserBracketTree}
     */
    ARRAY_INITIALISER_BRACKET(ArrayInitialiserBracketTree.class),

    /**
     * {@link FunctionExpressionTree}
     */
    FUNCTION_EXPRESSION(FunctionExpressionTree.class),

    /**
     * {@link LexicalVariablesTree}
     */
    LEXICAL_VARS(LexicalVariablesTree.class),

    /**
     * {@link ExitTree}
     */
    EXIT_EXPRESSION(ExitTree.class),

    /**
     * {@link ListExpressionTree}
     */
    LIST_EXPRESSION(ListExpressionTree.class),

    /**
     * {@link NewExpressionTree}
     */
    NEW_EXPRESSION(NewExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code =}
     */
    ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code *=}
     */
    MULTIPLY_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code /=}
     */
    DIVIDE_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code %=}
     */
    REMAINDER_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code +=}
     */
    PLUS_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code -=}
     */
    MINUS_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code <<=}
     */
    LEFT_SHIFT_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code >>=}
     */
    RIGHT_SHIFT_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code &=}
     */
    AND_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code ^=}
     */
    XOR_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code |=}
     */
    OR_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link AssignmentExpressionTree}
     * {@code .=}
     */
    CONCATENATION_ASSIGNMENT(AssignmentExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code *}
     */
    MULTIPLY(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code /}
     */
    DIVIDE(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code %}
     */
    REMAINDER(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code +}
     */
    PLUS(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code -}
     */
    MINUS(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <<}
     */
    LEFT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >>}
     */
    RIGHT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >>>}
     */
    UNSIGNED_RIGHT_SHIFT(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     */
    INSTANCE_OF(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <}
     */
    LESS_THAN(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >}
     */
    GREATER_THAN(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <=}
     */
    LESS_THAN_OR_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code >=}
     */
    GREATER_THAN_OR_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ==}
     */
    EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ===}
     */
    STRICT_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code !=}
     */
    NOT_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code !==}
     */
    STRICT_NOT_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code <>}
     */
    ALTERNATIVE_NOT_EQUAL_TO(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code &}
     */
    BITWISE_AND(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ^}
     */
    BITWISE_XOR(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code |}
     */
    BITWISE_OR(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code &&}
     */
    CONDITIONAL_AND(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code ||}
     */
    CONDITIONAL_OR(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code and}
     */
    ALTERNATIVE_CONDITIONAL_AND(BinaryExpressionTree.class),

    /**
     * {@link BinaryExpressionTree}
     * {@code or}
     */
    ALTERNATIVE_CONDITIONAL_OR(BinaryExpressionTree.class),

    /**
     * {@link ConditionalExpressionTree}
     */
    CONDITIONAL_EXPRESSION(ConditionalExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ++}
     */
    POSTFIX_INCREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code --}
     */
    POSTFIX_DECREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ++}
     */
    PREFIX_INCREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code --}
     */
    PREFIX_DECREMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code +}
     */
    UNARY_PLUS(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code -}
     */
    UNARY_MINUS(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code ~}
     */
    BITWISE_COMPLEMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code !}
     */
    LOGICAL_COMPLEMENT(UnaryExpressionTree.class),

    /**
     * {@link UnaryExpressionTree}
     * {@code @}
     */
    ERROR_CONTROL(UnaryExpressionTree.class),

    /**
     * {@link CastExpressionTree}
     */
    CAST_EXPRESSION(CastExpressionTree.class),

    /**
     * {@link LiteralTree}
     * {@code null}
     */
    NULL_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code true}
     * {@code false}
     */
    BOOLEAN_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code numeric}
     */
    NUMERIC_LITERAL(LiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code string}
     */
    STRING_LITERAL(LiteralTree.class),

    /**
     * {@link ExpandableStringLiteralTree}
     */
    EXPANDABLE_STRING_LITERAL(ExpandableStringLiteralTree.class),

    /**
     * {@link LiteralTree}
     * {@code __CLASS__},
     * {@code __FILE__},
     * {@code __DIR__},
     * {@code __FUNCTION__},
     * {@code __LINE__},
     * {@code __METHOD__},
     * {@code __NAMESPACE__},
     * {@code __TRAIT__}
     */
    MAGIC_CONSTANT(LiteralTree.class),

    /**
     * {@link TraitUseStatementTree}
     */
    TRAIT_USE_STATEMENT(TraitUseStatementTree.class),

    /**
     * {@link BlockTree}
     */
    BLOCK(BlockTree.class),

    /**
     * {@link LabelTree}
     */
    LABEL(LabelTree.class),

    /**
     * {@link IfStatementTree}
     */
    IF_STATEMENT(IfStatementTree.class),

    /**
     * {@link IfStatementTree}
     */
    ALTRNATIVE_IF_STATEMENT(IfStatementTree.class),

    /**
     * {@link ElseifClauseTree}
     */
    ELSEIF_CLAUSE(ElseifClauseTree.class),

    /**
     * {@link ElseifClauseTree}
     */
    ALTERNATIVE_ELSEIF_CLAUSE(ElseifClauseTree.class),

    /**
     * {@link ElseClauseTree}
     */
    ELSE_CLAUSE(ElseClauseTree.class),

    /**
     * {@link ElseClauseTree}
     */
    ALTERNATIVE_ELSE_CLAUSE(ElseClauseTree.class),

    /**
     * {@link WhileStatementTree}
     */
    WHILE_STATEMENT(WhileStatementTree.class),

    /**
     * {@link WhileStatementTree}
     */
    ALTERNATIVE_WHILE_STATEMENT(WhileStatementTree.class),

    /**
     * {@link DoWhileStatementTree}
     */
    DO_WHILE_STATEMENT(DoWhileStatementTree.class),

    /**
     * {@link ForStatementTree}
     */
    FOR_STATEMENT(ForStatementTree.class),

    /**
     * {@link ForStatementTree}
     */
    ALTERNATIVE_FOR_STATEMENT(ForStatementTree.class),

    /**
     * {@link SwitchStatementTree}
     */
    SWITCH_STATEMENT(SwitchStatementTree.class),

    /**
     * {@link SwitchStatementTree}
     */
    ALTERNATIVE_SWITCH_STATEMENT(SwitchStatementTree.class),

    /**
     * {@link CaseClauseTree}
     */
    CASE_CLAUSE(CaseClauseTree.class),

    /**
     * {@link DefaultClauseTree}
     */
    DEFAULT_CLAUSE(DefaultClauseTree.class),

    /**
     * {@link BreakStatementTree}
     */
    BREAK_STATEMENT(BreakStatementTree.class),

    /**
     * {@link ContinueStatementTree}
     */
    CONTINUE_STATEMENT(ContinueStatementTree.class),

    /**
     * {@link ReturnStatementTree}
     */
    RETURN_STATEMENT(ReturnStatementTree.class),

    /**
     * {@link ExpressionStatementTree}
     */
    EXPRESSION_STATEMENT(ExpressionStatementTree.class),

    /**
     * {@link ForEachStatementTree}
     */
    FOREACH_STATEMENT(ForEachStatementTree.class),

    /**
     * {@link ForEachStatementTree}
     */
    ALTERNATIVE_FOREACH_STATEMENT(ForEachStatementTree.class),

    /**
     * {@link DeclareStatementTree}
     */
    DECLARE_STATEMENT(DeclareStatementTree.class),

    /**
     * {@link DeclareStatementTree}
     */
    ALTERNATIVE_DECLARE_STATEMENT(DeclareStatementTree.class),

    /**
     * {@link TryStatementTree}
     */
    TRY_STATEMENT(TryStatementTree.class),

    /**
     * {@link CatchBlockTree}
     */
    CATCH_BLOCK(CatchBlockTree.class),

    /**
     * {@link ThrowStatementTree}
     */
    THROW_STATEMENT(ThrowStatementTree.class),

    /**
     * {@link GotoStatementTree}
     */
    GOTO_STATEMENT(GotoStatementTree.class),

    /**
     * {@link YieldStatementTree}
     */
    YIELD_STATEMENT(YieldStatementTree.class),

    /**
     * {@link GlobalStatementTree}
     */
    GLOBAL_STATEMENT(GlobalStatementTree.class),

    /**
     * {@link EchoStatementTree}
     */
    ECHO_STATEMENT(EchoStatementTree.class),

    /**
     * {@link EchoStatementTree}
     */
    UNSET_VARIABLE_STATEMENT(EchoStatementTree.class),

    /**
     * {@link TraitAdaptationStatementTree}
     */
    TRAIT_ADAPTATION_STATEMENT(TraitAdaptationStatementTree.class),

    /**
     * {@link TraitPrecedenceTree}
     */
    TRAIT_PRECEDENCE(TraitPrecedenceTree.class),

    /**
     * {@link TraitMethodReferenceTree}
     */
    TRAIT_METHOD_REFERENCE(TraitMethodReferenceTree.class),

    /**
     * {@link TraitAliasTree}
     */
    TRAIT_ALIAS(TraitAliasTree.class),

    /**
     * {@link SyntaxToken}
     */
    INLINE_HTML(SyntaxToken.class),

    /**
     * {@link SyntaxToken}
     */
    TOKEN(SyntaxToken.class);

    final Class<? extends Tree> associatedInterface;

    private Kind(Class<? extends Tree> associatedInterface) {
      this.associatedInterface = associatedInterface;
    }

    public Class<? extends Tree> getAssociatedInterface() {
      return associatedInterface;
    }
  }

}
