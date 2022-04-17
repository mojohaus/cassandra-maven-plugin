package org.codehaus.mojo.cassandra;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractCqlExecMojoTest {

    @Test
    public void splitStatementsUsingCqlLexerWithSemicolonTest() {
        assertThat(AbstractCqlExecMojo.splitStatementsUsingCqlLexer(
                "SELECT * FROM tbl1 WHERE col1 = 'abc1;def1';SELECT * FROM tbl2 WHERE col2 = 'abc2;def2';"))
                .containsExactly("SELECT * FROM tbl1 WHERE col1 = 'abc1;def1';",
                        "SELECT * FROM tbl2 WHERE col2 = 'abc2;def2';");
    }

    @Test
    public void splitStatementsUsingCqlLexerWithCommentTest() {
        assertThat(AbstractCqlExecMojo.splitStatementsUsingCqlLexer(
                "--\nSELECT * FROM tbl1 WHERE col1 = '--';"))
                .hasSize(1)
                .containsExactly("SELECT * FROM tbl1 WHERE col1 = '--';");
    }

    @Test
    public void splitStatementsUsingCqlLexerWithEmptyInput() {
        assertThat(AbstractCqlExecMojo.splitStatementsUsingCqlLexer(""))
                .hasSize(0);

        assertThat(AbstractCqlExecMojo.splitStatementsUsingCqlLexer(";;;"))
                .hasSize(3)
                .containsExactly(";", ";", ";");
    }

}