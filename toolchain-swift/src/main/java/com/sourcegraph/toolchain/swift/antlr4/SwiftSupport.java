package com.sourcegraph.toolchain.swift.antlr4;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;

import java.util.BitSet;

/**
 * Based on https://raw.githubusercontent.com/antlr/grammars-v4/5372ed3e71775f004494ab7ad1735c34aa48b51e/swift/SwiftLexerSupport.java
 */
public class SwiftSupport {
    /* TODO
	There is one caveat to the rules above. If the ! or ? predefined operator
	 has no whitespace on the left, it is treated as a postfix operator,
	 regardless of whether it has whitespace on the right. To use the ? as
	 the optional-chaining operator, it must not have whitespace on the left.
	  To use it in the ternary conditional (? :) operator, it must have
	  whitespace around both sides.
	*/

    /*
    operator-head : /  =  -  +  !  *  %  <  >  &  |  ^  ~  ?
      | [\u00A1-\u00A7]
      | [\u00A9\u00AB]
      | [\u00AC\u00AE]
      | [\u00B0-\u00B1\u00B6\u00BB\u00BF\u00D7\u00F7]
      | [\u2016-\u2017\u2020-\u2027]
      | [\u2030-\u203E]
      | [\u2041-\u2053]
      | [\u2055-\u205E]
      | [\u2190-\u23FF]
      | [\u2500-\u2775]
      | [\u2794-\u2BFF]
      | [\u2E00-\u2E7F]
      | [\u3001-\u3003]
      | [\u3008-\u3030]
      ;
     */
    public static final BitSet operatorHead = new BitSet(0x3100); // costs about 2k

    public static final BitSet leftWS = new BitSet(255);
    public static final BitSet rightWS = new BitSet(255);

    static {
        operatorHead.set(SwiftParser.BANG);
        operatorHead.set(SwiftParser.LT);
        operatorHead.set(SwiftParser.GT);
        operatorHead.set(SwiftParser.AND);
        operatorHead.set(SwiftParser.OR);
        operatorHead.set(SwiftParser.SUB);
        operatorHead.set(SwiftParser.ADD);
        operatorHead.set(SwiftParser.MUL);
        operatorHead.set(SwiftParser.DIV);
        operatorHead.set(SwiftParser.MOD);
        operatorHead.set(SwiftParser.EQUAL);
        operatorHead.set(SwiftParser.CARET);
        operatorHead.set(SwiftParser.TILDE);
        operatorHead.set(SwiftParser.QUESTION);
        operatorHead.set(0xA1, 0xA7 + 1);
        operatorHead.set(0xA9, 0xAB + 1);
        operatorHead.set(0xAC, 0xAE + 1);
        operatorHead.set(0xB0, 0xB1 + 1);
        operatorHead.set(0xB6);
        operatorHead.set(0xBB);
        operatorHead.set(0xBF);
        operatorHead.set(0xD7);
        operatorHead.set(0xF7);
        operatorHead.set(0x2016, 0x2017 + 1);
        operatorHead.set(0x2020, 0x2027 + 1);
        operatorHead.set(0x2030, 0x203E + 1);
        operatorHead.set(0x2041, 0x2053 + 1);
        operatorHead.set(0x2055, 0x205E + 1);
        operatorHead.set(0x2190, 0x23FF + 1);
        operatorHead.set(0x2500, 0x2775 + 1);
        operatorHead.set(0x2794, 0x2BFF + 1);
        operatorHead.set(0x2E00, 0x2E7F + 1);
        operatorHead.set(0x3001, 0x3003 + 1);
        operatorHead.set(0x3008, 0x3030 + 1);

        leftWS.set(SwiftParser.WS);
        leftWS.set(SwiftParser.LPAREN);
        leftWS.set(SwiftParser.LBRACK);
        leftWS.set(SwiftParser.LCURLY);
        leftWS.set(SwiftParser.COMMA);
        leftWS.set(SwiftParser.COLON);
        leftWS.set(SwiftParser.SEMI);

        rightWS.set(SwiftParser.WS);
        rightWS.set(SwiftParser.RPAREN);
        rightWS.set(SwiftParser.RBRACK);
        rightWS.set(SwiftParser.RCURLY);
        rightWS.set(SwiftParser.COMMA);
        rightWS.set(SwiftParser.COLON);
        rightWS.set(SwiftParser.SEMI);
        rightWS.set(SwiftParser.Line_comment);
        rightWS.set(SwiftParser.Block_comment);
    }

    public static boolean isOperatorHead(int ttype) {
        return ttype > 0 && operatorHead.get(ttype);
    }

