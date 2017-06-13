package org.solidityj

import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import java.util.*

class ASTBuilder(val sourceName: String = "") : SolidityBaseVisitor<ASTNode>() {

    private var contractName: String? = null

    private fun withLocation(ctx: ParserRuleContext, node: ASTNode): ASTNode {
        val interval = ctx.sourceInterval
        node.location = SourceLocation(sourceName, interval.a, interval.b)
        return node
    }

    override fun visitEnumDefinition(ctx: SolidityParser.EnumDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val members = ctx.enumValue().map { visit(it) as EnumValue }

        return withLocation(ctx, EnumDefinition(name, members))
    }

    override fun visitUsingForDeclaration(ctx: SolidityParser.UsingForDeclarationContext?): ASTNode {
        val libraryName = UserDefinedTypeName(ctx!!.Identifier().text)
        val typeName = visit(ctx.typeName()) as TypeName

        return withLocation(ctx, UsingForDirective(libraryName, typeName))
    }

    override fun visitEnumValue(ctx: SolidityParser.EnumValueContext?): ASTNode {
        return withLocation(ctx!!, EnumValue(ctx.text))
    }

    override fun visitElementaryTypeNameExpression(ctx: SolidityParser.ElementaryTypeNameExpressionContext?): ASTNode {
        val typeName = visit(ctx!!.elementaryTypeName()) as ElementaryTypeName
        return withLocation(ctx, ElementaryTypeNameExpression(typeName))
    }

