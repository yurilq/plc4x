/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.plc4x.language.java;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.WordUtils;
import org.apache.plc4x.plugins.codegenerator.protocol.freemarker.FreemarkerLanguageTemplateHelper;
import org.apache.plc4x.plugins.codegenerator.types.definitions.*;
import org.apache.plc4x.plugins.codegenerator.types.fields.*;
import org.apache.plc4x.plugins.codegenerator.types.references.*;
import org.apache.plc4x.plugins.codegenerator.types.terms.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "WeakerAccess"})
public class JavaLanguageTemplateHelper implements FreemarkerLanguageTemplateHelper {

    private final Map<String, TypeDefinition> types;

    public JavaLanguageTemplateHelper(Map<String, TypeDefinition> types) {
        this.types = types;
    }

    public String packageName(String protocolName, String languageName, String languageFlavorName) {
        return "org.apache.plc4x." + String.join("", languageName.split("\\-")) + "." +
            String.join("", protocolName.split("\\-")) + "." +
            String.join("", languageFlavorName.split("\\-"));
    }

    public String getLanguageTypeNameForField(TypedField field) {
        boolean optional = field instanceof OptionalField;
        return getLanguageTypeNameForField(field, !optional);
    }

    public String getNonPrimitiveLanguageTypeNameForField(TypedField field) {
        return getLanguageTypeNameForField(field, false);
    }

    private String getLanguageTypeNameForField(TypedField field, boolean allowPrimitive) {
        TypeReference typeReference = field.getType();
        return getLanguageTypeName(typeReference, allowPrimitive);
    }

    public String getLanguageTypeNameForSpecType(TypeReference typeReference) {
        return getLanguageTypeName(typeReference, true);
    }

    public String getLanguageTypeName(TypeReference typeReference, boolean allowPrimitive) {
        if(typeReference instanceof SimpleTypeReference) {
            SimpleTypeReference simpleTypeReference = (SimpleTypeReference) typeReference;
            switch (simpleTypeReference.getBaseType()) {
                case BIT: {
                    return allowPrimitive ? "boolean" : "Boolean";
                }
                case UINT: {
                    IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                    if (integerTypeReference.getSizeInBits() <= 4) {
                        return allowPrimitive ? "byte" : "Byte";
                    }
                    if (integerTypeReference.getSizeInBits() <= 8) {
                        return allowPrimitive ? "short" : "Short";
                    }
                    if (integerTypeReference.getSizeInBits() <= 16) {
                        return allowPrimitive ? "int" : "Integer";
                    }
                    if (integerTypeReference.getSizeInBits() <= 32) {
                        return allowPrimitive ? "long" : "Long";
                    }
                    return "BigInteger";
                }
                case INT: {
                    IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                    if (integerTypeReference.getSizeInBits() <= 8) {
                        return allowPrimitive ? "byte" : "Byte";
                    }
                    if (integerTypeReference.getSizeInBits() <= 16) {
                        return allowPrimitive ? "short" : "Short";
                    }
                    if (integerTypeReference.getSizeInBits() <= 32) {
                        return allowPrimitive ? "int" : "Integer";
                    }
                    if (integerTypeReference.getSizeInBits() <= 64) {
                        return allowPrimitive ? "long" : "Long";
                    }
                    return "BigInteger";
                }
                case FLOAT:
                case UFLOAT: {
                    FloatTypeReference floatTypeReference = (FloatTypeReference) simpleTypeReference;
                    int sizeInBits = ((floatTypeReference.getBaseType() == SimpleTypeReference.SimpleBaseType.FLOAT) ? 1 : 0) +
                        floatTypeReference.getExponent() + floatTypeReference.getMantissa();
                    if (sizeInBits <= 32) {
                        return allowPrimitive ? "float" : "Float";
                    }
                    if (sizeInBits <= 64) {
                        return allowPrimitive ? "double" : "Double";
                    }
                    return "BigDecimal";
                }
                case STRING: {
                    return "String";
                }
                case TIME: {
                    return "LocalTime";
                }
                case DATE: {
                    return "LocalDate";
                }
                case DATETIME: {
                    return "LocalDateTime";
                }
            }
            return "Hurz";
        } else {
            return ((ComplexTypeReference) typeReference).getName();
        }
    }

