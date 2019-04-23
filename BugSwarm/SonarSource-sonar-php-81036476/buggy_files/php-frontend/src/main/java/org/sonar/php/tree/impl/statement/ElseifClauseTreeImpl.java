package org.sonar.php.tree.impl.statement;

import com.google.common.collect.Iterators;
import org.sonar.php.tree.impl.PHPTree;
import org.sonar.php.tree.impl.lexical.InternalSyntaxToken;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.php.api.tree.statement.ElseifClauseTree;
import org.sonar.plugins.php.api.tree.statement.StatementTree;
import org.sonar.plugins.php.api.visitors.TreeVisitor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ElseifClauseTreeImpl extends PHPTree implements ElseifClauseTree {

  private final Kind KIND;

  private final InternalSyntaxToken elseifToken;
  private final ExpressionTree condition;
  private final InternalSyntaxToken colonToken;
  private final List<StatementTree> statement;

  public ElseifClauseTreeImpl(InternalSyntaxToken elseifToken, ExpressionTree condition, StatementTree statement) {
    this.KIND = Kind.ELSEIF_CLAUSE;

    this.elseifToken = elseifToken;
    this.condition = condition;
    this.statement = Collections.singletonList(statement);

    this.colonToken = null;
  }

  @Override
  public SyntaxToken elseifToken() {
    return elseifToken;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Nullable
  @Override
  public SyntaxToken colonToken() {
    return colonToken;
  }

  @Override
  public List<StatementTree> statement() {
    return statement;
  }

  @Override
  public Kind getKind() {
    return KIND;
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.concat(
        Iterators.forArray(elseifToken, condition, colonToken),
        statement.iterator()
    );
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitElseifClause(this);
  }
}
