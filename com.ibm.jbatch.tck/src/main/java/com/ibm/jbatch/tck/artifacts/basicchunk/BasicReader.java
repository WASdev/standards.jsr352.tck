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
package com.ibm.jbatch.tck.artifacts.basicchunk;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Inject;

@javax.inject.Named("basicReader")
public class BasicReader extends AbstractItemReader {

	@Inject    
    @BatchProperty(name = "number.of.items.to.be.read")
    String injectedNumberOfItemsToBeRead;
	//Default: read 10 items
    private int numberOfItemsToBeRead = 10;
	
	@Inject
    @BatchProperty(name = "throw.reader.exception.for.these.items")
    String injectedThrowReaderExceptionForTheseItems;
	//Default: don't throw any exceptions
    private int[] throwReaderExceptionForTheseItems = {};

    private int currentItemId = -1;
    private BasicItem currentItem = null;
    
    @Override
    public void open(Serializable checkpoint) {

    	if (injectedNumberOfItemsToBeRead != null) {
        	numberOfItemsToBeRead = Integer.parseInt(injectedNumberOfItemsToBeRead);
        }
    	
        if (injectedThrowReaderExceptionForTheseItems != null) {
            String[] exceptionsStringArray = injectedThrowReaderExceptionForTheseItems.split(",");
            throwReaderExceptionForTheseItems = new int[exceptionsStringArray.length];
            for (int i = 0; i < exceptionsStringArray.length; i++) {
            	throwReaderExceptionForTheseItems[i] = Integer.parseInt(exceptionsStringArray[i]);
            }
        }        
    }

    @Override
    public BasicItem readItem() throws Exception {
    	currentItemId++;
    	
    	if (currentItemId < numberOfItemsToBeRead) {
    		currentItem = new BasicItem(currentItemId);
            if (readerExceptionShouldBeThrownForCurrentItem()) {
            	throw new BasicReaderException("BasicReaderException thrown for item " + currentItem.getId());
            }        	
        	currentItem.setRead(true);
        	return currentItem;
    	}
    	
    	return null;
    }

    private boolean readerExceptionShouldBeThrownForCurrentItem() {

    	for (int i: throwReaderExceptionForTheseItems) {
    		if (currentItem.getId()==i) { return true; }
    	}
    	
        return false;
    }
}
