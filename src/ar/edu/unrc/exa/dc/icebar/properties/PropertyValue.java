package ar.edu.unrc.exa.dc.icebar.properties;

public class PropertyValue {

    private final boolean error;
    private final String value;

    private PropertyValue(String value, boolean error) {
        this.value = value;
        this.error = error;
    }

    public static PropertyValue validValue(String value) {
        return new PropertyValue(value,false);
    }

    public static PropertyValue invalidValue(String value) {
        return new PropertyValue(value, true);
    }

    public static PropertyValue noValue() {
        return new PropertyValue(null,false);
    }

    public String value() {
        return value;
    }

    public boolean error() {
        return error;
    }

    public boolean hasValidValue() {
        return !error && value != null;
    }

}
