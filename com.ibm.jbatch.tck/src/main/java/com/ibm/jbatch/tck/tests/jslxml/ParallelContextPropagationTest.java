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
package com.ibm.jbatch.tck.tests.jslxml;

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


public class ParallelContextPropagationTest {

	//private static JobOperatorBridge jobOp = null;
	private static JobOperator jobOp;
	private static int sleepTime = 20000;
	
	@BeforeMethod
	@BeforeClass
	public static void setup() throws Exception {
		//jobOp = new JobOperatorBridge();
		jobOp = BatchRuntime.getJobOperator();
	}


    /*
     * @testName: testPartitionContextPropagation
     * @assertion:
     * @test_Strategy: 
     */
	@Test
	@org.junit.Test
	//@TCKCandidate("Probably a good one to add given confusion caused by Bug 5164")
	public void testPartitionContextPropagation() throws Exception {
		
		long theExecId = jobOp.start("partitionCtxPropagation", null);
		Thread.sleep(sleepTime);
		
		// Check job COMPLETED since some validation is crammed into the execution.
		JobExecution je = jobOp.getJobExecution(theExecId);
		assertEquals("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct instance id
		long theInstanceId = jobOp.getJobInstance(theExecId).getInstanceId();
		
		// Get the correct step execution id
		List<StepExecution> se = jobOp.getStepExecutions(theExecId);
		assertEquals("Number StepExecutions", 1, se.size());
		long theStepExecId = se.get(0).getStepExecutionId();
		
		
		// Now parse the exit status to view the partitions' own views of the job execution id, job instance id,
		// and step execution ids, via JobContext and StepContext.
		
		String status = je.getExitStatus();
		String[] statusIDs = status.split(":");
		int numberOfPartitions = statusIDs.length - 1; 
		String[] jobExecIDs = new String[numberOfPartitions];
		String[] jobInstanceIDs = new String[numberOfPartitions];
		String[] stepExecIDs = new String[numberOfPartitions];

		//before the first ":" is unimportant, so use a 1-index.
		for (int i = 1; i <= numberOfPartitions; i++) {
			jobExecIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("J") + 1, statusIDs[i].indexOf("I"));
			jobInstanceIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("I") + 1, statusIDs[i].indexOf("S"));
			stepExecIDs[i - 1] = statusIDs[i].substring(statusIDs[i].indexOf("S") + 1);
		}

		// Back to 0-indexed counting
		for (int i = 0; i < numberOfPartitions; i++) {
			assertEquals("For partition # " + i + ", check job execution id", theExecId, Long.parseLong(jobExecIDs[i]));
			assertEquals("For partition # " + i + ", check job instance id", theInstanceId, Long.parseLong(jobInstanceIDs[i]));
			assertEquals("For partition # " + i + ", check step exec id", theStepExecId, Long.parseLong(stepExecIDs[i]));
		}
	}


    /*
     * @testName: testSplitFlowContextPropagation
     * @assertion:
     * @test_Strategy: 
     */
	@Test
	@org.junit.Test
	//@TCKCandidate("Probably a good one to add given confusion caused by Bug 5164")
	public void testSplitFlowContextPropagation() throws Exception {

		long theExecId = jobOp.start("splitFlowCtxPropagation", null);
		Thread.sleep(sleepTime);

		// Check job COMPLETED since some validation is crammed into the execution.
		JobExecution je = jobOp.getJobExecution(theExecId);
		assertEquals("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct instance id
		long theInstanceId = jobOp.getJobInstance(theExecId).getInstanceId();
		
		List<StepExecution> stepExecutions = jobOp.getStepExecutions(theExecId);
		assertEquals("Number StepExecutions", 2, stepExecutions.size());

		for (StepExecution se : stepExecutions) {
			// Ignore part before ':'
			String toParse = se.getExitStatus().split(":")[1];

			String execIdStr = toParse.substring(toParse.indexOf("J") + 1, toParse.indexOf("I"));
			assertEquals("check job execution id", theExecId, Long.parseLong(execIdStr));

			String instanceId = toParse.substring(toParse.indexOf("I") + 1, toParse.indexOf("S"));
			assertEquals("check job instance id", theInstanceId, Long.parseLong(instanceId));

			String stepId = toParse.substring(toParse.indexOf("S") + 1);
			assertEquals("check step execution id", se.getStepExecutionId(), Long.parseLong(stepId));
		}
	}


	/**
	 * 
	 * Test artifacts below
	 *
	 */
	public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";
	
	@Named("ParallelContextPropagationTestSFB")
	public static class SFB extends AbstractBatchlet {

		@Inject JobContext jobCtx;  @Inject StepContext stepCtx;

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

	@Named("ParallelContextPropagationTestPB")
	public static class PB extends AbstractBatchlet {

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

	@Named("ParallelContextPropagationTestC")
	public static class C implements PartitionCollector {

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

	@Named("ParallelContextPropagationTestA")
	public static class A extends AbstractPartitionAnalyzer {

		@Inject JobContext jobCtx;

		@Override
		public void analyzeCollectorData(Serializable data) throws Exception {
			jobCtx.setExitStatus(jobCtx.getExitStatus() + data);
		}
	}
}
