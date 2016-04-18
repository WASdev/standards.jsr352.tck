/**
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
package com.ibm.jbatch.tck.artifacts.inner;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;


public class PartitionRerunArtifacts {
	
	//Reader that force fails when needed
	@javax.inject.Named("PartitionRerunArtifacts.R")
	public static class Reader extends AbstractItemReader {

		@Inject
		@BatchProperty(name="force.failure")
		String forceFailure;

		@Inject
		@BatchProperty(name="partition.num")
		String partitionNum;
		
		@Inject StepContext stepCtx;

		@Override
		public Object readItem() {
			if (Boolean.parseBoolean(forceFailure) == true && partitionNum.charAt(0) == '1') {//if force failure is true and partition 1
				throw new RuntimeException("Forcing failure for step: " + stepCtx.getStepName());
			}
			return null;
		}
	}

	//dummy writer
	@javax.inject.Named("PartitionRerunArtifacts.W")
	public static class Writer extends AbstractItemWriter {
		@Override
		public void writeItems(List<Object> items) {
		}
	}

	//Collects all execution ids from partitions
	@javax.inject.Named("PartitionRerunArtifacts.C")
	public static class Collector implements PartitionCollector {
		//@Inject JobContext jobCtx;
		@Inject StepContext stepCtx;
		
		@Override
		public Serializable collectPartitionData() throws Exception {
			return stepCtx.getStepExecutionId();
		}
	}

	//Checks ids to make sure they are the same in the same run of the job
	@javax.inject.Named("PartitionRerunArtifacts.A")
	public static class Analyzer extends AbstractPartitionAnalyzer {
		@Inject JobContext jobCtx;

		@Override
		public void analyzeCollectorData(Serializable data) throws Exception {
			if(jobCtx.getExitStatus() == null)
				jobCtx.setExitStatus(data.toString()+",");
			else
				jobCtx.setExitStatus(jobCtx.getExitStatus()+data.toString()+",");
		}
	}

	//Simple batchlet that fails when forceFailure2 flag is set
	@javax.inject.Named("PartitionRerunArtifacts.B")
	public static class Batchlet extends AbstractBatchlet {
		@Inject
		@BatchProperty(name="force.failure2")
		String forceFailure2;
		
		@Inject StepContext stepCtx;
		
		@Override
		public String process() {
			if (Boolean.parseBoolean(forceFailure2) == true) {
				throw new RuntimeException("Forcing failure for step2: " + stepCtx.getStepName());
			}
			return "true";
		}
	}
}