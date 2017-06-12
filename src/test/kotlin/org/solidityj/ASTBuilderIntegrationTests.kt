package org.solidityj

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Test

fun parseCode(string: String): ASTNode {
    val input = ANTLRInputStream(string)
    val lexer = SolidityLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = SolidityParser(tokens)
    val builder = ASTBuilder()

    return builder.visit(parser.sourceUnit())
}

class ASTBuilderIntegrationTests {

    @Test
    fun testParseToken() {
        val sourceCode = """
            contract StandardToken is ERC20, SafeMath {

                mapping(address => uint) balances;
                mapping (address => mapping (address => uint)) allowed;

                function transfer(address _to, uint _value) returns (bool success) {
                    balances[msg.sender] = safeSub(balances[msg.sender], _value);
                    balances[_to] = safeAdd(balances[_to], _value);
                    Transfer(msg.sender, _to, _value);
                    return true;
                }

                function transferFrom(address _from, address _to, uint _value) returns (bool success) {
                    var _allowance = allowed[_from][msg.sender];

                    // Check is not needed because safeSub(_allowance, _value) will already throw if this condition is not met
                    // if (_value > _allowance) throw;

                    balances[_to] = safeAdd(balances[_to], _value);
                    balances[_from] = safeSub(balances[_from], _value);
                    allowed[_from][msg.sender] = safeSub(_allowance, _value);
                    Transfer(_from, _to, _value);
                    return true;
                }

                function balanceOf(address _owner) constant returns (uint balance) {
                    return balances[_owner];
                }

                function approve(address _spender, uint _value) returns (bool success) {
                    allowed[msg.sender][_spender] = _value;
                    Approval(msg.sender, _spender, _value);
                    return true;
                }

                function allowance(address _owner, address _spender) constant returns (uint remaining) {
                    return allowed[_owner][_spender];
                }
            }
        """

        parseCode(sourceCode)

    }
}