    public String getNullValueForType(TypeReference typeReference) {
        if(typeReference instanceof SimpleTypeReference) {
            SimpleTypeReference simpleTypeReference = (SimpleTypeReference) typeReference;
            switch (simpleTypeReference.getBaseType()) {
                case BIT: {
                    return "false";
                }
                case UINT: {
                    IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                    if (integerTypeReference.getSizeInBits() <= 16) {
                        return "0";
                    }
                    if (integerTypeReference.getSizeInBits() <= 32) {
                        return "0l";
                    }
                    return "null";
                }
                case INT: {
                    IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                    if (integerTypeReference.getSizeInBits() <= 32) {
                        return "0";
                    }
                    if (integerTypeReference.getSizeInBits() <= 64) {
                        return "0l";
                    }
                    return "null";
                }
                case FLOAT: {
                    FloatTypeReference floatTypeReference = (FloatTypeReference) simpleTypeReference;
                    int sizeInBits = ((floatTypeReference.getBaseType() == SimpleTypeReference.SimpleBaseType.FLOAT) ? 1 : 0) +
                        floatTypeReference.getExponent() + floatTypeReference.getMantissa();
                    if (sizeInBits <= 32) {
                        return "0.0f";
                    }
                    if (sizeInBits <= 64) {
                        return "0.0";
                    }
                    return "null";
                }
                case STRING: {
                    return "null";
                }
            }
            return "Hurz";
        } else {
            return "null";
        }
    }

    public String getArgumentType(TypeReference typeReference, int index) {
        if(typeReference instanceof ComplexTypeReference) {
            ComplexTypeReference complexTypeReference = (ComplexTypeReference) typeReference;
            if(!types.containsKey(complexTypeReference.getName())) {
                throw new RuntimeException("Could not find definition of complex type " + complexTypeReference.getName());
            }
            TypeDefinition complexTypeDefinition = types.get(complexTypeReference.getName());
            if(complexTypeDefinition.getParserArguments().length <= index) {
                throw new RuntimeException("Type " + complexTypeReference.getName() + " specifies too few parser arguments");
            }
            return getLanguageTypeNameForSpecType(complexTypeDefinition.getParserArguments()[index].getType());
        }
        return "Hurz";
    }

