#  syntax: lalr1 generator source grammar
#
#  Lapg (Lexer and Parser Generator)
#  Copyright 2002-2010 Evgeny Gryaznov
# 
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

.lang        "java" 
.prefix      "Lapg"
.package	 "net.sf.lapg.parser"
.maxtoken    2048
.breaks		 "on"
.gentree	 "on"
.positions   "line,offset"
.endpositions "offset"

# Vocabulary

[0]

identifier(String): /[a-zA-Z_][a-zA-Z_0-9]*|'([^\n\\']|\\.)*'/
			{ $lexem = current(); break; }

regexp(String):	/\/([^\/\\\n]|\\.)*\//	{ $lexem = token.toString().substring(1, token.length()-1); break; }
scon(String):	/"([^\n\\"]|\\.)*"/		{ $lexem = unescape(current(), 1, token.length()-1); break; }
icon(Integer):	/-?[0-9]+/				{ $lexem = Integer.parseInt(current()); break; }

eoi:           /\n%%.*/					{ templatesStart = lapg_n.endoffset; break; }
'%':           /\n%/
_skip:         /\n|[\t\r ]+/    		{ return false; }
_skip_comment:  /#.*/					{ return !skipComments; }

'::=':  /::=/
'|':    /\|/
'=':	/=/
';':    /;/
'.':    /\./
',':	/,/
':':    /:/
'[':    /\[/
']':    /\]/
'(':	/\(/
')':	/\)/
'<<':   /<</
'<':	/</
'>':	/>/
'*':	/*/
'?':	/?/
'&':	/&/
'@':	/@/

'{':	/{/		{ deep = 1; group = 1; break; }

[1]

_skip:	/'([^\n\\']|\\.)*'/
_skip:	/"([^\n\\"]|\\.)*"/
_skip:	/[^'"{}]+/
'i{':	/{/				{ deep++; break; }
'}':	/}/				{ if( --deep == 0 ) group = 0; break; }

# Grammar

input (AstRoot) ::=
	optionsopt lexer_parts grammar_parts				{  $$ = new AstRoot($optionsopt, $lexer_parts, $grammar_parts, source, ${input.offset}, ${input.endoffset}); }  
;

options (List<AstOption>) ::= 
	  option											{ $$ = new ArrayList<AstOption>(16); $options.add($option); }  
	| list=options option								{ $list.add($option); } 
;

option (AstOption) ::=
	  '.' identifier expression 						{ $$ = new AstOption($identifier, $expression, source, ${option.offset}, ${option.endoffset}); }
;

symbol (AstIdentifier) ::=
	identifier											{ $$ = new AstIdentifier($identifier, source, ${symbol.offset}, ${symbol.endoffset}); } 
;

reference (AstReference) ::=
	identifier											{ $$ = new AstReference($identifier, source, ${reference.offset}, ${reference.endoffset}); } 
;

type (String) ::=
	  '(' scon ')'										{ $$ = $scon; }
	| '(' type_part_list ')'							{ $$ = source.getText(${self[0].offset}+1, ${self[2].endoffset}-1); }
;

type_part_list ::=
	type_part_list type_part | type_part ;

type_part ::=
	'<' | '>' | '[' | ']' | identifier | '*' | '.' | ',' | '?' | '@' | '&' | '(' type_part_listopt ')'
;

pattern (AstRegexp) ::=
	regexp												{ $$ = new AstRegexp($regexp, source, ${pattern.offset}, ${pattern.endoffset}); } 
;

lexer_parts (List<AstLexerPart>) ::= 
	  lexer_part 										{ $$ = new ArrayList<AstLexerPart>(64); $lexer_parts.add($lexer_part); }
	| list=lexer_parts lexer_part						{ $list.add($lexer_part); }
;

lexer_part (AstLexerPart) ::=
	  '[' icon_list ']'									{ $$ = new AstGroupsSelector($icon_list, source, ${lexer_part.offset}, ${lexer_part.endoffset}); }
	| symbol typeopt ':'								{ $$ = new AstLexeme($symbol, $typeopt, null, null, null, source, ${lexer_part.offset}, ${lexer_part.endoffset}); }
	| symbol typeopt ':' pattern iconopt commandopt		{ $$ = new AstLexeme($symbol, $typeopt, $pattern, $iconopt, $commandopt, source, ${lexer_part.offset}, ${lexer_part.endoffset}); }
;

icon_list (List<Integer>) ::=
	  icon 												{ $$ = new ArrayList<Integer>(4); $icon_list.add($icon); } 
	| list=icon_list icon  								{ $list.add($icon); }
;

grammar_parts (List<AstGrammarPart>) ::=
	  grammar_part 										{ $$ = new ArrayList<AstGrammarPart>(64); $grammar_parts.add($grammar_part); }
	| list=grammar_parts grammar_part					{ $list.add($grammar_part); }
;

grammar_part (AstGrammarPart) ::= 
	  symbol typeopt '::=' rules ';'					{ $$ = new AstNonTerm($symbol, $typeopt, $rules, null, source, ${grammar_part.offset}, ${grammar_part.endoffset}); }
	| annotations_decl symbol typeopt '::=' rules ';'	{ $$ = new AstNonTerm($symbol, $typeopt, $rules, $annotations_decl, source, ${grammar_part.offset}, ${grammar_part.endoffset}); }
	| '%' identifier references ';'						{ $$ = new AstDirective($identifier, $references, source, ${grammar_part.offset}, ${grammar_part.endoffset}); }
; 

references (List<AstReference>) ::= 
	  reference 										{ $$ = new ArrayList<AstReference>(); $references.add($reference); }
	| list=references reference							{ $list.add($reference); }
;

rules (List<AstRule>) ::= 
	  rule0												{ $$ = new ArrayList<AstRule>(); $rules.add($rule0); }
	| list=rules '|' rule0								{ $list.add($rule0); }
;

rule0 (AstRule) ::=
	  annotations_declopt rulesyms commandopt rule_priorityopt
														{ $$ = new AstRule($rulesyms, $commandopt, $rule_priorityopt, $annotations_declopt, source, ${rule0.offset}, ${rule0.endoffset}); }
	| annotations_declopt commandopt rule_priorityopt  	{ $$ = new AstRule(null, $commandopt, $rule_priorityopt, $annotations_declopt, source, ${rule0.offset}, ${rule0.endoffset}); }
;

rulesyms (List<AstRuleSymbol>) ::=
	  rulesym											{ $$ = new ArrayList<AstRuleSymbol>(); $rulesyms.add($rulesym); }
	| list=rulesyms rulesym 							{ $list.add($rulesym); }
;

rulesym (AstRuleSymbol) ::=
	  commandopt identifier '=' reference annotations_declopt	{ $$ = new AstRuleSymbol($commandopt, $identifier, $reference, $annotations_declopt, source, ${rulesym.offset}, ${rulesym.endoffset}); }
	| commandopt reference annotations_declopt					{ $$ = new AstRuleSymbol($commandopt, null, $reference, $annotations_declopt, source, ${rulesym.offset}, ${rulesym.endoffset}); }
;

annotations_decl (java.util.@Map<String,Object>) ::=
	'['	annotations ']'									{ $$ = $annotations; }
;

annotations (java.util.@Map<String,Object>) ::=
	  identifier 										{ $$ = new java.util.@HashMap<String,Object>(); $annotations.put($identifier, Boolean.TRUE); }
	| identifier ':' expression							{ $$ = new java.util.@HashMap<String,Object>(); $annotations.put($identifier, $expression); }
	| annotations ',' identifier ':' expression			{ $annotations#0.put($identifier, $expression); }
	| annotations ',' identifier						{ $annotations#0.put($identifier, Boolean.TRUE); }
;


map_entries (java.util.@Map<String,Object>) ::=
	  identifier ':' expression							{ $$ = new java.util.@HashMap<String,Object>(); $map_entries.put($identifier, $expression); }
	| map_entries ',' identifier ':' expression			{ $map_entries#0.put($identifier, $expression); }
;

expression ::=
	  scon
	| icon
	| reference
	| '[' map_entries ']'								{ $$ = $map_entries; }
	| '[' expression_list ']'							{ $$ = $expression_list; }
;

expression_list (List<Object>) ::=
	expression											{ $$ = new ArrayList(); $expression_list.add($expression); }
	| expression_list ',' expression					{ $expression_list#0.add($expression); }
;

rule_priority (AstReference) ::=
	'<<' reference										{ $$ = $reference; } 
;

command (AstCode) ::=
	'{' command_tokensopt '}'							{ $$ = new AstCode(source, ${self[0].offset}+1, ${self[2].endoffset}-1); }  
;

command_tokens ::=
	command_tokens command_token | command_token ;

command_token ::=
	'i{' command_tokensopt '}' 
;

##################################################################################

%%

${template java.imports-}
${call base-}
import java.util.List;
import java.util.ArrayList;
import net.sf.lapg.parser.ast.*;
${end}

${template java_lexer.lexercode}
private int deep = 0;
private int templatesStart = -1;
private boolean skipComments = true;

int getTemplatesStart() {
	return templatesStart;
}

public void setSkipComments(boolean skip) {
	this.skipComments = skip;
}

private String unescape(String s, int start, int end) {
	StringBuilder sb = new StringBuilder();
	end = Math.min(end, s.length());
	for(int i = start; i < end; i++) {
		char c = s.charAt(i);
		if(c == '\\') {
			if(++i == end) {
				break;
			}
			c = s.charAt(i);
			if(c == 'u' || c == 'x') {
				// FIXME process unicode
			} else if(c == 'n') {
				sb.append('\n');
			} else if(c == 'r') {
				sb.append('\r');
			} else if(c == 't') {
				sb.append('\t');
			} else {
				sb.append(c);
			}
		} else {
			sb.append(c);
		}
	} 
	return sb.toString();
}
${end}


${template java.classcode}
${call base-}
net.sf.lapg.parser.LapgTree.@TextSource source;
${end}

${template java_tree.createParser-}
${call base-}
parser.source = source;
${end}

${template java_tree.parseStatements-}
${call base-}
if(result != null) {
	result.setTemplatesStart(lexer.getTemplatesStart());
}
${end}