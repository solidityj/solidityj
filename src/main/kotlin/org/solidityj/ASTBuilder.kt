package org.solidityj

import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import java.util.*

class ASTBuilder : SolidityBaseVisitor<ASTNode>() {

    private var contractName: String? = null

    override fun visitEnumDefinition(ctx: SolidityParser.EnumDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val members = ctx.enumValue().map { visit(it) as EnumValue }

        return EnumDefinition(name, members)
    }

    override fun visitUsingForDeclaration(ctx: SolidityParser.UsingForDeclarationContext?): ASTNode {
        val libraryName = UserDefinedTypeName(ctx!!.Identifier().text)
        val typeName = visit(ctx.typeName()) as TypeName

        return UsingForDirective(libraryName, typeName)
    }

    override fun visitEnumValue(ctx: SolidityParser.EnumValueContext?): ASTNode {
        return EnumValue(ctx!!.text)
    }

    override fun visitElementaryTypeNameExpression(ctx: SolidityParser.ElementaryTypeNameExpressionContext?): ASTNode {
        val typeName = visit(ctx!!.elementaryTypeName()) as ElementaryTypeName
        return ElementaryTypeNameExpression(typeName)
    }

    override fun visitAssemblyAssignment(ctx: SolidityParser.AssemblyAssignmentContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBlock(ctx: SolidityParser.BlockContext?): ASTNode {
        val statements = ctx!!.statement().map { visit(it) as Statement }
        return Block(statements)
    }

    override fun visitExpressionStatement(ctx: SolidityParser.ExpressionStatementContext?): ASTNode {
        val expression = visit(ctx!!.expression()) as Expression
        return ExpressionStatement(expression)
    }

    override fun visitDoWhileStatement(ctx: SolidityParser.DoWhileStatementContext?): ASTNode {
        val expression = visit(ctx!!.expression()) as Expression
        val body = visit(ctx.statement()) as Statement
        return WhileStatement(expression, body, true)
    }

    override fun visitMapping(ctx: SolidityParser.MappingContext?): ASTNode {
        val keyType = visit(ctx!!.elementaryTypeName()) as ElementaryTypeName
        val valueType = visit(ctx.typeName()) as TypeName
        return Mapping(keyType, valueType)
    }

    override fun visitAssemblyLocalBinding(ctx: SolidityParser.AssemblyLocalBindingContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitNameValueList(ctx: SolidityParser.NameValueListContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitNewExpression(ctx: SolidityParser.NewExpressionContext?): ASTNode {
        val typeName = visit(ctx!!.typeName()) as TypeName
        return NewExpression(typeName)
    }

    override fun visitModifierDefinition(ctx: SolidityParser.ModifierDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        val parameters = visit(ctx.parameterList()) as ParameterList
        val body = visit(ctx.block()) as Block

        return ModifierDefinition(name, parameters, body)
    }

    override fun visitFunctionTypeName(ctx: SolidityParser.FunctionTypeNameContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitInheritanceSpecifier(ctx: SolidityParser.InheritanceSpecifierContext?): ASTNode {
        val baseName = visit(ctx!!.userDefinedTypeName()) as UserDefinedTypeName
        val arguments = ctx.expression().map { visit(it) as Expression }
        return InheritanceSpecifier(baseName, arguments)
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

        return FunctionCall(expression, arguments, emptyList())
    }

    override fun visitParameterList(ctx: SolidityParser.ParameterListContext?): ASTNode {
        val len = ctx!!.typeName().size
        val parameters = (0 until len).map {
            val type = visit(ctx.typeName(it)) as TypeName
            val name = ctx.Identifier(it).text
            VariableDeclaration(type, name, null)
        }

        return ParameterList(parameters)
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
        return EventDefinition(name, parameters, isAnonymous)
    }

    override fun visitContractDefinition(ctx: SolidityParser.ContractDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier().text
        this.contractName = name

        val baseContracts = ctx.inheritanceSpecifier().map {
            visitInheritanceSpecifier(it) as InheritanceSpecifier }
        val subNodes = ctx.contractPart().map { visitContractPart(it) }
        val isLibrary = ctx.getChild(0).text == "library"

        this.contractName = null

        return ContractDefinition(name, baseContracts, subNodes, isLibrary)
    }

    override fun visitArrayLiteral(ctx: SolidityParser.ArrayLiteralContext?): ASTNode {
        val value = ctx!!.expression().map { visit(it) as Expression }
        return ArrayLiteral(value)
    }

    override fun visitTypeName(ctx: SolidityParser.TypeNameContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitElementaryTypeName(ctx: SolidityParser.ElementaryTypeNameContext?): ASTNode {
        when (ctx!!.text) {
            "int" -> return ElementaryTypes.INT
            "uint" -> return ElementaryTypes.UINT
            "bytes" -> return ElementaryTypes.BYTES
            "byte" -> return ElementaryTypes.BYTE
            "string" -> return ElementaryTypes.STRING
            "address" -> return ElementaryTypes.ADDRESS
            "bool" -> return ElementaryTypes.BOOL
            "fixed" -> return ElementaryTypes.FIXED
            "ufixed" -> return ElementaryTypes.UFIXED
            "var" -> return TypeName.VAR
        }
        throw RuntimeException("variable size elementary types not supported")
    }

    override fun visitInlineAssemblyStatement(ctx: SolidityParser.InlineAssemblyStatementContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitReturnStatement(ctx: SolidityParser.ReturnStatementContext?): ASTNode {
        var expression : Expression? = null
        if (ctx!!.expression() != null) {
            expression = visitExpression(ctx.expression()) as Expression
        }
        return Return(expression)
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

        return StructDefinition(name, members)
    }

    override fun visitPragmaDirective(ctx: SolidityParser.PragmaDirectiveContext?): ASTNode {
        val name = ctx!!.pragmaName().Identifier().text
        val value = ctx.pragmaValue().text

        return PragmaDirective(name, value)
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
                    .map { VariableDeclaration(TypeName.VAR, it.name, null, visibility = Visibility.Default) }
        }

        val initialValue = visit(ctx.expression()) as Expression

        return VariableDeclarationStatement(variables, initialValue)
    }

    override fun visitImportDeclaration(ctx: SolidityParser.ImportDeclarationContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitImportDirective(ctx: SolidityParser.ImportDirectiveContext?): ASTNode {
        val pathString = ctx!!.StringLiteral().text
        return ImportDirective(null, pathString.substring(1, pathString.length - 1))
    }

    override fun visitIndexedParameterList(ctx: SolidityParser.IndexedParameterListContext?): ASTNode {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitPrimaryExpression(ctx: SolidityParser.PrimaryExpressionContext?): ASTNode {
        if (ctx!!.BooleanLiteral() != null) {
            return BooleanLiteral(ctx.text == "true")
        } else if (ctx.HexLiteral() != null) {
            return NumberLiteral(ctx.text, Subdenomination.None)
        } else if (ctx.Identifier() != null) {
            return Identifier(ctx.text)
        } else if (ctx.StringLiteral() != null) {
            val value = ctx.text.substring(1, ctx.text.length - 1)
            return StringLiteral(value)
        } else if (ctx.numberLiteral() != null) {
            return visit(ctx.numberLiteral())
        } else {
            throw UnsupportedOperationException("not implemented")
        }
    }

    override fun visitExpression(ctx: SolidityParser.ExpressionContext?): ASTNode {
        if (ctx!!.childCount == 2) {

            val op = UnaryOperator.fromToken(ctx.getChild(0).text)
            val subExpression = visit(ctx.expression(0)) as Expression

            // @TODO: handle more expressions
            return UnaryOperation(op, subExpression, isPrefix = true)

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

                return BinaryOperation(lhs, op, rhs)

            } else if (isAssignmentOperator(ctx.getChild(1).text)) {
                val op = AssignmentOperator.fromToken(ctx.getChild(1).text)

                val lvalue = visit(ctx.expression(0)) as Expression
                val rvalue = visit(ctx.expression(1)) as Expression

                return Assignment(lvalue, op, rvalue)
            }

            throw RuntimeException("unrecognized expression: " + ctx.text)


        } else if (ctx.childCount == 4) {

            if (ctx.getChild(1).text == "[" && ctx.getChild(3).text == "]") {
                val base = visit(ctx.expression(0)) as Expression
                val index = visit(ctx.expression(1)) as Expression

                return IndexAccess(base, index)
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
        return Throw()
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
        var modifiers = ArrayList<ModifierInvocation>()

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
        var functionCall = visit(ctx) as FunctionCall
        return ModifierInvocation(functionCall.names[0], functionCall.arguments)
    }

    override fun visitFunctionDefinition(ctx: SolidityParser.FunctionDefinitionContext?): ASTNode {
        val name = ctx!!.Identifier(0)?.text ?: ""

        val body : Block?
        if (ctx.block() != null) {
            body = visitBlock(ctx.block()) as Block
        } else {
            body = null
        }

        val visibility = parseVisibility(ctx)
        var returnParameters = parseReturnParameters(ctx)
        val parameters = visit(ctx.parameterList(0)) as ParameterList
        val modifiers = parseModifierInvocations(ctx)
        val isConstructor = name == this.contractName
        val isDeclaredConst = hasSpecifier(ctx, "constant")
        val isPayable = hasSpecifier(ctx, "payable")
        return FunctionDefinition(name, visibility, returnParameters, parameters, modifiers, body, isConstructor , isDeclaredConst, isPayable)
    }

    private fun parseVisibility(ctx: SolidityParser.FunctionDefinitionContext): Visibility {
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

    private fun hasSpecifier(ctx: SolidityParser.FunctionDefinitionContext, specifier: String): Boolean {
        for (child in ctx.children) {
            if (child is TerminalNodeImpl && child.text == specifier) {
                return true
            }
        }
        return false
    }

    override fun visitForStatement(ctx: SolidityParser.ForStatementContext?): ASTNode {
        val initExpression = visit(ctx!!.simpleStatement()) as Statement
        val conditionExpression = visit(ctx.expression(0)) as Expression
        val loopExpression = ExpressionStatement(visit(ctx.expression(1)) as Expression)
        val body = visit(ctx.statement()) as Statement

        return ForStatement(initExpression, conditionExpression, loopExpression, body)
    }

    override fun visitBreakStatement(ctx: SolidityParser.BreakStatementContext?): ASTNode {
        return Break()
    }

    override fun visitStateVariableDeclaration(ctx: SolidityParser.StateVariableDeclarationContext?): ASTNode {
        val type = visit(ctx!!.typeName()) as TypeName
        val name = ctx.Identifier().text
        val expression = if (ctx.expression() != null) visit(ctx.expression()) as Expression else null
        // @TODO: fix visibility, constant, indexed
        val decl = VariableDeclaration(
                type, name, expression,
                visibility = Visibility.Default,
                isStateVar = true,
                isConstant = false,
                isIndexed = false
        )

        return VariableDeclarationStatement(listOf(decl), expression)
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

        return IfStatement(condition, trueBody, falseBody)
    }

    override fun visitVariableDeclaration(ctx: SolidityParser.VariableDeclarationContext?): ASTNode {
        val type = visit(ctx!!.typeName()) as TypeName
        val name = ctx.Identifier().text

        return VariableDeclaration(type, name, null, visibility = Visibility.Default)
    }

    override fun visitWhileStatement(ctx: SolidityParser.WhileStatementContext?): ASTNode {
        val condition = visitExpression(ctx!!.expression()) as Expression
        val body = visitStatement(ctx.statement()) as Statement
        val isDoWhile = ctx.getChild(0).text == "do"

        return WhileStatement(condition, body, isDoWhile)
    }

    override fun visitSimpleStatement(ctx: SolidityParser.SimpleStatementContext?): ASTNode {
        return visit(ctx!!.getChild(0))
    }

    override fun visitPlaceholderStatement(ctx: SolidityParser.PlaceholderStatementContext?): ASTNode {
        return PlaceholderStatement()
    }

    override fun visitContinueStatement(ctx: SolidityParser.ContinueStatementContext?): ASTNode {
        return Continue()
    }

    override fun visitSourceUnit(ctx: SolidityParser.SourceUnitContext?): ASTNode {
        val nodes = ctx!!.children.filter { it !is TerminalNodeImpl }.map { visit(it) }
        return SourceUnit(nodes)
    }

    override fun visitUserDefinedTypeName(ctx: SolidityParser.UserDefinedTypeNameContext?): ASTNode {
        return UserDefinedTypeName(ctx!!.text)
    }

    override fun visitNumberLiteral(ctx: SolidityParser.NumberLiteralContext?): ASTNode {
        // @TODO: fix subdenomination
        return NumberLiteral(ctx!!.text, Subdenomination.None)
    }
}