    public int getNumBits(SimpleTypeReference simpleTypeReference) {
        switch (simpleTypeReference.getBaseType()) {
            case BIT: {
                return 1;
            }
            case UINT:
            case INT: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                return integerTypeReference.getSizeInBits();
            }
            case FLOAT: {
                FloatTypeReference floatTypeReference = (FloatTypeReference) simpleTypeReference;
                return floatTypeReference.getSizeInBits();
            }
            case STRING: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                return integerTypeReference.getSizeInBits();
            }
            default: {
                return 0;
            }
        }
    }

    public String getReadBufferReadMethodCall(SimpleTypeReference simpleTypeReference) {
        switch (simpleTypeReference.getBaseType()) {
            case BIT: {
                return "io.readBit()";
            }
            case UINT: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                if (integerTypeReference.getSizeInBits() <= 4) {
                    return "io.readUnsignedByte(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 8) {
                    return "io.readUnsignedShort(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 16) {
                    return "io.readUnsignedInt(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 32) {
                    return "io.readUnsignedLong(" + integerTypeReference.getSizeInBits() + ")";
                }
                return "io.readUnsignedBigInteger(" + integerTypeReference.getSizeInBits() + ")";
            }
            case INT: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                if (integerTypeReference.getSizeInBits() <= 8) {
                    return "io.readByte(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 16) {
                    return "io.readShort(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 32) {
                    return "io.readInt(" + integerTypeReference.getSizeInBits() + ")";
                }
                if (integerTypeReference.getSizeInBits() <= 64) {
                    return "io.readLong(" + integerTypeReference.getSizeInBits() + ")";
                }
                return "io.readBigInteger(" + integerTypeReference.getSizeInBits() + ")";
            }
            case FLOAT: {
                FloatTypeReference floatTypeReference = (FloatTypeReference) simpleTypeReference;
                String type = (floatTypeReference.getSizeInBits() <= 32) ? "Float" : "Double";
                String typeCast = (floatTypeReference.getSizeInBits() <= 32) ? "float" : "double";
                String defaultNull = (floatTypeReference.getSizeInBits() <= 32) ? "0.0f" : "0.0";
                StringBuilder sb = new StringBuilder("((Supplier<").append(type).append(">) (() -> {");
                sb.append("\n            try {");
                if (floatTypeReference.getBaseType() == SimpleTypeReference.SimpleBaseType.FLOAT) {
                    sb.append("\n               boolean negative = io.readBit();");
                } else {
                    sb.append("\n               boolean negative = false;");
                }
                sb.append("\n               long exponent = io.readUnsignedLong(").append(floatTypeReference.getExponent()).append(");");
                sb.append("\n               long mantissa = io.readUnsignedLong(").append(floatTypeReference.getMantissa()).append(");");
                sb.append("\n               return (").append(typeCast).append(") ((negative ? -1 : 1) * (0.01 * mantissa) * Math.pow(2, exponent));");
                sb.append("\n            } catch(ParseException e) {");
                sb.append("\n               return ").append(defaultNull).append(";");
                sb.append("\n            }");
                sb.append("\n        })).get()");
                return sb.toString();
            }
            case STRING: {
                StringTypeReference stringTypeReference = (StringTypeReference) simpleTypeReference;
                return "io.readString(" + stringTypeReference.getSizeInBits() + ", \"" +
                    stringTypeReference.getEncoding() + "\")";
            }
        }
        return "Hurz";
    }

    public String getWriteBufferReadMethodCall(SimpleTypeReference simpleTypeReference, String fieldName) {
        switch (simpleTypeReference.getBaseType()) {
            case BIT: {
                return "io.writeBit((boolean) " + fieldName + ")";
            }
            case UINT: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                if (integerTypeReference.getSizeInBits() <= 4) {
                    return "io.writeUnsignedByte(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").byteValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 8) {
                    return "io.writeUnsignedShort(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").shortValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 16) {
                    return "io.writeUnsignedInt(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").intValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 32) {
                    return "io.writeUnsignedLong(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").longValue())";
                }
                return "io.writeUnsignedBigInteger(" + integerTypeReference.getSizeInBits() + ", (BigInteger) " + fieldName + ")";
            }
            case INT: {
                IntegerTypeReference integerTypeReference = (IntegerTypeReference) simpleTypeReference;
                if (integerTypeReference.getSizeInBits() <= 8) {
                    return "io.writeByte(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").byteValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 16) {
                    return "io.writeShort(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").shortValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 32) {
                    return "io.writeInt(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").intValue())";
                }
                if (integerTypeReference.getSizeInBits() <= 64) {
                    return "io.writeLong(" + integerTypeReference.getSizeInBits() + ", ((Number) " + fieldName + ").longValue())";
                }
                return "io.writeBigInteger(" + integerTypeReference.getSizeInBits() + ", BigInteger.valueOf( " + fieldName + "))";
            }
            case FLOAT:
            case UFLOAT: {
                FloatTypeReference floatTypeReference = (FloatTypeReference) simpleTypeReference;
                StringBuilder sb = new StringBuilder();
                if(simpleTypeReference.getBaseType() == SimpleTypeReference.SimpleBaseType.FLOAT) {
                    sb.append("\n        boolean negative = value < 0;");
                    sb.append("\n        io.writeBit(negative);");
                }
                sb.append("\n        final int exponent = Math.getExponent(value);");
                sb.append("\n        final double mantissa = value / Math.pow(2, exponent);");
                sb.append("\n        io.writeInt(").append(floatTypeReference.getExponent()).append(", exponent);");
                sb.append("\n        io.writeDouble(").append(floatTypeReference.getMantissa()).append(", mantissa)");
                return sb.toString().substring(9);
            }
            case STRING: {
                StringTypeReference stringTypeReference = (StringTypeReference) simpleTypeReference;
                return "io.writeString(" + stringTypeReference.getSizeInBits() + ", \"" +
                    stringTypeReference.getEncoding() + "\", (String) " + fieldName + ")";
            }
        }
        return "Hurz";
    }

    public String getReadMethodName(SimpleTypeReference simpleTypeReference) {
        String languageTypeName = getLanguageTypeNameForSpecType(simpleTypeReference);
        languageTypeName = languageTypeName.substring(0, 1).toUpperCase() + languageTypeName.substring(1);
        if(simpleTypeReference.getBaseType().equals(SimpleTypeReference.SimpleBaseType.UINT)) {
            return "readUnsigned" + languageTypeName;
        } else {
            return "read" + languageTypeName;
        }
    }

    public String getReservedValue(ReservedField reservedField) {
        final String languageTypeName = getLanguageTypeName(reservedField.getType(), true);
        if("BigInteger".equals(languageTypeName)) {
            return "BigInteger.valueOf(" + reservedField.getReferenceValue() + ")";
        } else {
            return "(" + languageTypeName + ") " + reservedField.getReferenceValue();
        }
    }

    public Collection<ComplexTypeReference> getComplexTypes(ComplexTypeDefinition complexTypeDefinition) {
        Map<String, ComplexTypeReference> types = new HashMap<>();
        for (Field field : complexTypeDefinition.getFields()) {
            if(field instanceof TypedField) {
                TypedField typedField = (TypedField) field;
                if(typedField.getType() instanceof ComplexTypeReference) {
                    ComplexTypeReference complexTypeReference = (ComplexTypeReference) typedField.getType();
                    types.put(complexTypeReference.getName(),  complexTypeReference);
                }
            } else if(field instanceof SwitchField) {
                SwitchField switchField = (SwitchField) field;
                for (DiscriminatedComplexTypeDefinition cas : switchField.getCases()) {
                    types.put(cas.getName(), new ComplexTypeReference() {
                        @Override
                        public String getName() {
                            return cas.getName();
                        }
                    });
                }
            }
        }
        return types.values();
    }

    public Collection<ComplexTypeReference> getEnumTypes(ComplexTypeDefinition complexTypeDefinition) {
        Map<String, ComplexTypeReference> types = new HashMap<>();
        for (Field field : complexTypeDefinition.getFields()) {
            if(field instanceof EnumField) {
                EnumField enumField = (EnumField) field;
                if(enumField.getType() instanceof ComplexTypeReference) {
                    ComplexTypeReference complexTypeReference = (ComplexTypeReference) enumField.getType();
                    types.put(complexTypeReference.getName(),  complexTypeReference);
                }
            }
        }
        for (Field field : complexTypeDefinition.getParentPropertyFields()) {
            if(field instanceof EnumField) {
                EnumField enumField = (EnumField) field;
                if(enumField.getType() instanceof ComplexTypeReference) {
                    ComplexTypeReference complexTypeReference = (ComplexTypeReference) enumField.getType();
                    types.put(complexTypeReference.getName(),  complexTypeReference);
                }
            }
        }
        return types.values();
    }

    public boolean isSimpleType(TypeReference typeReference) {
        return typeReference instanceof SimpleTypeReference;
    }

    public boolean isDiscriminatedType(TypeDefinition typeDefinition) {
        return typeDefinition instanceof DiscriminatedComplexTypeDefinition;
    }

    public boolean isCountArray(ArrayField arrayField) {
        return arrayField.getLoopType() == ArrayField.LoopType.COUNT;
    }

    public boolean isLengthArray(ArrayField arrayField) {
        return arrayField.getLoopType() == ArrayField.LoopType.LENGTH;
    }

    public boolean isTerminatedArray(ArrayField arrayField) {
        return arrayField.getLoopType() == ArrayField.LoopType.TERMINATED;
    }

    public boolean isCountArray(ManualArrayField arrayField) {
        return arrayField.getLoopType() == ManualArrayField.LoopType.COUNT;
    }

    public boolean isLengthArray(ManualArrayField arrayField) {
        return arrayField.getLoopType() == ManualArrayField.LoopType.LENGTH;
    }

    public boolean isTerminatedArray(ManualArrayField arrayField) {
        return arrayField.getLoopType() == ManualArrayField.LoopType.TERMINATED;
    }

    public String toSwitchExpression(String expression) {
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("([^\\.]*)\\.([a-zA-Z\\d]+)(.*)");
        Matcher matcher;
        while ((matcher = pattern.matcher(expression)).matches()) {
            String prefix = matcher.group(1);
            String middle = matcher.group(2);
            sb.append(prefix).append(".get").append(WordUtils.capitalize(middle)).append("()");
            expression = matcher.group(3);
        }
        sb.append(expression);
        return sb.toString();
    }

    public String toParseExpression(Term term, Argument[] parserArguments) {
        return toExpression(term, term1 -> toVariableParseExpression(term1, parserArguments));
    }

    public String toSerializationExpression(Term term, Argument[] parserArguments) {
        return toExpression(term, term1 -> toVariableSerializationExpression(term1, parserArguments));
    }

    private String toExpression(Term term, Function<Term, String> variableExpressionGenerator) {
        if(term == null) {
            return "";
        }
        if(term instanceof Literal) {
            if(term instanceof NullLiteral) {
                return "null";
            } else if(term instanceof BooleanLiteral) {
                return Boolean.toString(((BooleanLiteral) term).getValue());
            } else if(term instanceof NumericLiteral) {
                return ((NumericLiteral) term).getNumber().toString();
            } else if(term instanceof StringLiteral) {
                return "\"" + ((StringLiteral) term).getValue() + "\"";
            } else if(term instanceof VariableLiteral) {
                VariableLiteral variableLiteral = (VariableLiteral) term;
                // If this literal references an Enum type, then we have to output it differently.
                if(types.get(variableLiteral.getName()) instanceof EnumTypeDefinition) {
                    return variableLiteral.getName() + "." + variableLiteral.getChild().getName();
                } else {
                    return variableExpressionGenerator.apply(term);
                }
            } else {
                throw new RuntimeException("Unsupported Literal type " + term.getClass().getName());
            }
        } else if (term instanceof UnaryTerm) {
            UnaryTerm ut = (UnaryTerm) term;
            Term a = ut.getA();
            switch(ut.getOperation()) {
                case "!":
                    return "!(" + toExpression(a, variableExpressionGenerator) + ")";
                case "-":
                    return "-(" + toExpression(a, variableExpressionGenerator) + ")";
                case "()":
                    return "(" + toExpression(a, variableExpressionGenerator) + ")";
                default:
                    throw new RuntimeException("Unsupported unary operation type " + ut.getOperation());
            }
        } else if (term instanceof BinaryTerm) {
            BinaryTerm bt = (BinaryTerm) term;
            Term a = bt.getA();
            Term b = bt.getB();
            String operation = bt.getOperation();
            switch (operation) {
                case "^":
                    return "Math.pow((" + toExpression(a, variableExpressionGenerator) + "), (" + toExpression(b, variableExpressionGenerator) + "))";
                default:
                    return "(" + toExpression(a, variableExpressionGenerator) + ") " + operation + " (" + toExpression(b, variableExpressionGenerator) + ")";
            }
        } else if (term instanceof TernaryTerm) {
            TernaryTerm tt = (TernaryTerm) term;
            if("if".equals(tt.getOperation())) {
                Term a = tt.getA();
                Term b = tt.getB();
                Term c = tt.getC();
                return "((" +  toExpression(a, variableExpressionGenerator) + ") ? " + toExpression(b, variableExpressionGenerator) + " : " + toExpression(c, variableExpressionGenerator) + ")";
            } else {
                throw new RuntimeException("Unsupported ternary operation type " + tt.getOperation());
            }
        } else {
            throw new RuntimeException("Unsupported Term type " + term.getClass().getName());
        }
    }

    private String toVariableParseExpression(Term term, Argument[] parserArguments) {
        VariableLiteral vl = (VariableLiteral) term;
        // CAST expressions are special as we need to add a ".class" to the second parameter in Java.
        if("CAST".equals(vl.getName())) {
            StringBuilder sb = new StringBuilder(vl.getName());
            if((vl.getArgs() == null) || (vl.getArgs().size() != 2)) {
                throw new RuntimeException("A CAST expression expects exactly two arguments.");
            }
            sb.append("(").append(toVariableParseExpression(vl.getArgs().get(0), parserArguments))
                .append(", ").append(((VariableLiteral) vl.getArgs().get(1)).getName()).append(".class)");
            return sb.toString() + ((vl.getChild() != null) ? "." + toVariableExpressionRest(vl.getChild()) : "");
        }
        else if("STATIC_CALL".equals(vl.getName())) {
            StringBuilder sb = new StringBuilder();
            if(!(vl.getArgs().get(0) instanceof StringLiteral)) {
                throw new RuntimeException("Expecting the first argument of a 'STATIC_CALL' to be a StringLiteral");
            }
            String methodName = ((StringLiteral) vl.getArgs().get(0)).getValue();
            methodName = methodName.substring(1, methodName.length() - 1);
            sb.append(methodName).append("(");
            for(int i = 1; i < vl.getArgs().size(); i++) {
                Term arg = vl.getArgs().get(i);
                if(i > 1) {
                    sb.append(", ");
                }
                if(arg instanceof VariableLiteral) {
                    VariableLiteral va = (VariableLiteral) arg;
                    // "io" is the default name of the reader argument which is always available.
                    boolean isDeserializerArg = "io".equals(va.getName());
                    if(parserArguments != null) {
                        for (Argument parserArgument : parserArguments) {
                            if (parserArgument.getName().equals(va.getName())) {
                                isDeserializerArg = true;
                                break;
                            }
                        }
                    }
                    if(isDeserializerArg) {
                        sb.append(va.getName() + ((va.getChild() != null) ? "." + toVariableExpressionRest(va.getChild()) : ""));
                    } else {
                        sb.append(toVariableParseExpression(va, null));
                    }
                } else if(arg instanceof StringLiteral) {
                    sb.append(((StringLiteral) arg).getValue());
                }
            }
            sb.append(")");
            return sb.toString();
        }
        // All uppercase names are not fields, but utility methods.
        else if(vl.getName().equals(vl.getName().toUpperCase())) {
            StringBuilder sb = new StringBuilder(vl.getName());
            if(vl.getArgs() != null) {
                sb.append("(");
                boolean firstArg = true;
                for(Term arg : vl.getArgs()) {
                    if(!firstArg) {
                        sb.append(", ");
                    }
                    sb.append(toParseExpression(arg, parserArguments));
                    firstArg = false;
                }
                sb.append(")");
            }
            if(vl.getIndex() != VariableLiteral.NO_INDEX) {
                sb.append("[").append(vl.getIndex()).append("]");
            }
            return sb.toString() + ((vl.getChild() != null) ? "." + toVariableExpressionRest(vl.getChild()) : "");
        }
        return vl.getName() + ((vl.getChild() != null) ? "." + toVariableExpressionRest(vl.getChild()) : "");
    }

    private String toVariableSerializationExpression(Term term, Argument[] parserArguments) {
        VariableLiteral vl = (VariableLiteral) term;
        if("STATIC_CALL".equals(vl.getName())) {
            StringBuilder sb = new StringBuilder();
            if(!(vl.getArgs().get(0) instanceof StringLiteral)) {
                throw new RuntimeException("Expecting the first argument of a 'STATIC_CALL' to be a StringLiteral");
            }
            String methodName = ((StringLiteral) vl.getArgs().get(0)).getValue();
            methodName = methodName.substring(1, methodName.length() - 1);
            sb.append(methodName).append("(");
            for(int i = 1; i < vl.getArgs().size(); i++) {
                Term arg = vl.getArgs().get(i);
                if(i > 1) {
                    sb.append(", ");
                }
                if(arg instanceof VariableLiteral) {
                    VariableLiteral va = (VariableLiteral) arg;
                    // "io" and "_value" are always available in every parser.
                    boolean isSerializerArg = "io".equals(va.getName()) || "_value".equals(va.getName()) || "element".equals(va.getName());
                    if(parserArguments != null) {
                        for (Argument parserArgument : parserArguments) {
                            if (parserArgument.getName().equals(va.getName())) {
                                isSerializerArg = true;
                                break;
                            }
                        }
                    }
                    if(isSerializerArg) {
                        sb.append(va.getName() + ((va.getChild() != null) ? "." + toVariableExpressionRest(va.getChild()) : ""));
                    } else {
                        sb.append(toVariableSerializationExpression(va, null));
                    }
                } else if(arg instanceof StringLiteral) {
                    sb.append(((StringLiteral) arg).getValue());
                }
            }
            sb.append(")");
            return sb.toString();
        }
        // All uppercase names are not fields, but utility methods.
        else if(vl.getName().equals(vl.getName().toUpperCase())) {
            StringBuilder sb = new StringBuilder(vl.getName());
            if(vl.getArgs() != null) {
                sb.append("(");
                boolean firstArg = true;
                for(Term arg : vl.getArgs()) {
                    if(!firstArg) {
                        sb.append(", ");
                    }

                    if(arg instanceof VariableLiteral) {
                        VariableLiteral va = (VariableLiteral) arg;
                        boolean isSerializerArg = false;
                        if(parserArguments != null) {
                            for (Argument parserArgument : parserArguments) {
                                if (parserArgument.getName().equals(va.getName())) {
                                    isSerializerArg = true;
                                    break;
                                }
                            }
                        }
                        if(isSerializerArg) {
                            sb.append(va.getName() + ((va.getChild() != null) ? "." + toVariableExpressionRest(va.getChild()) : ""));
                        } else {
                            sb.append(toVariableSerializationExpression(va, null));
                        }
                    } else if(arg instanceof StringLiteral) {
                        sb.append(((StringLiteral) arg).getValue());
                    }
                    firstArg = false;
                }
                sb.append(")");
            }
            return sb.toString();
        }
        boolean isSerializerArg = false;
        if(parserArguments != null) {
            for (Argument parserArgument : parserArguments) {
                if (parserArgument.getName().equals(vl.getName())) {
                    isSerializerArg = true;
                    break;
                }
            }
        }
        if(isSerializerArg) {
            return vl.getName() + ((vl.getChild() != null) ? "." + toVariableExpressionRest(vl.getChild()) : "");
        } else {
            return "_value." + toVariableExpressionRest(vl);
        }
    }

    private String toVariableExpressionRest(VariableLiteral vl) {
        return "get" + WordUtils.capitalize(vl.getName()) + "()" + ((vl.isIndexed() ? "[" + vl.getIndex() + "]" : "") +
            ((vl.getChild() != null) ? "." + toVariableExpressionRest(vl.getChild()) : ""));
    }

    public String getSizeInBits(ComplexTypeDefinition complexTypeDefinition) {
        int sizeInBits = 0;
        StringBuilder sb = new StringBuilder("");
        for (Field field : complexTypeDefinition.getFields()) {
            if(field instanceof ArrayField) {
                ArrayField arrayField = (ArrayField) field;
                final SimpleTypeReference type = (SimpleTypeReference) arrayField.getType();
                switch (arrayField.getLoopType()) {
                    case COUNT:
                        sb.append("(").append(toSerializationExpression(arrayField.getLoopExpression(), null)).append(" * ").append(type.getSizeInBits()).append(") + ");
                        break;
                    case LENGTH:
                        sb.append("(").append(toSerializationExpression(arrayField.getLoopExpression(), null)).append(" * 8) + ");
                        break;
                    case TERMINATED:
                        // No terminated.
                        break;
                }
            } else if(field instanceof TypedField) {
                TypedField typedField = (TypedField) field;
                final TypeReference type = typedField.getType();
                if(field instanceof ManualField) {
                    ManualField manualField = (ManualField) field;
                    sb.append("(").append(toSerializationExpression(manualField.getLengthExpression(), null)).append(") + ");
                }
                else if(type instanceof SimpleTypeReference) {
                    SimpleTypeReference simpleTypeReference = (SimpleTypeReference) type;
                    sizeInBits += simpleTypeReference.getSizeInBits();
                } else {
                    // No ComplexTypeReference supported.
                }
            }
        }
        return sb.toString() + sizeInBits;
    }

    public String escapeValue(TypeReference typeReference, String valueString) {
        if(valueString == null) {
            return null;
        }
        if(typeReference instanceof SimpleTypeReference) {
            SimpleTypeReference simpleTypeReference = (SimpleTypeReference) typeReference;
            switch (simpleTypeReference.getBaseType()) {
                case UINT:
                case INT:
                    // If it's a one character string and is numeric, output it as char.
                    if(!NumberUtils.isParsable(valueString) && (valueString.length() == 1)) {
                        return "'" + valueString + "'";
                    }
                    break;
                case STRING:
                    return "\"" + valueString + "\"";
            }
        }
        return valueString;
    }

    public SimpleTypeReference getEnumBaseType(TypeReference enumType) {
        if(!(enumType instanceof ComplexTypeReference)) {
            throw new RuntimeException("type reference for enum types must be of type complex type");
        }
        ComplexTypeReference complexType = (ComplexTypeReference) enumType;
        EnumTypeDefinition enumTypeDefinition = (EnumTypeDefinition) types.get(complexType.getName());
        return (SimpleTypeReference) enumTypeDefinition.getType();
    }

    public List<Argument> getSerializerArguments(Argument[] arguments) {
        List<Argument> serializerArguments = new LinkedList<>();
        if(arguments != null) {
            for (Argument argument : arguments) {
                if ("lastItem".equals(argument.getName())) {
                    serializerArguments.add(argument);
                }
            }
        }
        return serializerArguments;
    }

    public boolean hasLastItemTerm(Term[] terms) {
        if(terms != null) {
            for (Term term : terms) {
                if (term.contains("lastItem")) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Term> getSerializerTerms(Term[] terms) {
        List<Term> serializerTerms = new LinkedList<>();
        if(terms != null) {
            for (Term term : terms) {
                if (term.contains("lastItem")) {
                    serializerTerms.add(term);
                }
            }
        }
        return serializerTerms;
    }

}
