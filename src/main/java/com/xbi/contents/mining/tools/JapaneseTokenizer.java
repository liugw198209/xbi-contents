/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.xbi.contents.mining.tools;

import org.atilika.kuromoji.Token;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default tokenizer
 * @author Guangwen Liu
 */
public class JapaneseTokenizer implements Tokenizer {
	List<Token> tokens = null;
	protected AtomicInteger position = new AtomicInteger(0);

	public JapaneseTokenizer(String text) {
		tokens = tokenizer.tokenize(text);
	}
	
	private static org.atilika.kuromoji.Tokenizer tokenizer = org.atilika.kuromoji.Tokenizer.builder().build();
	private TokenPreProcess tokenPreProcess;
	
	@Override
	public boolean hasMoreTokens() {
		return position.get() < tokens.size();
	}

	@Override
	public int countTokens() {
		return tokens.size();
	}

	@Override
	public String nextToken() {
		Token token = tokens.get(position.getAndIncrement());
		String base = token.getSurfaceForm();
        if(tokenPreProcess != null)
            base = tokenPreProcess.preProcess(base);
        return base;
	}

	@Override
	public List<String> getTokens() {
		List<String> tokens = new ArrayList<>();
		while(hasMoreTokens()) {
			tokens.add(nextToken());
		}
		return tokens;
	}

	@Override
	public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
		this.tokenPreProcess = tokenPreProcessor;
		
	}
	
	

	
}
