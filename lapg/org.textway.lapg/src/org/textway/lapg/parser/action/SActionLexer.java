package org.textway.lapg.parser.action;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;

public abstract class SActionLexer {

	public static class LapgSymbol {
		public Object sym;
		public int lexem;
		public int state;
		public int offset;
	}

	public interface Lexems {
		public static final int eoi = 0;
		public static final int LCURLY = 1;
		public static final int _skip = 2;
		public static final int RCURLY = 3;
	}

	public interface ErrorReporter {
		void error(int start, int line, String s);
	}

	public static final int TOKEN_SIZE = 2048;

	private Reader stream;
	final private ErrorReporter reporter;

	private char chr;

	private int group;

	final private StringBuilder token = new StringBuilder(TOKEN_SIZE);

	private int tokenLine = 1;
	private int currLine = 1;
	private int currOffset = 0;


	public SActionLexer(ErrorReporter reporter) throws IOException {
		this.reporter = reporter;
		reset();
	}

	public void reset() throws IOException {
		this.group = 0;
		chr = nextChar();
	}

	protected abstract char nextChar() throws IOException;

	protected void advance() throws IOException {
		if (chr == 0) return;
		currOffset++;
		if (chr == '\n') {
			currLine++;
		}
		token.append(chr);
		chr = nextChar();
	}

	public int getState() {
		return group;
	}

	public void setState(int state) {
		this.group = state;
	}

	public int getTokenLine() {
		return tokenLine;
	}

	public int getLine() {
		return currLine;
	}

	public void setLine(int currLine) {
		this.currLine = currLine;
	}

	public int getOffset() {
		return currOffset;
	}

	public void setOffset(int currOffset) {
		this.currOffset = currOffset;
	}

	public String current() {
		return token.toString();
	}

	private static final short lapg_char2no[] = {
		0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 6, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 7, 1, 1
	};

	private static final short lapg_lexemnum[] = {
		1, 2, 2, 2, 3
	};

	private static final short[][] lapg_lexem = new short[][] {
		{ -2, 1, 2, 3, 1, 1, 4, 5},
		{ -6, 1, -6, -6, 1, 1, -6, -6},
		{ -3, -3, -3, -3, -3, -3, -3, -3},
		{ -1, 3, 3, 6, 7, -1, 3, 3},
		{ -1, 4, 4, 4, 8, -1, 9, 4},
		{ -7, -7, -7, -7, -7, -7, -7, -7},
		{ -4, -4, -4, -4, -4, -4, -4, -4},
		{ -1, 3, 3, 3, 3, -1, 3, 3},
		{ -1, 4, 4, 4, 4, -1, 4, 4},
		{ -5, -5, -5, -5, -5, -5, -5, -5}
	};

	private static int mapCharacter(int chr) {
		if (chr >= 0 && chr < 128) {
			return lapg_char2no[chr];
		}
		return 1;
	}

	public LapgSymbol next() throws IOException {
		LapgSymbol lapg_n = new LapgSymbol();
		int state;

		do {
			lapg_n.offset = currOffset;
			tokenLine = currLine;
			if (token.length() > TOKEN_SIZE) {
				token.setLength(TOKEN_SIZE);
				token.trimToSize();
			}
			token.setLength(0);

			for (state = group; state >= 0;) {
				state = lapg_lexem[state][mapCharacter(chr)];
				if (state == -1 && chr == 0) {
					lapg_n.lexem = 0;
					lapg_n.sym = null;
					reporter.error(lapg_n.offset, this.getTokenLine(), "Unexpected end of input reached");
					return lapg_n;
				}
				if (state >= -1 && chr != 0) {
					currOffset++;
					if (chr == '\n') {
						currLine++;
					}
					token.append(chr);
					chr = nextChar();
				}
			}

			if (state == -1) {
				reporter.error(lapg_n.offset, this.getTokenLine(), MessageFormat.format("invalid lexem at line {0}: `{1}`, skipped", currLine, current()));
				lapg_n.lexem = -1;
				continue;
			}

			if (state == -2) {
				lapg_n.lexem = 0;
				lapg_n.sym = null;
				return lapg_n;
			}

			lapg_n.lexem = lapg_lexemnum[-state - 3];
			lapg_n.sym = null;

		} while (lapg_n.lexem == -1 || !createToken(lapg_n, -state - 3));
		return lapg_n;
	}

	protected boolean createToken(LapgSymbol lapg_n, int lexemIndex) throws IOException {
		switch (lexemIndex) {
			case 1:
				return false;
			case 2:
				return false;
			case 3:
				return false;
		}
		return true;
	}
}