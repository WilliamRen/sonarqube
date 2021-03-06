/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.highlighting;

import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import java.io.Serializable;

public class SyntaxHighlightingRule implements Serializable {

  private final int startPosition;
  private final int endPosition;
  private final TypeOfText textType;

  private SyntaxHighlightingRule(int startPosition, int endPosition, TypeOfText textType) {
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.textType = textType;
  }

  public static SyntaxHighlightingRule create(int startPosition, int endPosition, TypeOfText textType) {
    return new SyntaxHighlightingRule(startPosition, endPosition, textType);
  }

  public int getStartPosition() {
    return startPosition;
  }

  public int getEndPosition() {
    return endPosition;
  }

  public TypeOfText getTextType() {
    return textType;
  }

  @Override
  public String toString() {
    return "" + startPosition + "," + endPosition + "," + textType.cssClass();
  }
}