    override fun visitAssemblyAssignment(ctx: SolidityParser.AssemblyAssignmentContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBlock(ctx: SolidityParser.BlockContext?): ASTNode {
        val statements = ctx!!.statement().map { visit(it) as Statement }
        return withLocation(ctx, Block(statements))
    }

    override fun visitExpressionStatement(ctx: SolidityParser.ExpressionStatementContext?): ASTNode {
        val expression = visit(ctx!!.expression()) as Expression
        return withLocation(ctx, ExpressionStatement(expression))
    }

    override fun visitDoWhileStatement(ctx: SolidityParser.DoWhileStatementContext?): ASTNode {
        val expression = visit(ctx!!.expression()) as Expression
        val body = visit(ctx.statement()) as Statement
        return withLocation(ctx, WhileStatement(expression, body, true))
    }

    override fun visitMapping(ctx: SolidityParser.MappingContext?): ASTNode {
        val keyType = visit(ctx!!.elementaryTypeName()) as ElementaryTypeName
        val valueType = visit(ctx.typeName()) as TypeName
        return withLocation(ctx, Mapping(keyType, valueType))
    }

    override fun visitAssemblyLocalBinding(ctx: SolidityParser.AssemblyLocalBindingContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitNameValueList(ctx: SolidityParser.NameValueListContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitNewExpression(ctx: SolidityParser.NewExpressionContext?): ASTNode {
        val typeName = visit(ctx!!.typeName()) as TypeName
        return withLocation(ctx, NewExpression(typeName))
    }

    override fun visitModifierDefinition(ctx: SolidityParser.ModifierDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val parameters = visit(ctx.parameterList()) as ParameterList
        val body = visit(ctx.block()) as Block

        return withLocation(ctx, ModifierDefinition(name, parameters, body))
    }

    override fun visitFunctionTypeName(ctx: SolidityParser.FunctionTypeNameContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitInheritanceSpecifier(ctx: SolidityParser.InheritanceSpecifierContext?): ASTNode {
        val baseName = visit(ctx!!.userDefinedTypeName()) as UserDefinedTypeName
        val arguments = ctx.expression().map { visit(it) as Expression }
        return withLocation(ctx, InheritanceSpecifier(baseName, arguments))
    }

    override fun visitFunctionCall(ctx: SolidityParser.FunctionCallContext?): ASTNode {
        val expression : Expression

        if (ctx!!.typeName() != null) {
            val type = visit(ctx.typeName()) as ElementaryTypeName
            expression = ElementaryTypeNameExpression(type)

        } else if (ctx.Identifier(0) != null) {
            val name = ctx.Identifier(0).text
            expression = Identifier(name)
        } else {
            throw UnsupportedOperationException("not implemented")
        }

        val arguments : List<Expression>

        // @TODO: name value pairs too
        if (ctx.functionCallArguments().expressionList() != null) {
            arguments = ctx.functionCallArguments()
                    .expressionList().expression().map { visit(it) as Expression }
        } else {
            arguments = emptyList()
        }

        return withLocation(ctx, FunctionCall(expression, arguments, emptyList()))
    }

    override fun visitParameterList(ctx: SolidityParser.ParameterListContext?): ASTNode {
        val len = ctx!!.typeName().size
        val parameters = (0 until len).map {
            val type = visit(ctx.typeName(it)) as TypeName
            val name = ctx.Identifier(it).text
            VariableDeclaration(type, name, null)
        }

        return withLocation(ctx, ParameterList(parameters))
    }

    override fun visitAssemblyItem(ctx: SolidityParser.AssemblyItemContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFunctionalAssemblyExpression(ctx: SolidityParser.FunctionalAssemblyExpressionContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitEventDefinition(ctx: SolidityParser.EventDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val parameters = ParameterList(ArrayList<VariableDeclaration>())
        val isAnonymous = ctx.children.any { it.text == "anonymous" }
        return withLocation(ctx, EventDefinition(name, parameters, isAnonymous))
    }

    override fun visitContractDefinition(ctx: SolidityParser.ContractDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        this.contractName = name

        val baseContracts = ctx.inheritanceSpecifier().map {
            visitInheritanceSpecifier(it) as InheritanceSpecifier }
        val subNodes = ctx.contractPart().map { visitContractPart(it) }
        val isLibrary = ctx.getChild(0).text == "library"

        this.contractName = null

        return withLocation(ctx, ContractDefinition(name, baseContracts, subNodes, isLibrary))
    }

    override fun visitArrayLiteral(ctx: SolidityParser.ArrayLiteralContext?): ASTNode {
        val value = ctx!!.expression().map { visit(it) as Expression }
        return withLocation(ctx, ArrayLiteral(value))
    }

    override fun visitTypeName(ctx: SolidityParser.TypeNameContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitElementaryTypeName(ctx: SolidityParser.ElementaryTypeNameContext?): ASTNode {
        val type = when (ctx!!.text) {
            "int" -> ElementaryTypes.Int
            "uint" -> ElementaryTypes.Uint
            "bytes" -> ElementaryTypes.Bytes
            "byte" -> ElementaryTypes.Byte
            "string" -> ElementaryTypes.String
            "address" -> ElementaryTypes.Address
            "bool" -> ElementaryTypes.Bool
            "fixed" -> ElementaryTypes.Fixed
            "ufixed" -> ElementaryTypes.Ufixed
            "var" -> TypeName.Var
            else -> throw RuntimeException("variable size elementary types not supported")
        }
        return withLocation(ctx, type)
    }

    override fun visitInlineAssemblyStatement(ctx: SolidityParser.InlineAssemblyStatementContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitReturnStatement(ctx: SolidityParser.ReturnStatementContext?): ASTNode {
        var expression : Expression? = null
        if (ctx!!.expression() != null) {
            expression = visitExpression(ctx.expression()) as Expression
        }
        return withLocation(ctx, Return(expression))
    }

    override fun visitInlineAssemblyBlock(ctx: SolidityParser.InlineAssemblyBlockContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitContractPart(ctx: SolidityParser.ContractPartContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitStructDefinition(ctx: SolidityParser.StructDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val members = ctx.variableDeclaration().map { visit(it) as VariableDeclaration }

        return withLocation(ctx, StructDefinition(name, members))
    }

    override fun visitPragmaDirective(ctx: SolidityParser.PragmaDirectiveContext?): ASTNode {
        val name = ctx!!.pragmaName().Identifier().text
        val value = ctx.pragmaValue().text

        return withLocation(ctx, PragmaDirective(name, value))
    }

    override fun visitStatement(ctx: SolidityParser.StatementContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitVariableDeclarationStatement(ctx: SolidityParser.VariableDeclarationStatementContext?): ASTNode {
        val variables : List<VariableDeclaration>

        if (ctx!!.variableDeclaration() != null) {
            variables = listOf(visit(ctx.variableDeclaration()) as VariableDeclaration)
        } else {
            variables = ctx.identifierList().Identifier()
                    .map { visit(it) as Identifier }
                    .map { VariableDeclaration(TypeName.Var, it.name, null, visibility = Visibility.Default) }
        }

        val initialValue = visit(ctx.expression()) as Expression

        return withLocation(ctx, VariableDeclarationStatement(variables, initialValue))
    }

    override fun visitImportDeclaration(ctx: SolidityParser.ImportDeclarationContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitImportDirective(ctx: SolidityParser.ImportDirectiveContext?): ASTNode {
        val pathString = ctx!!.StringLiteral().text
        return withLocation(ctx, ImportDirective(null, pathString.substring(1, pathString.length - 1)))
    }

    override fun visitIndexedParameterList(ctx: SolidityParser.IndexedParameterListContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitPrimaryExpression(ctx: SolidityParser.PrimaryExpressionContext?): ASTNode {
        val node: ASTNode

        if (ctx!!.BooleanLiteral() != null) {
            node = BooleanLiteral(ctx.text == "true")
        } else if (ctx.HexLiteral() != null) {
            node = NumberLiteral(ctx.text, Subdenomination.None)
        } else if (ctx.Identifier() != null) {
            node = Identifier(ctx.text)
        } else if (ctx.StringLiteral() != null) {
            val value = ctx.text.substring(1, ctx.text.length - 1)
            node = StringLiteral(value)
        } else if (ctx.numberLiteral() != null) {
            node = visit(ctx.numberLiteral())
        } else {
            throw UnsupportedOperationException("not implemented")
        }

        return withLocation(ctx, node)
    }

    override fun visitExpression(ctx: SolidityParser.ExpressionContext?): ASTNode {
        if (ctx!!.childCount == 2) {

            val op = UnaryOperator.fromToken(ctx.getChild(0).text)
            val subExpression = visit(ctx.expression(0)) as Expression

            // @TODO: handle more expressions
            return withLocation(ctx, UnaryOperation(op, subExpression, isPrefix = true))

        } else if (ctx.childCount == 3) {

            if (ctx.getChild(1).text == ".") {
                val expression = visit(ctx.getChild(0)) as Expression
                return MemberAccess(expression, ctx.getChild(2).text)
            }

            if (isBinaryOperator(ctx.getChild(1).text)) {
                val op = BinaryOperator.fromToken(ctx.getChild(1).text)
                // binary expression
                val lhs = visit(ctx.expression(0)) as Expression
                val rhs = visit(ctx.expression(1)) as Expression

                return withLocation(ctx, BinaryOperation(lhs, op, rhs))

            } else if (isAssignmentOperator(ctx.getChild(1).text)) {
                val op = AssignmentOperator.fromToken(ctx.getChild(1).text)

                val lvalue = visit(ctx.expression(0)) as Expression
                val rvalue = visit(ctx.expression(1)) as Expression

                return withLocation(ctx, Assignment(lvalue, op, rvalue))
            }

            throw RuntimeException("unrecognized expression: " + ctx.text)


        } else if (ctx.childCount == 4) {

            if (ctx.getChild(1).text == "[" && ctx.getChild(3).text == "]") {
                val base = visit(ctx.expression(0)) as Expression
                val index = visit(ctx.expression(1)) as Expression

                return withLocation(ctx, IndexAccess(base, index))
            }

            throw RuntimeException("unrecognized expression: " + ctx.text)

        }

        return visit(ctx.getChild(0))
    }

    private fun isAssignmentOperator(text: String?): Boolean {
        return text in AssignmentOperator.values().map { it.op }
    }

    private fun isBinaryOperator(text: String?): Boolean {
        return text in BinaryOperator.values().map { it.op }
    }

    override fun visitThrowStatement(ctx: SolidityParser.ThrowStatementContext?): ASTNode {
        return withLocation(ctx!!, Throw())
    }

    private fun isReturnsToken(ctx: ParseTree): Boolean {
        return ctx is TerminalNodeImpl && ctx.text == "returns"
    }

    private fun parseReturnParameters(ctx: SolidityParser.FunctionDefinitionContext): ParameterList {
        var foundReturn = false
        for (child in ctx.children) {
            if (!foundReturn && isReturnsToken(child)) {
                foundReturn = true
                continue
            }
            if (foundReturn) {
                return visit(child) as ParameterList
            }
        }
        return ParameterList(ArrayList())
    }

    private fun parseModifierInvocations(ctx: SolidityParser.FunctionDefinitionContext): List<ModifierInvocation> {
        val nonModifiers = listOf("constant", "payable", "returns", "private", "public", "internal", "external")
        val modifiers = ArrayList<ModifierInvocation>()

        for (child in ctx.children.subList(3, ctx.childCount)) {
            if (child is TerminalNodeImpl && !(nonModifiers.contains(child.text))) {
                modifiers.add(ModifierInvocation(child.text, ArrayList()))
                continue
            }
            if (child is SolidityParser.FunctionCallContext) {
                modifiers.add(parseModifierInvocation(child))
                continue
            }
        }
        return modifiers
    }

    private fun parseModifierInvocation(ctx: SolidityParser.FunctionCallContext): ModifierInvocation {
        val functionCall = visit(ctx) as FunctionCall
        return withLocation(ctx, ModifierInvocation(functionCall.names[0], functionCall.arguments)) as ModifierInvocation
    }

    override fun visitFunctionDefinition(ctx: SolidityParser.FunctionDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier(0)?.text ?: ""

        val body : Block?
        if (ctx.block() != null) {
            body = visitBlock(ctx.block()) as Block
        } else {
            body = null
        }

        val visibility = parseFunctionVisibility(ctx)
        val returnParameters = parseReturnParameters(ctx)
        val parameters = visit(ctx.parameterList(0)) as ParameterList
        val modifiers = parseModifierInvocations(ctx)
        val isConstructor = name == this.contractName
        val isDeclaredConst = hasSpecifier(ctx, "constant")
        val isPayable = hasSpecifier(ctx, "payable")

        return withLocation(ctx, FunctionDefinition(
            name,
            visibility,
            returnParameters,
            parameters,
            modifiers,
            body,
            isConstructor,
            isDeclaredConst,
            isPayable
        ))
    }

    private fun parseFunctionVisibility(ctx: SolidityParser.FunctionDefinitionContext): Visibility {
        for (child in ctx.children.subList(3, ctx.childCount)) {
            when (child.text) {
                "internal" -> return Visibility.Internal
                "external" -> return Visibility.External
                "public" -> return Visibility.Public
                "private" -> return Visibility.Private
            }
        }
        return Visibility.Default
    }

    private fun hasSpecifier(ctx: ParserRuleContext, specifier: String): Boolean {
        return ctx.children.any { it is TerminalNodeImpl && it.text == specifier }
    }

    override fun visitForStatement(ctx: SolidityParser.ForStatementContext?): ASTNode {
        val initExpression = visit(ctx!!.simpleStatement()) as Statement
        val conditionExpression = visit(ctx.expression(0)) as Expression
        val loopExpression = ExpressionStatement(visit(ctx.expression(1)) as Expression)
        val body = visit(ctx.statement()) as Statement

        return withLocation(ctx, ForStatement(initExpression, conditionExpression, loopExpression, body))
    }

    override fun visitBreakStatement(ctx: SolidityParser.BreakStatementContext?): ASTNode {
        return withLocation(ctx!!, Break())
    }

    private fun parseStateVariableVisibility(ctx: SolidityParser.StateVariableDeclarationContext): Visibility {
        for (child in ctx.children) {
            when (child.text) {
                "internal" -> return Visibility.Internal
                "public" -> return Visibility.Public
                "private" -> return Visibility.Private
            }
        }
        return Visibility.Default
    }

    override fun visitStateVariableDeclaration(ctx: SolidityParser.StateVariableDeclarationContext?): ASTNode {
        val type = visit(ctx!!.typeName()) as TypeName
        val name = ctx.Identifier().text
        val expression = if (ctx.expression() != null) visit(ctx.expression()) as Expression else null

        val decl = VariableDeclaration(
                type, name, expression,
                visibility = parseStateVariableVisibility(ctx),
                isStateVar = true,
                isConstant = hasSpecifier(ctx, "constant"),
                isIndexed = false
        )

        return withLocation(ctx, VariableDeclarationStatement(listOf(decl), expression))
    }

    override fun visitIfStatement(ctx: SolidityParser.IfStatementContext?): ASTNode {
        val condition = visitExpression(ctx!!.expression()) as Expression
        val trueBody = visitStatement(ctx.statement(0)) as Statement

        val falseBody : Statement?
        if (ctx.statement().size > 1) {
            falseBody = visitStatement(ctx.statement(1)) as Statement
        } else {
            falseBody = null
        }

        return withLocation(ctx, IfStatement(condition, trueBody, falseBody))
    }

    override fun visitVariableDeclaration(ctx: SolidityParser.VariableDeclarationContext?): ASTNode {
        val type = visit(ctx!!.typeName()) as TypeName
        val name = ctx.Identifier().text

        return withLocation(ctx, VariableDeclaration(type, name, null, visibility = Visibility.Default))
    }

    override fun visitWhileStatement(ctx: SolidityParser.WhileStatementContext?): ASTNode {
        val condition = visitExpression(ctx!!.expression()) as Expression
        val body = visitStatement(ctx.statement()) as Statement
        val isDoWhile = ctx.getChild(0).text == "do"

        return withLocation(ctx, WhileStatement(condition, body, isDoWhile))
    }

    override fun visitSimpleStatement(ctx: SolidityParser.SimpleStatementContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitPlaceholderStatement(ctx: SolidityParser.PlaceholderStatementContext?): ASTNode {
        return withLocation(ctx!!, PlaceholderStatement())
    }

    override fun visitContinueStatement(ctx: SolidityParser.ContinueStatementContext?): ASTNode {
        return withLocation(ctx!!, Continue())
    }

    override fun visitSourceUnit(ctx: SolidityParser.SourceUnitContext?): ASTNode {
        val nodes = ctx!!.children.filter { it !is TerminalNodeImpl }.map { visit(it) }
        return withLocation(ctx, SourceUnit(nodes))
    }

    override fun visitUserDefinedTypeName(ctx: SolidityParser.UserDefinedTypeNameContext?): ASTNode {
        return withLocation(ctx!!, UserDefinedTypeName(ctx.text))
    }

    override fun visitNumberLiteral(ctx: SolidityParser.NumberLiteralContext?): ASTNode {
        val number = ctx!!.getChild(0).text
        var subdenomination = Subdenomination.None
        if (ctx.childCount == 2) {
            subdenomination = Subdenomination.fromToken(ctx.getChild(1).text)
        }
        return withLocation(ctx, NumberLiteral(number, subdenomination))
    }
}


fun parseCode(string: String): ASTNode {
    val input = ANTLRInputStream(string)
    val lexer = SolidityLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = SolidityParser(tokens)
    val builder = ASTBuilder()

    return builder.visit(parser.sourceUnit())
}
