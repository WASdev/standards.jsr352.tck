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

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;


public class ListenerOnErrorTests {
	private static JobOperatorBridge jobOp = null;
	
	@BeforeMethod
	@BeforeClass
	public static void setup() throws Exception {
		jobOp = new JobOperatorBridge();
	}

    /*
     * @testName: testOnWriteErrorItems
     * 
     * @assertion: Ensure Job has failed and on Error ensure the writeListener returns the right items
     * 
     * @test_Strategy: Intentionally fail writer and ensure the onError method returns the correct string
     * 					of items
     */
	@Test
	@org.junit.Test
	public void testOnWriteErrorItems() throws Exception {
		String GOOD_EXIT_STATUS = new String("[10, 12, 14, 16, 18]");
		
	    Reporter.log("Create job parameters for execution<p>");
        Properties jobParams = new Properties();
		
	    Reporter.log("write.fail=true<p>");
	    jobParams.put("write.fail", "true");

	    Reporter.log("Invoke startJobAndWaitForResult for execution<p>");
	    JobExecution je = jobOp.startJobAndWaitForResult("listenerOnError", jobParams);
	
	    Reporter.log("JobExecution getBatchStatus()=" + je.getBatchStatus() + "<p>");
	    Reporter.log("JobExecution getExitStatus()=" + je.getExitStatus() + "<p>");
	    assertWithMessage("Testing execution for the WRITE LISTENER", BatchStatus.FAILED, je.getBatchStatus());
	    assertWithMessage("Testing execution for the WRITE LISTENER", GOOD_EXIT_STATUS, je.getExitStatus());
	}

    /*
     * @testName: testOnProcessErrorItems
     * 
     * @assertion: Ensure Job has failed and on Error ensure the procesListener returns the right items
     * 
     * @test_Strategy: Intentionally fail processor and ensure the onError method returns the correct string
     * 					of items
     */
	@Test
	@org.junit.Test
	public void testOnProcessErrorItems() throws Exception {
		String GOOD_EXIT_STATUS = new String("8");
        Reporter.log("Create job parameters for execution:<p>");
        Properties jobParams = new Properties();

        Reporter.log("process.fail=true<p>");
        jobParams.put("process.fail", "true");

        Reporter.log("Invoke startJobAndWaitForResult for execution<p>");
        JobExecution je = jobOp.startJobAndWaitForResult("listenerOnError", jobParams);

        Reporter.log("JobExecution getBatchStatus()=" + je.getBatchStatus() + "<p>");
        Reporter.log("JobExecution getExitStatus()=" + je.getExitStatus() + "<p>");
        assertWithMessage("Testing execution for the PROCESS LISTENER", BatchStatus.FAILED, je.getBatchStatus());
        assertWithMessage("Testing execution for the PROCESS LISTENER", GOOD_EXIT_STATUS, je.getExitStatus());
	}
}
