package org.solidityj

open class ASTNode {
    val nodeName: String
        get() = this.javaClass.simpleName

    open val childNodes: List<ASTNode>
        get() = emptyList()

    override fun toString(): String {
        return toString(0)
    }

    fun toString(indentation: Int): String {
        val sb = StringBuilder()

        indent(indentation, sb)
        sb.append(nodeName + '\n')

        for (node in childNodes) {
            sb.append(node.toString(indentation + 1))
        }

        return sb.toString()
    }

    private fun indent(indentation: Int, sb: StringBuilder) {
        for (i in 1..indentation) { sb.append("  ") }
    }
}

class SourceUnit(val nodes: List<ASTNode>) : ASTNode() {
    override val childNodes: List<ASTNode>
        get() = nodes
}

class PragmaDirective(val name: String, val value: String) : ASTNode()

enum class Visibility {
    Default, Private, Internal, Public, External
}

open class Declaration(
    val name: String?,
    val visibility: Visibility = Visibility.Default
) : ASTNode()

class ImportDirective(
        unitAlias: String?,
        val path: String
) : Declaration(unitAlias)

class ContractDefinition(
        name: String,
        val baseContracts: List<InheritanceSpecifier>,
        val subNodes: List<ASTNode>,
        val isLibrary: Boolean
): Declaration(name) {
    override val childNodes: List<ASTNode>
        get() = subNodes
}

class InheritanceSpecifier(
        val baseName: UserDefinedTypeName,
        val arguments: List<Expression>
) : ASTNode() {
    override val childNodes: List<ASTNode>
        get() = listOf(baseName) + arguments
}

class UsingForDirective(
        val libraryName: UserDefinedTypeName,
        val typeName: TypeName
) : ASTNode()

class StructDefinition(
        name: String,
        val members: List<VariableDeclaration>
) : Declaration(name) {
    override val childNodes: List<ASTNode>
        get() = members
}

class EnumDefinition(
        name: String,
        val members: List<EnumValue>
) : Declaration(name) {
    override val childNodes: List<ASTNode>
        get() = members
}

class EnumValue(name: String) : Declaration(name)

class ParameterList(
        val parameters: List<VariableDeclaration>
) : ASTNode()

open class CallableDeclaration(
        name: String,
        visibility: Visibility,
        val parameters: ParameterList,
        val returnParameters: ParameterList? = null
): Declaration(name, visibility) {
    override val childNodes: List<ASTNode>
        get() = listOf(parameters)
}

class FunctionDefinition(
        name: String,
        visibility: Visibility,
        parameters: ParameterList,
        returnParameters: ParameterList,
        val modifiers: List<ModifierInvocation>,
        val body: Block?,
        val isConstructor: Boolean = false,
        val isDeclaredConst: Boolean = false,
        val isPayable: Boolean = false
) : CallableDeclaration(name, visibility, parameters, returnParameters) {
    override val childNodes: List<ASTNode>
        get() = super.childNodes + listOfNotNull(body)
}

open class VariableDeclaration(
        val type: TypeName,
        name: String,
        val value: Expression?,
        visibility: Visibility = Visibility.Default,
        val isStateVar: Boolean = false,
        val isIndexed: Boolean = false,
        val isConstant: Boolean = false
) : Declaration(name, visibility) {
    override val childNodes: List<ASTNode>
        get() = listOfNotNull(type, value)
}

class ModifierDefinition(
        name: String,
        parameters: ParameterList,
        val body: Block
): CallableDeclaration(name, Visibility.Default, parameters) {
    override val childNodes: List<ASTNode>
        get() = super.childNodes + listOf(body)
}

class ModifierInvocation(
        val name: String,
        val arguments: List<Expression>
) : ASTNode() {
    override val childNodes: List<ASTNode>
        get() = arguments
}

class EventDefinition(
        name: String,
        parameters: ParameterList,
        val isAnonymous: Boolean
) : CallableDeclaration(name, Visibility.Default, parameters)

class MagicVariableDeclaration(
        name: String,
        val type: TypeName
) : Declaration(name)

open class TypeName : ASTNode() {
    object VAR : TypeName()
}

class ElementaryTypeName(
    val type: String,
    val firstNumber: Int? = null,
    val secondNumber: Int? = null
) : TypeName() {
    override fun toString(): String {
        var str = type
        if (firstNumber != null) {
            str += firstNumber
        }
        if (secondNumber != null) {
            str += "x" + secondNumber
        }
        return str
    }

}

object ElementaryTypes {

    val INT = ElementaryTypeName("int")
    val UINT = ElementaryTypeName("uint")
    val BYTES = ElementaryTypeName("bytes")
    val BYTE = ElementaryTypeName("byte")
    val STRING = ElementaryTypeName("string")
    val ADDRESS = ElementaryTypeName("address")
    val BOOL = ElementaryTypeName("bool")
    val FIXED = ElementaryTypeName("fixed")
    val UFIXED = ElementaryTypeName("ufixed")

    fun intMTypeName(m: Int): ElementaryTypeName { return ElementaryTypeName("int", m) }
    fun uintMTypeName(m: Int): ElementaryTypeName { return ElementaryTypeName("uint", m) }
    fun bytesMTypeName(m: Int): ElementaryTypeName { return ElementaryTypeName("bytes", m) }
    fun fixedMxNTypeName(m: Int, n: Int): ElementaryTypeName { return ElementaryTypeName("uint", m, n) }
    fun ufixedMxNTypeName(m: Int, n: Int): ElementaryTypeName { return ElementaryTypeName("uint", m, n) }
}


