package org.codehaus.mojo.cassandra;

import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractCqlExecMojoTest {

    @Test
    public void splitStatementsUsingCqlLexerWithSemicolonTest() {
        splitStatementsUsingCqlLexerTest("SELECT * FROM tbl1 WHERE col1 = 'abc1;def1';SELECT * FROM tbl2 WHERE col2 = 'abc2;def2';", "SELECT * FROM tbl1 WHERE col1 = 'abc1;def1';", "SELECT * FROM tbl2 WHERE col2 = 'abc2;def2';");
    }

    @Test
    public void splitStatementsUsingCqlLexerWithCommentTest() {
        splitStatementsUsingCqlLexerTest("--\nSELECT * FROM tbl1 WHERE col1 = '--';", "SELECT * FROM tbl1 WHERE col1 = '--';");
    }

    @Test
    public void splitStatementsUsingCqlLexerWithEmptyInput() {
        splitStatementsUsingCqlLexerTest("");
        splitStatementsUsingCqlLexerTest(";;;", ";", ";", ";");
    }

    public void splitStatementsUsingCqlLexerTest(String input, String...expected) {
        String[] split = AbstractCqlExecMojo.splitStatementsUsingCqlLexer(input);
        assertEquals("Split does not match expected number of statements", expected.length, split.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], split[i]);
        }
    }
}