/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.graph.asm.degree.annotate.directed;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.GraphAlgorithm;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.asm.degree.annotate.DegreeAnnotationFunctions.DegreeCount;
import org.apache.flink.graph.asm.degree.annotate.DegreeAnnotationFunctions.JoinVertexWithVertexDegree;
import org.apache.flink.graph.asm.degree.annotate.DegreeAnnotationFunctions.MapEdgeToTargetId;
import org.apache.flink.types.LongValue;
import org.apache.flink.util.Preconditions;

import static org.apache.flink.api.common.ExecutionConfig.PARALLELISM_DEFAULT;

/**
 * Annotates vertices of a directed graph with the in-degree.
 *
 * @param <K> graph label type
 * @param <VV> vertex value type
 * @param <EV> edge value type
 */
public class VertexInDegree<K, VV, EV>
implements GraphAlgorithm<K, VV, EV, DataSet<Vertex<K, LongValue>>> {

	// Optional configuration
	private boolean includeZeroDegreeVertices = false;

	private int parallelism = PARALLELISM_DEFAULT;

	/**
	 * By default only the edge set is processed for the computation of degree.
	 * When this flag is set an additional join is performed against the vertex
	 * set in order to output vertices with an in-degree of zero.
	 *
	 * @param includeZeroDegreeVertices whether to output vertices with an
	 *                                  in-degree of zero
	 * @return this
	 */
	public VertexInDegree<K, VV, EV> setIncludeZeroDegreeVertices(boolean includeZeroDegreeVertices) {
		this.includeZeroDegreeVertices = includeZeroDegreeVertices;

		return this;
	}

	/**
	 * Override the operator parallelism.
	 *
	 * @param parallelism operator parallelism
	 * @return this
	 */
	public VertexInDegree<K, VV, EV> setParallelism(int parallelism) {
		Preconditions.checkArgument(parallelism > 0 || parallelism == PARALLELISM_DEFAULT,
			"The parallelism must be greater than zero.");

		this.parallelism = parallelism;

		return this;
	}

	@Override
	public DataSet<Vertex<K, LongValue>> run(Graph<K, VV, EV> input)
			throws Exception {
		// t
		DataSet<Vertex<K, LongValue>> targetIds = input
			.getEdges()
			.map(new MapEdgeToTargetId<K, EV>())
				.setParallelism(parallelism)
				.name("Map edge to target ID");

		// t, d(t)
		DataSet<Vertex<K, LongValue>> targetDegree = targetIds
			.groupBy(0)
			.reduce(new DegreeCount<K>())
				.setParallelism(parallelism)
				.name("Degree count");

		if (includeZeroDegreeVertices) {
			targetDegree = input.getVertices()
				.leftOuterJoin(targetDegree)
				.where(0)
				.equalTo(0)
				.with(new JoinVertexWithVertexDegree<K, VV>())
					.setParallelism(parallelism)
					.name("Join zero degree vertices");
		}

		return targetDegree;
	}
}
