/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openimaj.text.nlp.textpipe.annotators;

import java.util.List;

import org.openimaj.text.nlp.textpipe.annotations.AnnotationUtils;
import org.openimaj.text.nlp.textpipe.annotations.POSAnnotation;
import org.openimaj.text.nlp.textpipe.annotations.RawTextAnnotation;
import org.openimaj.text.nlp.textpipe.annotations.SentenceAnnotation;
import org.openimaj.text.nlp.textpipe.annotations.TokenAnnotation;

public abstract class AbstractPhraseAnnotator extends
		AbstractTextPipeAnnotator<RawTextAnnotation> {

	@Override
	public void performAnnotation(RawTextAnnotation annotation)
			throws MissingRequiredAnnotationException {
		if (!annotation.getAnnotationKeyList().contains(
				SentenceAnnotation.class))
			throw new MissingRequiredAnnotationException(
					"No SentenceAnnotation found");
		for (SentenceAnnotation sentence : annotation
				.getAnnotationsFor(SentenceAnnotation.class)) {
			if (sentence.getAnnotationKeyList().contains(TokenAnnotation.class)) {
				if (AnnotationUtils.allHaveAnnotation(
						sentence.getAnnotationsFor(TokenAnnotation.class),
						POSAnnotation.class)) {
					phraseChunk(sentence
							.getAnnotationsFor(TokenAnnotation.class));
				} else
					throw new MissingRequiredAnnotationException(
							"No POSAnnotation found on token");
			} else
				throw new MissingRequiredAnnotationException(
						"UnTokenized sentence found");
		}
	}

	protected abstract void phraseChunk(List<TokenAnnotation> tokens);

}
