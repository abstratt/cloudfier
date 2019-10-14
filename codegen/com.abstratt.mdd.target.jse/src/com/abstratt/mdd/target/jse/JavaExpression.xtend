package com.abstratt.mdd.target.jse


// TODO-RC only use of this miniframework I found was by some dead code in PlainJavaBehaviorGenerator
// should adopt (why?) or delete it - I *think the case here was to generate a temporary Java DOM 
// (which we could reason about, maybe make some simplifications, before rendering the
// actual Java code from there)
//
// Example:   
//    def private JavaExpression generateSafeComparisonExpression(JavaExpression op1, JavaExpression op2, boolean op1Optional, boolean op2Optional, boolean javaPrimitives, CharSequence primitiveComparisonOp) {
//        if (javaPrimitives && !op1Optional && !op2Optional)
//            return new BinaryOperatorExpression(op1, primitiveComparisonOp.toString(), op2) 
//        val core = '''«op1».compareTo(«op2») «primitiveComparisonOp» 0'''
//        if (op1Optional && op2Optional) {
//            new MultipleOperandExpression('&&', #[new BinaryOperatorExpression(op1, '!=', new OpaqueExpression('null')), new OpaqueExpression(core)])
//        } else if (op1Optional) {
//            new BinaryOperatorExpression(
//                new BinaryOperatorExpression(op1, '!=', new OpaqueExpression('null')),
//                '&&',
//                new OpaqueExpression(core)                
//            )
//        } else if (op2Optional) {
//            new BinaryOperatorExpression(
//                new BinaryOperatorExpression(op2, '!=', new OpaqueExpression('null')),
//                '&&',
//                new OpaqueExpression(core)                
//            )
//        } else 
//            new OpaqueExpression(core)
//    }
    

/*


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
*/
