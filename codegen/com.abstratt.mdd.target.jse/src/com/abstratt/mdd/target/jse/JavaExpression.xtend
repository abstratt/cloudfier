package com.abstratt.mdd.target.jse

import org.eclipse.xtend.lib.annotations.Data

abstract class JavaExpression {
        
}

class BooleanExpression extends JavaExpression {
    def BooleanExpression negate() {
        return null
    }
}

@Data
class IdentifierExpression extends JavaExpression {
    String identifier
}

@Data
class UnaryOperatorExpression extends JavaExpression {
    String operator
    JavaExpression operand
}

@Data
class BinaryOperatorExpression extends JavaExpression {
    JavaExpression leftOperand
    String operator
    JavaExpression rightOperand
}

@Data
class MultipleOperandExpression extends JavaExpression {
    String operator
    JavaExpression[] operands
}

@Data
class OpaqueExpression extends JavaExpression {
    CharSequence expression
}