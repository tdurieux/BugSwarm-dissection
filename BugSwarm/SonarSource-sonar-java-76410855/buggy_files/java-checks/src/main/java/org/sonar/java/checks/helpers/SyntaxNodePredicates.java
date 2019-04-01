/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
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
package org.sonar.java.checks.helpers;

import com.google.common.base.Predicate;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Set;

public class SyntaxNodePredicates {

  private SyntaxNodePredicates() {
    // Useful predicates to be used with syntax nodes
  }

  public static Predicate<Tree> kind(final Tree.Kind syntaxNodeKind) {
    return new Predicate<Tree>() {
      @Override
      public boolean apply(Tree syntaxNode) {
        return syntaxNode.is(syntaxNodeKind);
      }
    };
  }

  public static Predicate<Tree> kinds(final Set<Tree.Kind> kinds) {
    return new Predicate<Tree>() {
      @Override
      public boolean apply(Tree input) {
        return kinds.contains(input.kind());
      }
    };
  }

}
