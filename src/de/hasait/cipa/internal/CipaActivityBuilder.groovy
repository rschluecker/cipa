/*
 * Copyright (C) 2018 by Sebastian Hasait (sebastian at hasait dot de)
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

package de.hasait.cipa.internal

import com.cloudbees.groovy.cps.NonCPS
import de.hasait.cipa.Cipa
import de.hasait.cipa.CipaNode
import de.hasait.cipa.activity.CipaActivity
import de.hasait.cipa.activity.CipaActivityRunContext
import de.hasait.cipa.resource.CipaFileResource
import de.hasait.cipa.resource.CipaResource
import de.hasait.cipa.resource.CipaResourceWithState

/**
 *
 */
class CipaActivityBuilder implements Serializable {

	private final Cipa cipa
	private final CipaNode node

	private final Set<CipaResourceWithState<?>> requiresRead = []
	private final Set<CipaResourceWithState<?>> requiresWrite = []
	private final Set<CipaResourceWithState<?>> provides = []

	private boolean used = false

	CipaActivityBuilder(Cipa cipa, CipaNode node) {
		this.cipa = cipa
		this.node = node
	}

	@NonCPS
	private String newState() {
		return UUID.randomUUID().toString()
	}

	@NonCPS
	CipaResourceWithState<CipaFileResource> providesDir(String relDir, boolean global = false) {
		CipaResourceWithState<CipaFileResource> cipaResourceWithState = cipa.newFileResourceWithState(global ? null : node, relDir, newState())
		provides.add(cipaResourceWithState)
		return cipaResourceWithState
	}

	@NonCPS
	public <R extends CipaResource> CipaResourceWithState<R> provides(R resource) {
		CipaResourceWithState<R> cipaResourceWithState = cipa.newResourceWithState(resource, newState())
		provides.add(cipaResourceWithState)
		return cipaResourceWithState
	}

	@NonCPS
	public <R extends CipaResource> CipaResourceWithState<R> modifies(CipaResourceWithState<R> modified) {
		requiresWrite.add(modified)
		CipaResourceWithState<R> newResourceState = cipa.newResourceState(modified, newState())
		provides.add(newResourceState)
		return newResourceState
	}

	@NonCPS
	public <R extends CipaResource> void reads(CipaResourceWithState<R> read) {
		requiresRead.add(read)
	}

	@NonCPS
	CipaActivity create(String name, Closure<?> logic) {
		if (provides && used) {
			throw new IllegalStateException("A builder having provided resources can be only used once: ${name}")
		}

		used = true
		return new BuildActivity(cipa, node, name, logic, requiresRead, requiresWrite, provides);
	}

	private static class BuildActivity implements CipaActivity, Serializable {

		private final String name
		private final CipaNode node
		private final Closure<?> logic
		private final Set<CipaResourceWithState<?>> requiresRead
		private final Set<CipaResourceWithState<?>> requiresWrite
		private final Set<CipaResourceWithState<?>> provides

		BuildActivity(Cipa cipa, CipaNode node, String name, Closure<?> logic,
					  Set<CipaResourceWithState<?>> requiresRead, Set<CipaResourceWithState<?>> requiresWrite, Set<CipaResourceWithState<?>> provides) {
			this.name = name
			this.node = node
			this.logic = logic
			this.requiresRead = requiresRead
			this.requiresWrite = requiresWrite
			this.provides = provides

			cipa.addBean(this)
		}

		String getName() {
			return name
		}

		CipaNode getNode() {
			return node
		}

		@NonCPS
		Set<CipaResourceWithState<?>> getRunRequiresRead() {
			return requiresRead
		}

		@NonCPS
		Set<CipaResourceWithState<?>> getRunRequiresWrite() {
			return requiresWrite
		}

		@NonCPS
		Set<CipaResourceWithState<?>> getRunProvides() {
			return provides
		}

		void prepareNode() {
			// nop
		}

		void runActivity(CipaActivityRunContext runContext) {
			logic.call(runContext)
		}

	}

}
