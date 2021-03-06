package org.codehaus.jparsec.examples.java.parser;

import static org.codehaus.jparsec.examples.java.parser.TerminalParser.parse;
import junit.framework.Assert;
import junit.framework.TestCase;

import org.codehaus.jparsec.Parser;
import org.codehaus.jparsec.Token;
import org.codehaus.jparsec.Tokens;
import org.codehaus.jparsec.error.ParserException;
import org.codehaus.jparsec.examples.java.ast.expression.DecimalPointNumberLiteral;
import org.codehaus.jparsec.examples.java.ast.expression.IntegerLiteral;
import org.codehaus.jparsec.examples.java.ast.expression.NumberType;
import org.codehaus.jparsec.examples.java.ast.expression.ScientificNumberLiteral;
import org.codehaus.jparsec.examples.java.ast.expression.IntegerLiteral.Radix;

/**
 * Unit test for {@link TerminalParser}.
 * 
 * @author Ben Yu
 */
public class TerminalParserTest extends TestCase {
  
  public void testParse() {
    assertParser(TerminalParser.term("."),
        "  . /** javadoc */ /* regular doc */ \n // line comment",
        new Token(2, 1, Tokens.reserved(".")));
  }
  
  public void testAdjacent() {
    assertOperator(TerminalParser.adjacent(""), "");
    assertOperator(TerminalParser.adjacent("<"), "<");
    assertOperator(TerminalParser.adjacent(">>"), ">>");
    assertOperator(TerminalParser.adjacent(">>>"), ">>>");
    assertOperator(TerminalParser.adjacent("<<"), "<<");
    assertOperator(TerminalParser.adjacent("<+>"), "<+>");
    assertFailure(TerminalParser.adjacent(">>"), "> >", 1, 4);
    assertFailure(TerminalParser.adjacent(">>"), ">+", 1, 2);
    assertParser(TerminalParser.adjacent(">>").optional(), ">+", null, ">+");
    assertOperator(TerminalParser.adjacent(">>").or(TerminalParser.adjacent(">+")), ">+");
  }
  
  public void testTerm() {
    assertOperator(TerminalParser.term("<<"), "<<");
    assertOperator(TerminalParser.term(">>"), ">>");
    assertOperator(TerminalParser.term(">>>"), ">>>");
    assertOperator(TerminalParser.term("||"), "||");
    assertOperator(TerminalParser.term(">"), ">");
    TerminalParser.parse(TerminalParser.term(">>").followedBy(TerminalParser.term(">")), ">> >");
    assertFailure(TerminalParser.term(">>").followedBy(TerminalParser.term(">")), ">>>", 1, 1);
    try {
      TerminalParser.term("not exist");
      fail();
    } catch (IllegalArgumentException e) {}
  }
  
  public void testLexer() {
    Parser<?> parser = TerminalParser.TOKENIZER;
    assertEquals(new ScientificNumberLiteral("1e2", NumberType.DOUBLE), parser.parse("1e2"));
    assertEquals(new ScientificNumberLiteral("1e2", NumberType.FLOAT), parser.parse("1e2f"));
    assertEquals("foo", parser.parse("\"foo\""));
    assertEquals('a', parser.parse("'a'"));
    assertEquals(Tokens.reserved("import"), parser.parse("import"));
    assertEquals(new DecimalPointNumberLiteral("1.2", NumberType.DOUBLE), parser.parse("1.2"));
    assertEquals(new IntegerLiteral(Radix.DEC, "1", NumberType.INT), parser.parse("1"));
    assertEquals(new IntegerLiteral(Radix.HEX, "1", NumberType.LONG), parser.parse("0X1L"));
    assertEquals(new IntegerLiteral(Radix.OCT, "1", NumberType.DOUBLE), parser.parse("01D"));
  }
  
  static void assertParser(Parser<?> parser, String source, Object value) {
    assertEquals(value, TerminalParser.parse(parser, source));
  }
  
  static void assertParser(Parser<?> parser, String source, Object value, String rest) {
    assertTrue(source.endsWith(rest));
    assertEquals(value,
        TerminalParser.parse(parser, source.substring(0, source.length() - rest.length())));
  }
  
  static void assertOperator(Parser<?> parser, String source) {
    Token actual = (Token) TerminalParser.parse(parser, source);
    assertEquals(0, actual.index());
    assertEquals(source.length(), actual.length());
    // TODO: do we make adjacent() call Tokens.reserved()?
    // That seems verbose unless we make Tokenizers public.
    assertEquals(source, actual.value().toString());
  }
  
  static <T> void assertResult(
      Parser<T> parser, String source, Class<? extends T> expectedType, String expectedResult) {
    assertToString(expectedType, expectedResult, parse(parser, source));
  }

  static <T> void assertToString(
      Class<? extends T> expectedType, String expectedResult, T result) {
    Assert.assertTrue(expectedType.isInstance(result));
    Assert.assertEquals(expectedResult, result.toString());
  }
  
  static void assertFailure(Parser<?> parser, String source, int line, int column) {
    assertFailure(parser, source, line, column, "");
  }
  
  static void assertFailure(
      Parser<?> parser, String source, int line, int column, String errorMessage) {
    try {
      TerminalParser.parse(parser, source);
      Assert.fail();
    } catch (ParserException e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains(errorMessage));
      Assert.assertEquals(line, e.getLocation().line);
      Assert.assertEquals(column, e.getLocation().column);
    }
  }
}
