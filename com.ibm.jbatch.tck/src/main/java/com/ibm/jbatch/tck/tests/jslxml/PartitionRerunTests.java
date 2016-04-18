/*
 * Copyright 2016 International Business Machines Corp.
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

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.util.List;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.annotations.SpecRef;
import com.ibm.jbatch.tck.annotations.TCKTest;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

public class PartitionRerunTests {
	static JobOperatorBridge jobOp = null;

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
	
	/*
	 * @testName: testRerunPartitionAndBatchlet
	 * 
	 * @assertion: Tests partition restart in two cases:
	 * 
	 *  a) when we fail in a partitioned step with a mix of COMPLETED and FAILED partitions
	 *  b) when we fail in a subsequent step after having COMPLETED a partitioned step with allow-start-if-complete=true
	 * 
	 * In case a) the failing partitions should re-execute on job restart, but only those failing partitions (not the previously-COMPLETED partitions) should execute.
	 * In case b) all partitions should re-execute, since this amounts to an entirely new execution of the step.
	 * 
	 * @test_Strategy: In the first job execution the test fails in one partition and passes in the other two while
	 * running through step1.  The second execution ensures that the one partition that failed in step1 is the only one to be run 
	 * again; we complete step1 then and fail in step 2.  In the third execution, since step1 has previously completed
	 * and allow-start-if-complete="true", we verify that all three partitions in step1 are re-executed.
	 */
	@TCKTest(
			specRef={
					@SpecRef(section={"10.8.4","8.2"},version="1.0", note="Within Sec. 10.8.4 see sequence #3.c"),
			},
			issueRef="https://java.net/bugzilla/show_bug.cgi?id=6494",
			note={
					"Note the spec doesn't explicitly describe this combination of partitions plus allow-start-if-complete=\"true\", but it seems the only valid interpretation.",
					"TODO - Update spec and reference new entry at that point."
			},			
			tckVersionUpdated="1.1.WORKING")
	@Test
	@org.junit.Test
	public void testRerunPartitionAndBatchlet() throws Exception {
		Properties origParams = new Properties();
		origParams.setProperty("force.failure", "true");
		origParams.setProperty("force.failure2", "false");

		JobExecution je = jobOp.startJobAndWaitForResult("partitionRerun", origParams);
		long execId = je.getExecutionId();
		
		checkStepExecId(je, "step1", 2);
		assertWithMessage("Didn't fail as expected", BatchStatus.FAILED, je.getBatchStatus());
		
		//Now run again, since we failed in one partition on the first run this run should have only that one partition rerun
		Properties restartParams = new Properties();
		restartParams.setProperty("force.failure", "false");
		restartParams.setProperty("force.failure2", "true");
		JobExecution restartje = jobOp.restartJobAndWaitForResult(execId, restartParams);
		long restartExecId = restartje.getExecutionId();

		checkStepExecId(restartje, "step1", 1);
		assertWithMessage("Didn't fail as expected", BatchStatus.FAILED, jobOp.getJobExecution(restartExecId).getBatchStatus());

		//Now a third time where we rerun from a fail in step to and expect allow-start-if-complete='true' variable to take over
		//since the failed partitions already reran.
		Properties restartParams2 = new Properties();
		restartParams2.setProperty("force.failure", "false");
		restartParams2.setProperty("force.failure2", "false");
		JobExecution restartje2 = jobOp.restartJobAndWaitForResult(restartExecId, restartParams2);
		long restartExecId2 = restartje2.getExecutionId();

		assertWithMessage("Didn't complete successfully", BatchStatus.COMPLETED, jobOp.getJobExecution(restartExecId2).getBatchStatus());
		checkStepExecId(restartje2, "step1", 3);				
	}
	
	/**
	 * 
	 * @param je  
	 * @param stepName
	 * @param numPartitionResults
	 */
	private void checkStepExecId(JobExecution je, String stepName, int numPartitionResults){
		List<StepExecution> stepExecs = jobOp.getStepExecutions(je.getExecutionId());
		
		Long stepExecId = null;
		for (StepExecution se : stepExecs) {
			if (se.getStepName().equals(stepName)) {
				stepExecId = se.getStepExecutionId();
				break;
			}
		}
		
		if (stepExecId == null) {
			throw new IllegalStateException("Didn't find step1 execution for job execution: " + je.getExecutionId());
		}
				
		String[] retvals = je.getExitStatus().split(",");
		assertWithMessage("Found different number of segments than expected in exit status string for job execution: " + je.getExecutionId(),
				 numPartitionResults, retvals.length);
		
		for(int i=0;i<retvals.length;i++){
			assertWithMessage("Did not return a number/numbers matching the stepExecId", stepExecId.longValue(), Long.parseLong(retvals[i]));
		}
	}

}
