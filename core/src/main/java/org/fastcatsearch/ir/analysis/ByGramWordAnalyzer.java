package org.fastcatsearch.ir.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;

/**
 * 1-gram, 2-gram과 3-gram으로 뽑아낸다.
 * */
public class ByGramWordAnalyzer extends Analyzer {

	private static final Logger logger = LoggerFactory.getLogger(ByGramWordAnalyzer.class);

	public ByGramWordAnalyzer() {
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {

		final NGramWordTokenizer tokenizer = new NGramWordTokenizer(reader, 2, 2, true);

		TokenFilter filter = new StandardFilter(tokenizer);

		return new TokenStreamComponents(tokenizer, filter);
	}
}