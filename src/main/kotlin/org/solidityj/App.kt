package org.solidityj

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.TokenStreamRewriter



fun main(args: Array<String>) {
    val input  = ANTLRInputStream("""
contract StandardToken is ERC20 {

  function transfer(address _to, uint _value) {
    msg[_to] = 2;
  }
}
    """)

    val lexer  = SolidityLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = SolidityParser(tokens)
    val builder = ASTBuilder()

    val ast = builder.visit(parser.sourceUnit())
    println(ast)
}