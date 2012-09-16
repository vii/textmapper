/**
 * Copyright 2002-2012 Evgeny Gryaznov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.textmapper.lapg.parser;

import org.textmapper.lapg.api.*;
import org.textmapper.lapg.parser.ast.AstLexeme;

import java.util.Map;

public class LapgGrammar {

	private final Grammar grammar;
	private final TextSourceElement templates;
	private final boolean hasErrors;
	private final Map<String, Object> options;
	private final String copyrightHeader;

	private final Map<Symbol, String> identifierMap;
	private final Map<SourceElement, Map<String, Object>> annotationsMap;
	private final Map<SourceElement, TextSourceElement> codeMap;
	private final Map<Lexem, LapgLexemeTransitionSwitch> transitionMap;

	public LapgGrammar(Grammar grammar, TextSourceElement templates, boolean hasErrors, Map<String, Object> options,
					   String copyrightHeader, Map<Symbol, String> identifierMap,
					   Map<SourceElement, Map<String, Object>> annotationsMap, Map<SourceElement, TextSourceElement> codeMap,
					   Map<Lexem, LapgLexemeTransitionSwitch> transitionMap) {
		this.grammar = grammar;
		this.templates = templates;
		this.hasErrors = hasErrors;
		this.options = options;
		this.copyrightHeader = copyrightHeader;
		this.identifierMap = identifierMap;
		this.annotationsMap = annotationsMap;
		this.codeMap = codeMap;
		this.transitionMap = transitionMap;
	}

	public Grammar getGrammar() {
		return grammar;
	}

	public TextSourceElement getTemplates() {
		return templates;
	}

	public boolean hasErrors() {
		return hasErrors;
	}

	public Map<String, Object> getOptions() {
		return options;
	}

	public String getCopyrightHeader() {
		return copyrightHeader;
	}

	public Object getAnnotation(SourceElement element, String name) {
		Map<String, Object> annotations = annotationsMap.get(element);
		return annotations != null ? annotations.get(name) : null;
	}

	public TextSourceElement getCode(SourceElement element) {
		return codeMap.get(element);
	}
	
	public String getId(Symbol sym) {
		return identifierMap.get(sym);
	}

	public LapgLexemeTransitionSwitch getTransition(Lexem l) {
		return transitionMap.get(l);
	}
}