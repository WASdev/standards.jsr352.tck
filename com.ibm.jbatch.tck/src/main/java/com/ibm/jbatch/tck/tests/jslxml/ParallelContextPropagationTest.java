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

import com.ibm.jbatch.tck.artifacts.specialized.ParallelContextPropagationArtifacts;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ParallelContextPropagationTest {

	private static JobOperatorBridge jobOp = null;
	private static int sleepTime = 20000;
	
	@BeforeMethod
	@BeforeClass
	public static void setup() throws Exception {
		jobOp = new JobOperatorBridge();
	}


    /*
     * @testName: testPartitionContextPropagation
     * 
     * @assertion: Ensure that execution ID, instance ID, and step execution ID stay consistant through a fixed amount of partitions
     * 
     * @test_Strategy: get all id's then execute jobs while checking the initial recorded id values against the values being returned
     * from the partitions ran
     */
	@Test
	@org.junit.Test
	public void testPartitionContextPropagation() throws Exception {
		
		JobExecution je = jobOp.startJobAndWaitForResult("partitionCtxPropagation", null);
		Thread.sleep(sleepTime);
		
		// Check job COMPLETED since some validation is crammed into the execution.
		assertEquals("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct exec id and instance id
		long theExecId = je.getExecutionId();
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
     * 
     * @assertion: Ensure that execution ID, instance ID, and step execution ID stay consistant through splits and flows
     * 
     * @test_Strategy: get all id's then execute jobs while checking the initial recorded id values against the values being returned
     * from within the split flows in the job
     */
	@Test
	@org.junit.Test
	public void testSplitFlowContextPropagation() throws Exception {

		JobExecution je = jobOp.startJobAndWaitForResult("splitFlowCtxPropagation", null);
		Thread.sleep(sleepTime);

		// Check job COMPLETED since some validation is crammed into the execution.
		assertEquals("Test successful completion", "COMPLETED", je.getBatchStatus().toString());

		// Get the correct instance id
		long theExecId = je.getExecutionId();
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
}
