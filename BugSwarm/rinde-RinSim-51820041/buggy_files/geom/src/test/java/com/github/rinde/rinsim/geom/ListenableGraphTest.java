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

import static com.github.rinde.rinsim.geom.ListenableGraph.EventTypes.ADD_CONNECTION;
import static com.github.rinde.rinsim.geom.ListenableGraph.EventTypes.CHANGE_CONNECTION_DATA;
import static com.github.rinde.rinsim.geom.ListenableGraph.EventTypes.REMOVE_CONNECTION;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.geom.ListenableGraph.EventTypes;
import com.github.rinde.rinsim.geom.ListenableGraph.GraphEvent;

/**
 * Tests for {@link ListenableGraph}.
 * @author Rinde van Lon
 */
public class ListenableGraphTest {

  Point a = new Point(0, 0);
  Point b = new Point(1, 0);
  Point c = new Point(2, 0);
  Point d = new Point(3, 0);
  Point e = new Point(4, 0);

  /**
   * Test whether the correct events are dispatched for all modifications.
   */
  @Test
  public void testModifications() {
    ListenerEventHistory history = new ListenerEventHistory();
    ListenableGraph<LengthData> graph = new ListenableGraph<>(
        new MultimapGraph<LengthData>());
    graph.getEventAPI().addListener(history, EventTypes.values());
    graph.getEventAPI().addListener(new GraphModificationChecker());

    graph.addConnection(a, b);
    assertEquals(asList(ADD_CONNECTION), history.getEventTypeHistory());

    graph.addConnection(b, a, null);
    assertEquals(asList(ADD_CONNECTION, ADD_CONNECTION),
        history.getEventTypeHistory());

    graph.addConnections(Arrays.<Connection<LengthData>> asList(
        new Connection<>(a, d, new LengthData(10d)),
        new Connection<>(d, e, new LengthData(7d))));
    assertEquals(
        asList(ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION),
        history.getEventTypeHistory());

    graph.removeConnection(d, e);
    assertEquals(
        asList(ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION,
            REMOVE_CONNECTION),
        history.getEventTypeHistory());

    graph.setConnectionData(a, d, new LengthData(16d));
    assertEquals(
        asList(ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION,
            REMOVE_CONNECTION, CHANGE_CONNECTION_DATA),
        history.getEventTypeHistory());

    graph.removeNode(a);
    assertEquals(
        asList(ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION, ADD_CONNECTION,
            REMOVE_CONNECTION, CHANGE_CONNECTION_DATA, REMOVE_CONNECTION,
            REMOVE_CONNECTION, REMOVE_CONNECTION),
        history.getEventTypeHistory());

    assertTrue(graph.isEmpty());
  }

  /**
   * Tests that merging two graphs generates correct modification events.
   */
  @Test
  public void testMerge() {
    ListenableGraph<LengthData> graph1 = new ListenableGraph<>(
        new MultimapGraph<LengthData>());
    ListenableGraph<LengthData> graph2 = new ListenableGraph<>(
        new MultimapGraph<LengthData>());

    graph1.addConnection(a, b);
    graph1.addConnection(b, a);

    graph2.addConnection(a, c);
    graph2.addConnection(c, a);

    graph1.getEventAPI().addListener(new GraphModificationChecker());
    ListenerEventHistory history = new ListenerEventHistory();
    graph1.getEventAPI().addListener(history);
    graph1.merge(graph2);

    assertEquals(asList(ADD_CONNECTION, ADD_CONNECTION),
        history.getEventTypeHistory());

    assertEquals(2, history.getHistory().size());
    assertEquals(new GraphEvent(ADD_CONNECTION, graph1, a, c, null), history
        .getHistory().get(0));
    assertEquals(new GraphEvent(ADD_CONNECTION, graph1, c, a, null), history
        .getHistory().get(1));
  }

  /**
   * Tests that modifications can not be applied directly to a connection.
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testUnmodifiableConnection() {
    ListenableGraph<LengthData> graph = new ListenableGraph<>(
        new MultimapGraph<LengthData>());
    graph.addConnection(a, b);
    graph.getConnection(a, b).setData(null);
  }

  /**
   * Tests that modifications can not be applied directly to a connection.
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testUnmodifiableConnections() {
    ListenableGraph<LengthData> graph = new ListenableGraph<>(
        new MultimapGraph<LengthData>());
    graph.addConnection(a, b);
    graph.getConnections().iterator().next().setData(null);
  }

  static class GraphModificationChecker implements Listener {

    @Override
    public void handleEvent(Event e) {
      GraphEvent ge = (GraphEvent) e;
      if (e.getEventType() == ADD_CONNECTION) {
        assertTrue(ge.getGraph().hasConnection(ge.getFrom(), ge.getTo()));
        assertEquals(ge.getConnData(),
            ge.getGraph().getConnection(ge.getFrom(), ge.getTo()).getData());
      } else if (e.getEventType() == REMOVE_CONNECTION) {
        assertFalse(ge.getGraph().hasConnection(ge.getFrom(), ge.getTo()));
      } else if (e.getEventType() == CHANGE_CONNECTION_DATA) {
        assertTrue(ge.getGraph().hasConnection(ge.getFrom(), ge.getTo()));
        assertEquals(ge.getConnData(),
            ge.getGraph().getConnection(ge.getFrom(), ge.getTo()).getData());
      }

    }

  }

}
