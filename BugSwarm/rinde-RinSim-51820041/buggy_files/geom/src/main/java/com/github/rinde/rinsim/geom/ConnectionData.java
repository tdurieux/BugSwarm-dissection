/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.geom;

/**
 * Simple interface to represent data associated to a {@link Connection} in a
 * {@link Graph}.
 * @author Bartosz Michalik 
 * @author Rinde van Lon 
 * @since 2.0
 */
public interface ConnectionData {

  /**
   * This method can be implemented to override the default length (euclidean
   * distance).
   * @return The length of the {@link Connection}.
   */
  double getLength();
}
