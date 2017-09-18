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

package de.hasait.cipa.test

import com.cloudbees.groovy.cps.NonCPS
import de.hasait.cipa.Cipa
import de.hasait.cipa.CipaInit
import de.hasait.cipa.CipaNode
import de.hasait.cipa.Script
import de.hasait.cipa.activity.CheckoutActivity
import de.hasait.cipa.activity.CipaActivity
import de.hasait.cipa.activity.CipaAroundActivity
import de.hasait.cipa.activity.StashFilesActivity
import de.hasait.cipa.activity.UnstashFilesActivity
import de.hasait.cipa.internal.CipaActivityWrapper
import de.hasait.cipa.resource.CipaFileResource
import de.hasait.cipa.resource.CipaResourceWithState
import de.hasait.cipa.resource.CipaStashResource

/**
 *
 */
class TestPipeline implements CipaInit, CipaAroundActivity, Serializable {

	private final Cipa cipa
	private Script script
	private def rawScript

	TestPipeline(rawScript) {
		cipa = new Cipa(rawScript)
		cipa.debug = true
		cipa.addBean(this)

		CipaNode node1 = cipa.newNode('node1')
		CipaNode node2 = cipa.newNode('node2')
		CipaResourceWithState<CipaFileResource> mainCheckedOutFiles = new CheckoutActivity(cipa, 'Checkout', 'Main', node1).excludeUser('autouser', 'robot').providedCheckedOutFiles
		CipaResourceWithState<CipaStashResource> mainStash = new StashFilesActivity(cipa, 'StashMain', mainCheckedOutFiles).providedStash
		CipaResourceWithState<CipaFileResource> files = new UnstashFilesActivity(cipa, "UnstashMain", mainStash, node2).providedFiles
		new TestReaderActivity(cipa, "TestR1", files, "StateR1")
		new TestReaderActivity(cipa, "TestR2", files, "StateR2")
		new TestReaderActivity(cipa, "TestR3", files, "StateR3")
		new TestWriterActivity(cipa, "TestW1", files, "StateW1")
		new TestWriterActivity(cipa, "TestW2", files, "StateW2")
		new TestWriterActivity(cipa, "TestW3", files, "StateW3")
	}

	@Override
	void initCipa(Cipa cipa) {
		script = cipa.findBean(Script.class)
		rawScript = script.rawScript

		cipa.configureJDK('JDK8')
		cipa.configureMaven('M3', 'ciserver-settings.xml', 'ciserver-toolchains.xml')
				.setOptions('-Xms1g -Xmx4g -XX:ReservedCodeCacheSize=256m -Dproject.build.sourceEncoding=UTF-8 -Dfile.encoding=UTF-8 -Dmaven.compile.fork=true')
	}

	void run() {
		cipa.run()
	}

	@Override
	void handleDependencyErrors(final CipaActivity activity, final List<CipaActivityWrapper> failedDependencies, final Closure<?> next) {
		rawScript.setCustomBuildProperty(key: "${activity.name}-DepsFailed", value: failedDependencies.size())
		next.call()
	}

	@Override
	void runAroundActivity(CipaActivity activity, Closure<?> next) {
		rawScript.setCustomBuildProperty(key: "${activity.name}-StartTime", value: new Date())
		try {
			script.echo("runAroundActivity ${activity.name}...")
			next.call()
		} catch (err) {
			script.echo("runAroundActivity caught: ${err}")
			script.echo("runAroundActivity caught: ${err?.class}")
			rawScript.setCustomBuildProperty(key: "${activity.name}-Failed", value: err.toString())
			throw err
		} finally {
			rawScript.setCustomBuildProperty(key: "${activity.name}-EndTime", value: new Date())
		}
	}

	@Override
	@NonCPS
	int getRunAroundActivityOrder() {
		return 0
	}

	static void main(String[] args) {
		System.out.println("Test")

		TestRawScript rawScript = new TestRawScript()
		rawScript.env.MAIN_SCM_URL = 'scm://somewhere.git'
		rawScript.env.MAIN_SCM_CREDENTIALS_ID = 'somecreds'
		rawScript.env.NODE_LABEL_PREFIX = 'nlprefix-'

		TestPipeline testPipeline = new TestPipeline(rawScript)
		testPipeline.run()
	}

}
