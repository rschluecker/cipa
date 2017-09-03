/*
 * Copyright (C) 2017 by Sebastian Hasait (sebastian at hasait dot de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hasait.cipa.resource

import de.hasait.cipa.CipaNode

/**
 *
 */
class CipaCustomResource implements CipaResource, Serializable {

	private final CipaNode node
	private final String type
	private final String id
	private final String state

	CipaCustomResource(CipaNode node, String type, String id, String state) {
		this.node = node
		if (!type || type.length() == 0) {
			throw new IllegalArgumentException('type is null or empty')
		}
		this.type = type
		if (!id || id.length() == 0) {
			throw new IllegalArgumentException('id is null or empty')
		}
		this.id = id
		this.state = state
	}

	@Override
	CipaNode getNode() {
		return node
	}

	@Override
	String getDescription() {
		if (node) {
			return "Custom resource [${type}:${id}] on [${node}] in state [${state}]"
		}
		return "Global custom resource [${type}:${id}] in state [${state}]"
	}

	String getType() {
		return type
	}

	String getId() {
		return id
	}

	String getState() {
		return state
	}


}
