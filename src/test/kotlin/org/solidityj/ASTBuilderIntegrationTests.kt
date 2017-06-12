package org.solidityj

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

        var ast = parseCode(sourceCode)

        assertTrue(ast is SourceUnit)

        if (ast !is SourceUnit) { throw RuntimeException("should not reach here") }

        assertTrue(ast.nodes.size == 1)
        assertTrue(ast.nodes[0] is ContractDefinition)

        val contract = ast.nodes[0] as ContractDefinition

        assertEquals("ERC20", contract.baseContracts[0].baseName.namePath)
        assertEquals(0, contract.baseContracts[0].arguments.size)
        assertEquals("SafeMath", contract.baseContracts[1].baseName.namePath)
        assertEquals(0, contract.baseContracts[1].arguments.size)

        assertFalse(contract.isLibrary)
        assertEquals("StandardToken", contract.name)
        assertEquals(Visibility.Default, contract.visibility) // @TODO: does this apply?

        assertEquals(7, contract.subNodes.size)
        assertTrue(contract.subNodes[0] is VariableDeclarationStatement)

        val varDecl = contract.subNodes[0] as VariableDeclarationStatement

        assertTrue(varDecl.variables[0] is VariableDeclaration)
        assertEquals(Visibility.Default, varDecl.variables[0].visibility)
        assertFalse(varDecl.variables[0].isConstant)
        assertTrue(varDecl.variables[0].isStateVar)
        assertTrue(varDecl.variables[0].type is Mapping)

        assertTrue(varDecl.variables[0].type is Mapping)
        var mapping = varDecl.variables[0].type as Mapping
        assertEquals(ElementaryTypes.ADDRESS, mapping.keyType)
        assertEquals(ElementaryTypes.UINT, mapping.valueType)

        assertTrue(contract.subNodes[1] is VariableDeclarationStatement)
        assertTrue(contract.subNodes[2] is FunctionDefinition)

        val funcDef = contract.subNodes[2] as FunctionDefinition
        assertEquals("transfer", funcDef.name)
        assertFalse(funcDef.isConstructor)
        assertFalse(funcDef.isDeclaredConst)
        assertFalse(funcDef.isPayable)

        assertTrue(contract.subNodes[3] is FunctionDefinition)
        assertTrue(contract.subNodes[4] is FunctionDefinition)
        assertTrue(contract.subNodes[5] is FunctionDefinition)
        assertTrue(contract.subNodes[6] is FunctionDefinition)
    }
}