    /*
    Operator_character
      : Operator_head
      | [\u0300-\u036F]
      | [\u1DC0-\u1DFF]
      | [\u20D0-\u20FF]
      | [\uFE00-\uFE0F]
      | [\uFE20-\uFE2F]
      //| [\uE0100-\uE01EF]  ANTLR can't do >16bit char
      ;
     */
    public static boolean isOperatorChar(int ttype) {
        return ttype > 0 && operatorHead.get(ttype) ||
                ttype >= 0x0300 && ttype <= 0x036F ||
                ttype >= 0x1DC0 && ttype <= 0x1DFF ||
                ttype >= 0x20D0 && ttype <= 0x20FF ||
                ttype >= 0xFE00 && ttype <= 0xFE0F ||
                ttype >= 0xFE20 && ttype <= 0xFE2F;
    }

    /**
     * Find stop token index of next operator; return -1 if not operator.
     */
    public static int getLastOpTokenIndex(TokenStream tokens) {
        int i = tokens.index(); // current on-channel lookahead token index
        Token lt = tokens.get(i);
        int size = tokens.size();
        if (i >= size - 1) {
            return -1;
        }

        if (lt.getType() == SwiftParser.DOT && tokens.get(i + 1).getType() == SwiftParser.DOT) {
            // dot-operator
            i += 2; // point at token after ".."
            if (i == size) {
                return -1;
            }
            lt = tokens.get(i);
            while (lt.getType() != Token.EOF &&
                    (lt.getType() == SwiftParser.DOT || isOperatorChar(lt.getType()))) {
                i++;
                if (i == size) {
                    return -1;
                }
                lt = tokens.get(i);
            }
            int stop = i - 1;
            return stop;
        }
        // Is it regular operator?
        if (!isOperatorHead(lt.getType())) {
            return -1;
        }
        i++;
        if (i == size) {
            return -1;
        }
        lt = tokens.get(i);
        while (lt.getType() != Token.EOF && isOperatorChar(lt.getType())) {
            i++;
            if (i == size) {
                return -1;
            }
            lt = tokens.get(i);
        }
        int stop = i - 1;
        return stop;
    }

    /**
     * "If an operator has whitespace around both sides or around neither side,
     * it is treated as a binary operator. As an example, the + operator in a+b
     * and a + b is treated as a binary operator."
     */
    public static boolean isBinaryOp(TokenStream tokens) {
        int stop = getLastOpTokenIndex(tokens);
        if (stop == -1) return false;

        int start = tokens.index();
        Token prevToken = tokens.get(start - 1); // includes hidden-channel tokens
        Token nextToken = tokens.get(stop + 1);
        boolean prevIsWS = isLeftOperatorWS(prevToken);
        boolean nextIsWS = isRightOperatorWS(nextToken);
        return prevIsWS && nextIsWS || (!prevIsWS && !nextIsWS);
    }

    /**
     * "If an operator has whitespace on the left side only, it is treated as a
     * prefix unary operator. As an example, the ++ operator in a ++b is treated
     * as a prefix unary operator."
     */
    public static boolean isPrefixOp(TokenStream tokens) {
        int stop = getLastOpTokenIndex(tokens);
        if (stop == -1) return false;

        int start = tokens.index();
        Token prevToken = tokens.get(start - 1); // includes hidden-channel tokens
        Token nextToken = tokens.get(stop + 1);
        boolean prevIsWS = isLeftOperatorWS(prevToken);
        boolean nextIsWS = isRightOperatorWS(nextToken);
        return prevIsWS && !nextIsWS;
    }

    /**
     * "If an operator has whitespace on the right side only, it is treated as a
     * postfix unary operator. As an example, the ++ operator in a++ b is treated
     * as a postfix unary operator."
     * <p>
     * "If an operator has no whitespace on the left but is followed immediately
     * by a dot (.), it is treated as a postfix unary operator. As an example,
     * the ++ operator in a++.b is treated as a postfix unary operator (a++ .b
     * rather than a ++ .b)."
     */
    public static boolean isPostfixOp(TokenStream tokens) {
        int stop = getLastOpTokenIndex(tokens);
        if (stop == -1) return false;

        int start = tokens.index();
        Token prevToken = tokens.get(start - 1); // includes hidden-channel tokens
        Token nextToken = tokens.get(stop + 1);
        boolean prevIsWS = isLeftOperatorWS(prevToken);
        boolean nextIsWS = isRightOperatorWS(nextToken);
        return
                !prevIsWS && nextIsWS ||
                        !prevIsWS && nextToken.getType() == SwiftParser.DOT;
    }

    public static boolean isOperator(TokenStream tokens, String op) {
        int stop = getLastOpTokenIndex(tokens);
        if (stop == -1) return false;

        int start = tokens.index();
        String text = tokens.getText(Interval.of(start, stop));
        return text.equals(op);
    }

    public static boolean isLeftOperatorWS(Token t) {
        int type = t.getType();
        return type != Token.EOF && leftWS.get(type);
    }

    public static boolean isRightOperatorWS(Token t) {
        int type = t.getType();
        return type == Token.EOF || rightWS.get(type);
    }
}