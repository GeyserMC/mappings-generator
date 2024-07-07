package org.geysermc.generator.javaclass;

import java.util.List;
import java.util.Locale;

public class FieldConstructor {
    private final StringBuilder builder = new StringBuilder("public static final ");
    private State state = State.START;
    private int parenthesisLevel = 0;

    public FieldConstructor(String baseClass) {
        builder.append(baseClass);
    }

    public FieldConstructor declareFieldName(String name) {
        assert state == State.START;
        builder.append(" ").append(name.toUpperCase(Locale.ROOT)).append(" = register(new ");
        parenthesisLevel++;
        state = State.WAITING_FOR_CLASS_NAME;
        return this;
    }

    public FieldConstructor declareClassName(String name) {
        assert state == State.WAITING_FOR_CLASS_NAME;
        builder.append(name)
                .append("(");
        parenthesisLevel++;
        state = State.ADD_PARAMETERS;
        return this;
    }

    public FieldConstructor addParameter(String parameter) {
        assert state == State.ADD_PARAMETERS;
        builder.append(parameter).append(", ");
        return this;
    }

    public FieldConstructor addParameter(int parameter) {
        assert state == State.ADD_PARAMETERS;
        builder.append(parameter).append(", ");
        return this;
    }

    public FieldConstructor finishParameters() {
        assert state == State.ADD_PARAMETERS;
        builder.append("builder()");
        state = State.ADD_METHODS;
        return this;
    }

    public FieldConstructor addMethod(String name, String... value) {
        assert state == State.ADD_METHODS;
        builder.append(".")
                .append(name)
                .append("(");
        builder.append(String.join(", ", value));
        builder.append(")");
        return this;
    }

    public FieldConstructor addMethod(String name, int value) {
        assert state == State.ADD_METHODS;
        builder.append(".")
                .append(name)
                .append("(")
                .append(value)
                .append(")");
        return this;
    }

    public FieldConstructor addMethod(String name, float value) {
        assert state == State.ADD_METHODS;
        builder.append(".")
                .append(name)
                .append("(")
                .append(value).append("f") // Note the f! :)
                .append(")");
        return this;
    }

    public FieldConstructor addMethod(String name, double value) {
        assert state == State.ADD_METHODS;
        builder.append(".")
                .append(name)
                .append("(")
                .append(value)
                .append(")");
        return this;
    }

    public FieldConstructor addExtraParameters(List<String> parameters) {
        assert state == State.ADD_METHODS;
        builder.append(", ").append(String.join(", ", parameters));
        return this;
    }

    public FieldConstructor newline() {
        builder.append("\n    ");
        return this;
    }

    public FieldConstructor finish() {
        builder.append(")".repeat(Math.max(0, parenthesisLevel)));
        builder.append(";");
        state = State.FINISH;
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    enum State {
        START,
        WAITING_FOR_CLASS_NAME,
        ADD_PARAMETERS,
        ADD_METHODS,
        FINISH
    }

    static String wrap(String string) {
        return "\"" + string + "\"";
    }
}
