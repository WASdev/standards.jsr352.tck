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

import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.ann.*;
import com.ibm.jbatch.tck.artifacts.specialized.ExitStatusBatchlet;
import com.ibm.jbatch.tck.artifacts.specialized.StepContextExitStatusListener;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExitStatusTests {

	private static JobOperatorBridge jobOp = null;
    public static void setup(String[] args, Properties props) throws Exception {
        String METHOD = "setup";
        try {
            jobOp = new JobOperatorBridge();
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    @BeforeMethod
    @BeforeClass
    public static void setUp() throws Exception {
        jobOp = new JobOperatorBridge();
    }
    
    @TCKTest(
        versions = {"1.0"},
        assertions = {"If the return value of a batchlet's process() method is not null, then the step exit status is overwritten."},
        specRefs = {
           	@SpecRef(version = "1.0", section = "8.7.1"),
           	@SpecRef(version = "1.0", section = "9.1.2", notes = "API for javax.batch.api.Batchlet")
        },
        apiRefs = {
        	@APIRef(className = "javax.batch.api.Batchlet", methodNames = "process")
        },
        strategy = "Have the batchlet set the step ES via stepCtx.setExitStatus(). Also have the batchlet's process() method return a non-null value. "
        		 + "Verify that the final ES of the step is the one set by the process() return value.",
        notes = "The assertion is not specified by the spec."
    )
    @Test
    @org.junit.Test
    public void testBatchletNonNullReturnValueOverWritesExitStatus() throws Exception {
        String METHOD = "testBatchletNonNullReturnValueOverWritesExitStatus";
        
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("set.exit.status=SET_EXIT_STATUS<p>");
            Reporter.log("process.return.value=BATCHLET_RETURN_VALUE<p>");
            jobParams.put("set.exit.status", ExitStatusBatchlet.SET_EXIT_STATUS);
            jobParams.put("process.return.value", ExitStatusBatchlet.PROCESS_RETURN_VALUE);

            Reporter.log("Locate job XML file: stepContextExitStatusTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("stepContextExitStatusTest", jobParams);
            String stepExitStatus = jobOp.getStepExecutions(execution1.getExecutionId()).get(0).getExitStatus();

            assertWithMessage(null, ExitStatusBatchlet.PROCESS_RETURN_VALUE, stepExitStatus);
            assertWithMessage("Expect Job Execution to be COMPLETED", BatchStatus.COMPLETED, execution1.getBatchStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    @TCKTest(
        versions = {"1.0"},
        assertions = {"If the return value of a batchlet's process() method is null, then the step exit status is left alone."},
        specRefs = {
           	@SpecRef(version = "1.0", section = "8.7.1"),
           	@SpecRef(version = "1.0", section = "9.1.2", notes = "API for javax.batch.api.Batchlet")
        },
        apiRefs = {
            @APIRef(className = "javax.batch.api.Batchlet", methodNames = "process")
        },
        strategy = "Have the batchlet set the step ES via stepCtx.setExitStatus(). Have the batchlet's process() method return null. "
                 + "Verify that the final ES of the step is the one set by setExitStatus().",
        notes = "The assertion is not specified by the spec."
    )
    @Test
    @org.junit.Test
    public void testBatchletNullReturnValueLeavesExitStatusAlone() throws Exception {
        String METHOD = "testBatchletNullReturnValueLeavesExitStatusAlone";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("set.exit.status=SET_EXIT_STATUS<p>");
            Reporter.log("process.return.value=PROCESS_RETURN_VALUE_NULL<p>");
            jobParams.put("set.exit.status", ExitStatusBatchlet.SET_EXIT_STATUS);
            jobParams.put("process.return.value", ExitStatusBatchlet.PROCESS_RETURN_VALUE_NULL);

            Reporter.log("Locate job XML file: stepContextExitStatusTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("stepContextExitStatusTest", jobParams);
            String stepExitStatus = jobOp.getStepExecutions(execution1.getExecutionId()).get(0).getExitStatus();

            assertWithMessage(null, ExitStatusBatchlet.SET_EXIT_STATUS, stepExitStatus);
            assertWithMessage("Expect Job Execution to be COMPLETED", BatchStatus.COMPLETED, execution1.getBatchStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
    
    @TCKTest(
        versions = {"1.0"},
        assertions = {"A StepListener artifact can obtain the user-defined exit status for the step via a call to StepContext.getExitStatus()"},
        specRefs = {
           	@SpecRef(version = "1.0", section = "9.2.2", citations = "StepListener intercepts step execution", notes = "API for javax.batch.api.listener.StepListener"),
           	@SpecRef(version = "1.0", section = "10.9.2", notes = "API for javax.batch.runtime.context.StepContext"),
        },
        apiRefs = {
            @APIRef(className = "javax.batch.api.listener.StepListener", methodNames = "afterStep()"),
            @APIRef(className = "javax.batch.runtime.context.StepContext", methodNames = "getExitStatus()"),
        },
        strategy = "Have the batchlet set the step ES via stepCtx.setExitStatus(). Verify that a call to StepContext.getExitStatus() from within "
        		+ "the scope of StepListener.afterStep() returns the correct value."
    )
    @Test
    @org.junit.Test
    public void testNonNullExitStatusIsSeenByAfterStep() throws Exception {
        String METHOD = "testNonNullExitStatusIsSeenByAfterStep";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("set.exit.status=SET_EXIT_STATUS<p>");
            Reporter.log("process.return.value=PROCESS_RETURN_VALUE_NULL<p>");
            Reporter.log("expected.exit.status=SET_EXIT_STATUS<p>");
            jobParams.put("set.exit.status", ExitStatusBatchlet.SET_EXIT_STATUS);
            jobParams.put("process.return.value", ExitStatusBatchlet.PROCESS_RETURN_VALUE_NULL);
            jobParams.put("expected.exit.status", ExitStatusBatchlet.SET_EXIT_STATUS);

            Reporter.log("Locate job XML file: stepContextExitStatusAfterStepTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("stepContextExitStatusAfterStepTest", jobParams);
            String stepExitStatus = jobOp.getStepExecutions(execution1.getExecutionId()).get(0).getExitStatus();

            assertWithMessage(null, ExitStatusBatchlet.SET_EXIT_STATUS, stepExitStatus);
            assertWithMessage("Expect Job Execution to be COMPLETED", BatchStatus.COMPLETED, execution1.getBatchStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
    
    @TCKTest(
        versions = {"1.0"},
        assertions = {"A StepListener artifact can obtain the user-defined exit status for the step via a call to StepContext.getExitStatus(). In the case where no exit "
        		+ "status has been set, a step's exit status does not default to its batch status until after StepListener.afterStep() has been invoked."},
        specRefs = {
        	@SpecRef(version = "1.0", section = "8.7.1", 
        		citations = "If no batch artifact sets the [step] exit status, the batch runtime will default the value to the string form of the batch status value.", 
        		notes = "Since StepListener is a batch artifact configured for the step, then it is able to set the step exit status via a call to setExitStatus(), even "
        				+ "from the scope of afterStep(). Therefore, the batch runtime won't default a step's exit status until after the StepListener completes execution."),
           	@SpecRef(version = "1.0", section = "9.2.2", notes = "API for javax.batch.api.listener.StepListener"),
           	@SpecRef(version = "1.0", section = "10.9.2", notes = "API for javax.batch.runtime.context.StepContext"),
        },
        apiRefs = {
            @APIRef(className = "javax.batch.api.listener.StepListener", methodNames = "afterStep()"),
            @APIRef(className = "javax.batch.runtime.context.StepContext", methodNames = "getExitStatus()"),
        },
        strategy = "Do not set an exit status for a step. Verify that a call to StepContext.getExitStatus() from within the scope of StepListener.afterStep() returns "
        		+ "null (and not a batch status)."
    )
    @Test
    @org.junit.Test
    public void testNullExitStatusIsSeenByAfterStep() throws Exception {
        String METHOD = "testNullExitStatusIsSeenByAfterStep";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("set.exit.status=DO_NOT_SET_EXIT_STATUS<p>");
            Reporter.log("process.return.value=PROCESS_RETURN_VALUE_NULL<p>");
            Reporter.log("expected.exit.status=NULL_EXIT_STATUS<p>");
            jobParams.put("set.exit.status", ExitStatusBatchlet.DO_NOT_SET_EXIT_STATUS);
            jobParams.put("process.return.value", ExitStatusBatchlet.PROCESS_RETURN_VALUE_NULL);
            jobParams.put("expected.exit.status", StepContextExitStatusListener.NULL_EXIT_STATUS);

            Reporter.log("Locate job XML file: stepContextExitStatusAfterStepTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("stepContextExitStatusAfterStepTest", jobParams);
            String stepExitStatus = jobOp.getStepExecutions(execution1.getExecutionId()).get(0).getExitStatus();

            //First assertion: by the time control is passed back to the Job Operator, the exit status should have defaulted from null to a COMPLETED batch status
            assertWithMessage(null, BatchStatus.COMPLETED.toString(), stepExitStatus);
            //Second assertion: If the exit status at the time of afterStep() is not null as expected, the Step Listener will throw an exception which causes the job to fail
            assertWithMessage("Expect Job Execution to be COMPLETED", BatchStatus.COMPLETED, execution1.getBatchStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
    
    private static void handleException(String methodName, Exception e) throws Exception {
        Reporter.log("Caught exception: " + e.getMessage() + "<p>");
        Reporter.log(methodName + " failed<p>");
        throw e;
    }
}
