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

import static com.ibm.jbatch.tck.utils.AssertionUtils.*;

import java.util.List;
import java.util.Properties;

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.annotations.APIRef;
import com.ibm.jbatch.tck.annotations.TCKTest;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class ParallelContextPropagationTests {

	private static JobOperatorBridge jobOp = null;

	/**
	 * @testName: testPartitionContextPropagation
	 * 
	 * @assertion: Ensures that the correct values of JobContext and StepContext data (ids and other) can be accessed from an artifact running in a partition
	 * 
	 * 		JobContext#getProperties()
	 *		JobContext#getJobName();
	 *		JobContext#getExecutionId();
	 *		JobContext#getInstanceId();
	 *		StepContext#getStepExecutionId();
	 *      StepContext#getProperties()
	 *
	 * @test_Strategy: Certain values like job name and job and step properties are checked against hard-coded values within the executing batchlet.
	 * The execution and instance ids obtained from the contexts are passed to the analyzer from a collector where they are aggregated into the job exit status 
	 * in a special format, and finally these are parsed out and checked against the corresponding values obtained from the JobExecution and StepExecutions.
	 */ 
	@TCKTest(
			apiRef={
					@APIRef(className="javax.batch.runtime.context.JobContext"),
					@APIRef(className="javax.batch.runtime.context.StepContext")
			},
			issueRef="https://java.net/bugzilla/show_bug.cgi?id=5164",
			note="There is no particular place in the spec that says that partitions and split " +
					"flows share the same values for the getters tested as the top-level JobContext/StepContext.",
			tckVersionUpdated="1.1.WORKING")
	@Test
	@org.junit.Test
	public void testPartitionContextPropagation() throws Exception {

		String METHOD = "testPartitionContextPropagation";

		JobExecution je = jobOp.startJobAndWaitForResult("partitionCtxPropagation", null);

		// Check job COMPLETED since some validation is crammed into the execution.
		assertWithMessage("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct exec id and instance id
		long theExecId = je.getExecutionId();
		long theInstanceId = jobOp.getJobInstance(theExecId).getInstanceId();

		// Get the correct step execution id
		List<StepExecution> se = jobOp.getStepExecutions(theExecId);
		assertWithMessage("Number StepExecutions", 1, se.size());
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
			assertWithMessage("For partition # " + i + ", check job execution id", theExecId, Long.parseLong(jobExecIDs[i]));
			assertWithMessage("For partition # " + i + ", check job instance id", theInstanceId, Long.parseLong(jobInstanceIDs[i]));
			assertWithMessage("For partition # " + i + ", check step exec id", theStepExecId, Long.parseLong(stepExecIDs[i]));
		}
	}


    /**
	 * @testName: testSplitFlowContextPropagation
	 * 
	 * @assertion: Ensures that the correct values of JobContext and StepContext data (ids and other) can be accessed from an artifact running in a split-flow. 
	 * 
	 * 		JobContext#getProperties()
	 *		JobContext#getJobName();
	 *		JobContext#getExecutionId();
	 *		JobContext#getInstanceId();
	 *		StepContext#getStepExecutionId();
	 *
	 * @test_Strategy: Certain values like job name and properties are checked against hard-coded values within the executing batchlet.
	 * The execution and instance ids obtained from the contexts are set within the step exit status in a special format, and finally these are parsed out 
	 * and checked against the corresponding values obtained from the JobExecution and StepExecutions. 
	 */
	@TCKTest(
			apiRef={
					@APIRef(className="javax.batch.runtime.context.JobContext"),
					@APIRef(className="javax.batch.runtime.context.StepContext")
			},
			issueRef="https://java.net/bugzilla/show_bug.cgi?id=5164",
			note="There is no particular place in the spec that says that partitions and split " +
					"flows share the same values for the getters tested as the top-level JobContext/StepContext.",
			tckVersionUpdated="1.1.WORKING")
	@Test
	@org.junit.Test
	public void testSplitFlowContextPropagation() throws Exception {

		String METHOD = "testSplitFlowContextPropagation";

		JobExecution je = jobOp.startJobAndWaitForResult("splitFlowCtxPropagation", null);

		// Check job COMPLETED since some validation is crammed into the execution.
		assertWithMessage("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct instance id
		long theExecId = je.getExecutionId();
		long theInstanceId = jobOp.getJobInstance(theExecId).getInstanceId();

		List<StepExecution> stepExecutions = jobOp.getStepExecutions(theExecId);
		assertWithMessage("Number StepExecutions", 2, stepExecutions.size());

		for (StepExecution se : stepExecutions) {
			// Ignore part before ':'
			String toParse = se.getExitStatus().split(":")[1];

			String execIdStr = toParse.substring(toParse.indexOf("J") + 1, toParse.indexOf("I"));
			assertWithMessage("check job execution id", theExecId, Long.parseLong(execIdStr));

			String instanceId = toParse.substring(toParse.indexOf("I") + 1, toParse.indexOf("S"));
			assertWithMessage("check job instance id", theInstanceId, Long.parseLong(instanceId));

			String stepId = toParse.substring(toParse.indexOf("S") + 1);
			assertWithMessage("check step execution id", se.getStepExecutionId(), Long.parseLong(stepId));
		}
	}

	private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

	public void setup(String[] args, Properties props) throws Exception {

		String METHOD = "setup";

		try {
			jobOp = new JobOperatorBridge();
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/* cleanup */
	public void  cleanup()	{		
		jobOp = null;
	}

	@BeforeTest
	@Before
	public void beforeTest() throws ClassNotFoundException {
		jobOp = new JobOperatorBridge(); 
	}

	@AfterTest
	public void afterTest() {
		jobOp = null;
	}
}
