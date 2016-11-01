/**
 * Copyright 2002-2016 Evgeny Gryaznov
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
package org.textmapper.tool.compiler;

import org.textmapper.lapg.api.ProcessingStatus;
import org.textmapper.lapg.api.SourceElement;
import org.textmapper.tool.common.UniqueOrder;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class TMRangePhrase {
	final List<TMRangeField> fields;

	TMRangePhrase(TMRangeField... fields) {
		this.fields = Arrays.asList(fields);
	}

	TMRangePhrase(List<TMRangeField> fields) {
		this.fields = fields;
	}

	static TMRangePhrase empty() {
		return new TMRangePhrase();
	}

	static TMRangePhrase type(String rangeType) {
		return new TMRangePhrase(new TMRangeField(rangeType));
	}

	boolean isSingleElement() {
		return fields.size() == 1 && fields.get(0).isMergeable();
	}

	boolean isUnnamedField() {
		return fields.size() == 1 && !fields.get(0).hasExplicitName();
	}

	TMRangePhrase makeNullable() {
		if (fields.isEmpty()) return this;

		TMRangeField[] result = new TMRangeField[fields.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = fields.get(i).makeNullable();
		}
		return new TMRangePhrase(result);
	}

	TMRangePhrase makeList() {
		if (!isSingleElement()) throw new IllegalStateException();

		return new TMRangePhrase(fields.get(0).makeList());
	}

	static TMRangePhrase merge(String newName, List<TMRangePhrase> phrases,
							   SourceElement anchor,
							   ProcessingStatus status) {
		if (phrases.stream().allMatch(TMRangePhrase::isSingleElement)) {
			return new TMRangePhrase(TMRangeField.merge(newName,
					phrases.stream()
							.map(p -> p.fields.get(0))
							.toArray(TMRangeField[]::new)));
		}

		Map<String, List<TMRangeField>> bySignature = new LinkedHashMap<>();
		UniqueOrder<String> nameOrder = new UniqueOrder<>();

		Set<String> seen = new HashSet<>();
		for (TMRangePhrase p : phrases) {
			seen.clear();
			for (TMRangeField f : p.fields) {
				String signature = f.getSignature();
				if (!seen.add(signature)) {
					status.report(ProcessingStatus.KIND_ERROR,
							"two fields with the same signature `" + signature + "`", anchor);
					continue;
				}
				if (f.hasExplicitName()) {
					nameOrder.add(f.getName());
				}
				List<TMRangeField> list = bySignature.get(signature);
				if (list == null) {
					bySignature.put(signature, list = new ArrayList<>());
				}
				list.add(f);
			}
			nameOrder.flush();
		}

		if (nameOrder.getResult(String[]::new) == null) {
			status.report(ProcessingStatus.KIND_ERROR,
					"named elements must occur in the same order in all productions", anchor);
		}

		int ind = 0;
		TMRangeField[] result = new TMRangeField[bySignature.size()];
		for (Entry<String, List<TMRangeField>> entry : bySignature.entrySet()) {
			TMRangeField composite;
			List<TMRangeField> fields = entry.getValue();
			if (fields.size() == 1) {
				composite = fields.get(0);
			} else {
				composite = TMRangeField.merge(null,
						fields.toArray(new TMRangeField[fields.size()]));
				if (composite == null) {
					status.report(ProcessingStatus.KIND_ERROR,
							"Cannot merge " + fields.size() + " fields: " + entry.getKey(), anchor);
					continue;
				}
			}
			if (fields.size() < phrases.size()) {
				composite = composite.makeNullable();
			}
			result[ind++] = composite;
		}
		TMRangePhrase phrase = ind == result.length ? new TMRangePhrase(result) :
				TMRangePhrase.empty();
		return verify(phrase, anchor, status);
	}

	static TMRangePhrase concat(List<TMRangePhrase> phrases,
							    SourceElement anchor,
							    ProcessingStatus status) {
		List<TMRangeField> result = new ArrayList<>();

		Set<String> seen = new HashSet<>();
		for (TMRangePhrase p : phrases) {
			for (TMRangeField f : p.fields) {
				String signature = f.getSignature();
				if (!seen.add(signature)) {
					status.report(ProcessingStatus.KIND_ERROR,
							"two fields with the same signature `" + signature + "`", anchor);
					continue;
				}
				result.add(f);
			}
		}

		return verify(new TMRangePhrase(result), anchor, status);
	}

	private static TMRangePhrase verify(TMRangePhrase phrase,
							   SourceElement anchor,
							   ProcessingStatus status) {
		Set<String> namedTypes = new HashSet<>();
		for (TMRangeField field : phrase.fields) {
			if (field.hasExplicitName()) {
				namedTypes.addAll(Arrays.asList(field.getTypes()));
			}
		}
		Map<String, String> unnamedTypes = new HashMap<>();
		for (TMRangeField field : phrase.fields) {
			if (field.hasExplicitName()) continue;
			String signature = field.getSignature();

			for (String type : field.getTypes()) {
				if (namedTypes.contains(type)) {
					status.report(ProcessingStatus.KIND_ERROR,
							"`" + type + "` occurs in both named and unnamed fields", anchor);
					return TMRangePhrase.empty();
				}

				String prev = unnamedTypes.putIfAbsent(type, signature);
				if (prev != null && !prev.equals(signature)) {
					status.report(ProcessingStatus.KIND_ERROR,
							"two unnamed fields share the same type `" + type + "`", anchor);
					return TMRangePhrase.empty();
				}
			}
		}
		return phrase;
	}

	@Override
	public String toString() {
		return fields.stream()
				.map(TMRangeField::toString)
				.collect(Collectors.joining(" "));
	}
}