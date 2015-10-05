/*
 * Copyright 2014 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.tck.artifacts.specialized;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ParallelContextPropagationArtifacts {

public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";
	
	@javax.inject.Named("PCPCheckJobBatchlet")
	public static class PCPCheckJobBatchlet extends AbstractBatchlet {

		@Inject JobContext jobCtx;
		@Inject StepContext stepCtx;

		@Override
		public String process() throws Exception {

			// Check job properties
			/*
			 * <property name="topLevelJobProperty" value="topLevelJobProperty.value" />
			 */
			String propVal = jobCtx.getProperties().getProperty("topLevelJobProperty");
			String expectedPropVal = "topLevelJobProperty.value";

			if (propVal == null || (!propVal.equals(expectedPropVal))) {
				throw new Exception("Expected propVal of " + expectedPropVal + ", but found: " + propVal);
			}

			// Check job name
			String jobName = jobCtx.getJobName();
			String expectedJobName = "splitFlowCtxPropagation";
			if (!jobName.equals(expectedJobName)) {
				throw new Exception("Expected jobName of " + expectedJobName + ", but found: " + jobName);
			}

			String data = stepExitStatus();
			stepCtx.setExitStatus(stepCtx.getExitStatus() + data);
			return GOOD_EXIT_STATUS;
		}

		private String stepExitStatus() {
			long execId = jobCtx.getExecutionId();
			long instanceId = jobCtx.getInstanceId();
			long stepExecId = stepCtx.getStepExecutionId();

			return ":J" + execId + "I" + instanceId + "S" + stepExecId;
		}
	}

	@javax.inject.Named("PCPProcessBatchlet")
	public static class PCPProcessBatchlet extends AbstractBatchlet {

		@Inject JobContext jobCtx; @Inject StepContext stepCtx;

		@Override
		public String process() throws Exception {

			// Check job properties

			/*
			 * <property name="topLevelJobProperty" value="topLevelJobProperty.value" />
			 */
			String propVal = jobCtx.getProperties().getProperty("topLevelJobProperty");
			assertEquals("Job Property comparison", "topLevelJobProperty.value", propVal);
			
			propVal = stepCtx.getProperties().getProperty("topLevelStepProperty");
			assertEquals("Step Property comparison", "topLevelStepProperty.value", propVal);
			
			assertEquals("Job name", "partitionCtxPropagation", jobCtx.getJobName());

			assertEquals("Step name", "step1", stepCtx.getStepName());

			return GOOD_EXIT_STATUS;
		}

		@Override
		public void stop() throws Exception {}
	}

	@javax.inject.Named("PCPCollector")
	public static class PCPCollector implements PartitionCollector {

		@Inject JobContext jobCtx; @Inject StepContext stepCtx;

		@Override
		public String collectPartitionData() throws Exception {

			assertEquals("step name", "step1", stepCtx.getStepName());

			long jobid = jobCtx.getExecutionId();
			long instanceid = jobCtx.getInstanceId();
			long stepid = stepCtx.getStepExecutionId();

			return ":J" + jobid + "I" + instanceid + "S" + stepid;
		}
	}

	@javax.inject.Named("PCPAnalyzer")
	public static class PCPAnalyzer extends AbstractPartitionAnalyzer {

		@Inject JobContext jobCtx;

		@Override
		public void analyzeCollectorData(Serializable data) throws Exception {
			jobCtx.setExitStatus(jobCtx.getExitStatus() + data);
		}
	}
}