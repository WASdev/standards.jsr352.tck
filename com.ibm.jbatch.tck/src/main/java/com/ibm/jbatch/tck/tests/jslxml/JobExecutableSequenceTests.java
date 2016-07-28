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

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertObjEquals;
import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.ann.APIRef;
import com.ibm.jbatch.tck.ann.SpecRef;
import com.ibm.jbatch.tck.ann.TCKTest;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

public class JobExecutableSequenceTests {

	private JobOperatorBridge jobOp = null;

	/**
	 * @testName: testJobExecutableSequenceToUnknown
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of 3 steps (step1 next to step2, step2 next to unknown, step3 unreachable
	 * 				   2. start job 
	 * 				   3. job should fail because it shouldn't be able to transition to unknown
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	
	 @TCKTest(
				specRefs={
						@SpecRef(section="5.3",version="1.0", notes={""})
				},
				apiRefs={
						@APIRef(className="com.ibm.jbatch.tck.artifacts.specialized.MyBatchletImpl")
				},
				versions="1.1.WORKING",
				assertions={"Section 5.3 Flow"}
				)    
	@Test
	@org.junit.Test
	public void testJobExecutableSequenceToUnknown() throws Exception {

		String METHOD = "testJobExecutableSequenceToUnknown";

		try {

			Reporter.log("starting job");
			JobExecution jobExec = null;
			boolean seenException = false;
			try {
				jobExec = jobOp.startJobAndWaitForResult("job_executable_sequence_invalid", null);
			} catch (JobStartException e) {
				Reporter.log("Caught JobStartException:  " + e.getLocalizedMessage());
				seenException = true;
			}
			// If we caught an exception we'd expect that a JobExecution would not have been created,
			// though we won't validate that it wasn't created.  
			// If we didn't catch an exception that we require that the implementation fail the job execution.
			if (!seenException) {
				Reporter.log("Didn't catch JobStartException, Job Batch Status = " + jobExec.getBatchStatus());
				assertWithMessage("Job should have failed because of out of scope execution elements.", BatchStatus.FAILED, jobExec.getBatchStatus());
			}
			Reporter.log("job failed");
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	 @TCKTest(
				specRefs={
						@SpecRef(section="10.8",version="1.0", notes={""}),
						@SpecRef(section="8.9.3",version="1.0", notes={""})
				},
				apiRefs={
						@APIRef(className="com.ibm.jbatch.tck.artifacts.specialized.MyBatchletImpl")
				},
				versions="1.1.WORKING",
				assertions={"A step is transitioned to more than one in execution of a job"}
				)
	
	
	@Test
	@org.junit.Test
	public void testJobTransitionWithSimpleLoop() throws Exception {

		String METHOD = "testJobTransitionWithSimpleLoop";

		try {
			JobExecution jobExec = null;
			try{
				jobExec = jobOp.startJobAndWaitForResult("job_simple_loop", null);
			} catch (JobStartException e) {
				Reporter.log("Caught JobStartException:  " + e.getLocalizedMessage());
				return; //Since the job will be invalid when it loops, if the job fails to start because of that, the test should pass.
			}
			
			Reporter.log("Job execution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
			
		}
		catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	
	/**
	 * @testName: testJobTransitionLoop
	 * @assertion: Section 10.8 restart processing
	 * @test_Strategy: 1. setup a job consisting of 3 steps (step1 next to step2, step2 fail, restart @ step1, transition to step 2, back to step 1)
	 * 				   2. start job 
	 * 				   3. job should fail because it shouldn't be able to transition twice to step 1 in the same execution
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	 @TCKTest(
				specRefs={
						@SpecRef(section="10.8",version="1.0", notes={""}),
						@SpecRef(section="8.9.3",version="1.0", notes={""})
				},
				apiRefs={
						@APIRef(className="com.ibm.jbatch.tck.artifacts.specialized.ExecutionCountBatchlet")
				},
				versions="1.1.WORKING",
				assertions={"A step is not executed more than once on job restart if not set with \"allow-start-if-complete\"",
							"A step may be executed more than once on job restart if set with \"allow-start-if-complete\""},
				issueRefs={"Bugzilla 5691", "Github 31"})    
	
	@Test
	@org.junit.Test
	public void testJobTransitionLoopWithRestart() throws Exception {

		String METHOD = "testJobTransitionLoopWithRestart";

		try {
			JobExecution jobExec = null;
			try{
				Properties jobParams = new Properties();
				jobParams.put("executionCount.number", "1");
				jobExec = jobOp.startJobAndWaitForResult("job_restart_second_transition", null);
			} catch (JobStartException e) {
				Reporter.log("Caught JobStartException:  " + e.getLocalizedMessage());
				return; //Since the job will be invalid when it loops, if the job fails to start because of that, the test should pass.
			}
			Reporter.log("First Job execution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
			
			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution> steps = jobOp.getStepExecutions(jobExec.getExecutionId());

			assertObjEquals(2, steps.size());

			for (StepExecution step : steps) {
				showStepState(step);
				assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			}
			
			JobExecution restartedJobExec =null;
			try{
				 Properties jobParams = new Properties();
				 jobParams.put("executionCount.number", "2");
				restartedJobExec = jobOp.restartJobAndWaitForResult(jobExec.getExecutionId(),jobParams);
			} catch(JobStartException e) {
				Reporter.log("Caught JobStartException:  " + e.getLocalizedMessage());
				return;
			}
			
			Reporter.log("Obtaining StepExecutions for execution id: " + restartedJobExec.getExecutionId() + "<p>");
			List<StepExecution> steps2 = jobOp.getStepExecutions(restartedJobExec.getExecutionId());
			
			final String message="Number of steps in this execution: ";

			assertObjEquals(message+"1", message+steps2.size());
			StepExecution step=steps2.get(0);
			showStepState(step);
			assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			assertObjEquals("step2", step.getStepName());
			
			Reporter.log("Second Job execution getExitStatus()="+restartedJobExec.getExitStatus()+"<p>");
			assertObjEquals("Exited on execution 2 of ExecutionCountBatchlet", restartedJobExec.getExitStatus());
			
			Reporter.log("Second Job execution getBatchStatus()="+restartedJobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED, restartedJobExec.getBatchStatus());

		} catch (Exception e) {
			handleException(METHOD, e);
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
	public void  cleanup()
	{		

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
	
	private void showStepState(StepExecution step) {
		Reporter.log("---------------------------<p>");
		Reporter.log("getStepName(): " + step.getStepName() + " - ");
		Reporter.log("getStepExecutionId(): " + step.getStepExecutionId() + " - ");
		Metric[] metrics = step.getMetrics();

		for (int i = 0; i < metrics.length; i++) {
			Reporter.log(metrics[i].getType() + ": " + metrics[i].getValue() + " - ");
		}

		Reporter.log("getStartTime(): " + step.getStartTime() + " - ");
		Reporter.log("getEndTime(): " + step.getEndTime() + " - ");
		Reporter.log("getBatchStatus(): " + step.getBatchStatus() + " - ");
		Reporter.log("getExitStatus(): " + step.getExitStatus()+"<p>");
		Reporter.log("---------------------------<p>");
	}
}

