package ir.sahab.nexus.plugin.tag.internal;

import com.google.common.annotations.VisibleForTesting;
import ir.sahab.nexus.plugin.tag.internal.dto.AssociatedComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.Validate;

/**
 * Represents a criterion for component search.
 */
public class ComponentSearchCriterion {

    private static final Pattern PATTERN = Pattern.compile("^(?<repository>\\S+):(?<group>\\S+)?:(?<name>\\S+)"
            + "(?<versionexp>(\\s+)(?<operator>=|>=|=<|<|>)(\\s+)(?<version>\\S+))?$");

    private String repository;

    private String group;

    private String name;

    private final Operator versionOperator;

    private final Version version;

    private ComponentSearchCriterion(String repository, String group, String name, Operator versionOperator,
            String versionValue) {
        this.repository = repository;
        this.group = group;
        this.name = name;
        this.versionOperator = versionOperator;
        this.version = new Version(versionValue);
    }

    public String getRepository() {
        return repository;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public Operator getVersionOperator() {
        return versionOperator;
    }

    public String getVersionValue() {
        return version.value;
    }

    /**
     * @param expression expression to parse
     * @return an equivalent version criterion
     * @throws IllegalArgumentException if expression is invalid
     */
    public static ComponentSearchCriterion parse(String expression) {
        Matcher matcher = PATTERN.matcher(expression);
        Validate.isTrue(matcher.matches(), "Invalid component criterion: %s", expression);
        String repository = matcher.group("repository");
        String group = matcher.group("group");
        String name = matcher.group("name");
        Operator versionOperator = null;
        String versionValue = null;
        if (matcher.group("versionexp") != null) {
            versionOperator = Operator.parse(matcher.group("operator"));
            versionValue = matcher.group("version");
        }
        return new ComponentSearchCriterion(repository, group, name, versionOperator, versionValue);
    }

    public boolean matches(AssociatedComponent component) {
        return repository.equals(component.getRepository())
                && Objects.equals(group, component.getGroup())
                && name.equals(component.getName())
                && (versionOperator == null || new Version(component.getVersion()).compare(versionOperator, version));
    }

    enum Operator {
        EQ("="), GT(">"), LT("<"), GTE(">="), LTE("=<");

        private final String expressionString;

        Operator(String expressionString) {
            this.expressionString = expressionString;

        }

        static Operator parse(String value) {
            for (Operator operator : values()) {
                if (operator.expressionString.equals(value)) {
                    return operator;
                }
            }
            throw new IllegalArgumentException("Invalid operator:" + value);
        }

    }

    /**
     * Represents a component version. Currently it supports simple versions composed of multiple integers separated by
     * '.' or '_'.
     */
    @VisibleForTesting
    static class Version {
        private static final Pattern PATTERN = Pattern.compile("^(\\d+)([._]\\d+)*$");

        private final String value;
        private final int[] subversionNumbers;

        public Version(String value) {
            this.value = value;
            subversionNumbers = value != null && PATTERN.matcher(value).matches()
                    ? Arrays.stream(value.split("[._]")).mapToInt(Integer::parseInt).toArray()
                    : null;
        }

        boolean compare(Operator operator, Version other) {
            if (operator != Operator.EQ && (subversionNumbers == null || other.subversionNumbers == null)) {
                return false;
            }
            switch(operator) {
                case EQ:
                    return Objects.equals(value, other.value);
                case GT:
                    return compareTo(other) > 0;
                case LT:
                    return compareTo(other) < 0;
                case GTE:
                    return compareTo(other) >= 0;
                case LTE:
                    return compareTo(other) <= 0;
                default:
                    throw new AssertionError("Unsupported comparison operator: " + this);
            }
        }

        private int compareTo(Version other) {
            for (int i = 0; i < Math.min(subversionNumbers.length, other.subversionNumbers.length); i++) {
                int compare = Integer.compare(subversionNumbers[i], other.subversionNumbers[i]);
                if (compare != 0) {
                    return compare;
                }
            }
            return Integer.compare(subversionNumbers.length, other.subversionNumbers.length);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}