class UserDefinedTypeName(val namePath: String) : TypeName()

class FunctionTypeName(
        val parameterTypes: ParameterList,
        val returnTypes: ParameterList,
        val visibility: Visibility,
        val isDeclaredConst: Boolean,
        val isPayable: Boolean
) : TypeName()

class Mapping(
        val keyType: ElementaryTypeName,
        val valueType: TypeName
) : TypeName() {
    override val childNodes: List<ASTNode>
        get() = listOf(keyType, valueType)
}

class ArrayTypeName(
        val baseType: TypeName,
        val length: Expression
) : TypeName()

open class Statement : ASTNode()

class Block(val statements: List<Statement>) : Statement() {
    override val childNodes: List<ASTNode>
        get() = statements
}

class PlaceholderStatement : Statement()

class IfStatement(
        val condition: Expression,
        val trueBody: Statement,
        val falseBody: Statement?
) : Statement() {
    override val childNodes: List<ASTNode>
        get() = listOfNotNull(condition, trueBody, falseBody)
}

abstract class BreakableStatement : Statement()

class WhileStatement(
        val condition: Expression,
        val body: Statement,
        val isDoWhile: Boolean
) : Statement() {
    override val childNodes: List<ASTNode>
        get() = listOf(condition, body)
}

class ForStatement(
        val initExpression: Statement,
        val conditionExpression: Expression,
        val loopExpression: ExpressionStatement,
        val body: Statement
) : BreakableStatement() {
    override val childNodes: List<ASTNode>
        get() = listOf(initExpression, conditionExpression, loopExpression, body)
}

class Continue : Statement()

class Break : Statement()

class Return(val expression: Expression?) : Statement() {
    override val childNodes: List<ASTNode>
        get() = listOfNotNull(expression)
}

class Throw : Statement()

class VariableDeclarationStatement(
        val variables: List<VariableDeclaration>,
        val initialValue: Expression?
) : Statement() {
    override val childNodes: List<ASTNode>
        get() = variables + listOfNotNull(initialValue)
}

class ExpressionStatement(val expression: Expression) : Statement() {
    override val childNodes: List<ASTNode>
        get() = listOf(expression)
}

open class Expression : ASTNode()

class Conditional(
        val condition: Expression,
        val trueExpression: Expression,
        val falseExpression: Expression
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(condition, trueExpression, falseExpression)
}

enum class AssignmentOperator(val op: String) {
    EQ("="), OREQ("|="), XOREQ("^="), ANDEQ("&="), LSHIFTEQ("<<="), RSHIFTEQ(">>="),
    PLUSEQ("+="), MINUSEQ("-="), MULEQ("*="), DIVEQ("/="), MODEQ("%=");

    companion object {
        fun fromToken(token: String): AssignmentOperator {
            return AssignmentOperator.values().find { it.op == token }
                    ?: throw IllegalArgumentException("invalid binary operator")
        }
    }
}

class Assignment(
        val leftHandSide: Expression,
        val operator: AssignmentOperator,
        var rightHandSide: Expression
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(leftHandSide, rightHandSide)
}

class TupleExpression(
        val components: List<Expression>,
        val isArray: Boolean
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = components
}

enum class UnaryOperator(val op: String) {
    NEGATE("!"), DELETE("delete"), INC("++"), DEC("--");

    companion object {
        fun fromToken(token: String): UnaryOperator {
            return UnaryOperator.values().find { it.op == token }
                    ?: throw IllegalArgumentException("invalid unary operator")
        }
    }
}

class UnaryOperation(
        val operator: UnaryOperator,
        val subExpression: Expression,
        val isPrefix: Boolean
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(subExpression)
}

enum class BinaryOperator(val op: String) {
    PLUS("+"), MINUS("-"), MUL("*"), DIV("-");

    companion object {
        fun fromToken(token: String): BinaryOperator {
            return BinaryOperator.values().find { it.op == token }
                    ?: throw IllegalArgumentException("invalid binary operator")
        }
    }
}

class BinaryOperation(
        val left: Expression,
        val operator: BinaryOperator,
        val right: Expression
) : Expression()

class FunctionCall(
        val expression: Expression,
        val arguments: List<Expression>,
        val names: List<String>
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(expression) + arguments
}

class NewExpression(val typeName: TypeName) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(typeName)
}

class MemberAccess(
        val expression: Expression,
        val memberName: String
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(expression)
}

class IndexAccess(
        val base: Expression,
        val index: Expression
) : Expression() {
    override val childNodes: List<ASTNode>
        get() = listOf(base, index)
}

open class PrimaryExpression : Expression()

class Identifier(val name: String) : PrimaryExpression()

class ElementaryTypeNameExpression(val type: ElementaryTypeName) : PrimaryExpression() {
    override val childNodes: List<ASTNode>
        get() = listOf(type)
}

enum class Subdenomination {
    None, Wei, Szabo, Finney, Ether, Second, Minute, Hour, Day, Week, Year
}

open class Literal : PrimaryExpression()

class NumberLiteral(
        val value: String,
        val subdenomination : Subdenomination
) : PrimaryExpression()

class BooleanLiteral(val value: Boolean) : Literal()

class StringLiteral(val value: String) : Literal()

class ArrayLiteral(val value: List<Expression>) : Literal()
