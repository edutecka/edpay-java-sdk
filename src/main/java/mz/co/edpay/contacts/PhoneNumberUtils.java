package mz.co.edpay.contacts;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Normalizes, formats, and validates phone values using configurable country and method rules. */
public final class PhoneNumberUtils {
    private static final Pattern COUNTRY_CODE = Pattern.compile("^[1-9][0-9]{0,2}$");
    private PhoneNumberUtils() {}

    public record Rules(String countryCallingCode, int nationalLength, List<String> nationalPrefixes, List<Integer> groupSizes) {
        public Rules {
            if (countryCallingCode == null || !COUNTRY_CODE.matcher(countryCallingCode.replaceFirst("^\\+", "")).matches()) throw new IllegalArgumentException("Invalid country calling code.");
            countryCallingCode = countryCallingCode.replaceFirst("^\\+", "");
            if (nationalLength < 1 || countryCallingCode.length() + nationalLength > 15) throw new IllegalArgumentException("Invalid national length.");
            nationalPrefixes = nationalPrefixes == null ? List.of() : List.copyOf(nationalPrefixes);
            groupSizes = groupSizes == null ? List.of() : List.copyOf(groupSizes);
            if (nationalPrefixes.stream().anyMatch(prefix -> prefix == null || !prefix.matches("[0-9]+"))) throw new IllegalArgumentException("Prefixes must contain digits only.");
            if (groupSizes.stream().anyMatch(size -> size == null || size < 1)) throw new IllegalArgumentException("Group sizes must be positive.");
        }
        public static Rules mozambique() { return new Rules("258", 9, List.of(), List.of(2, 3, 4)); }
        public static Rules emola() { return new Rules("258", 9, List.of("86", "87"), List.of(2, 3, 4)); }
        public static Rules mpesa() { return new Rules("258", 9, List.of("84", "85"), List.of(2, 3, 4)); }
    }

    /** Phone rules for known phone-based methods; empty means the method does not use a phone or is gateway-specific. */
    public static Optional<Rules> rulesForPaymentMethod(String method) {
        if (method == null) return Optional.empty();
        return switch (canonicalMethod(method)) {
            case "emola" -> Optional.of(Rules.emola());
            case "mpesa" -> Optional.of(Rules.mpesa());
            default -> Optional.empty();
        };
    }

    private static String canonicalMethod(String method) {
        return method.trim().toLowerCase(Locale.ROOT).replaceAll("[ _-]", "");
    }

    public static boolean requiresPhoneForPaymentMethod(String method) {
        return rulesForPaymentMethod(method).isPresent();
    }

    /** Formats the known phone methods and passes non-phone or gateway-specific values through unchanged. */
    public static String normalizeForPaymentMethod(String method, String value) {
        Optional<Rules> rules = rulesForPaymentMethod(method);
        if (rules.isEmpty()) return value;
        String national = normalize(value, rules.get(), false);
        return canonicalMethod(method).equals("mpesa") ? "258" + national : national;
    }

    public static String formatForPaymentMethod(String method, String value) {
        Optional<Rules> rules = rulesForPaymentMethod(method);
        return rules.map(rule -> format(value, rule)).orElse(value);
    }

    /** Unknown and non-phone method identifiers are not interpreted as phone numbers. */
    public static boolean isValidForPaymentMethod(String method, String value) {
        Optional<Rules> rules = rulesForPaymentMethod(method);
        return rules.map(rule -> isValid(value, rule)).orElse(true);
    }

    private record Parsed(String countryCode, String national) {}

    private static Parsed parse(String value, Rules rules) {
        if (rules == null) rules = Rules.mozambique();
        if (value == null) throw new IllegalArgumentException("Phone number is required.");
        String input = value.trim();
        if (input.isBlank() || !input.matches("^\\+?[0-9\\s().-]+$") || input.chars().filter(c -> c == '(').count() != input.chars().filter(c -> c == ')').count()) throw new IllegalArgumentException("Phone number contains unsupported characters.");
        String digits = input.replaceAll("\\D", "");
        boolean international = input.startsWith("+");
        if (digits.startsWith("00")) { digits = digits.substring(2); international = true; }
        if (!international && digits.startsWith(rules.countryCallingCode()) && digits.length() > rules.nationalLength()) international = true;
        if (international) {
            if (!digits.startsWith(rules.countryCallingCode())) throw new IllegalArgumentException("Phone number does not use the configured calling code.");
            digits = digits.substring(rules.countryCallingCode().length());
        }
        if (digits.length() != rules.nationalLength() || !digits.matches("[0-9]+")) throw new IllegalArgumentException("Phone number does not match the configured national length.");
        if (!rules.nationalPrefixes().isEmpty() && rules.nationalPrefixes().stream().noneMatch(digits::startsWith)) throw new IllegalArgumentException("Phone number does not match configured prefixes.");
        return new Parsed(rules.countryCallingCode(), digits);
    }

    public static String normalize(String value, Rules rules, boolean internationalOutput) {
        Parsed phone = parse(value, rules);
        return internationalOutput ? "+" + phone.countryCode() + phone.national() : phone.national();
    }
    public static String normalize(String value) { return normalize(value, Rules.mozambique(), true); }
    public static String normalize(String value, Rules rules) { return normalize(value, rules, true); }
    public static String addCountryPrefix(String value, Rules rules) { return normalize(value, rules, true); }
    public static String addCountryPrefix(String value) { return addCountryPrefix(value, Rules.mozambique()); }
    public static String removeCountryPrefix(String value, Rules rules) { return normalize(value, rules, false); }
    public static String removeCountryPrefix(String value) { return removeCountryPrefix(value, Rules.mozambique()); }
    public static String format(String value, Rules rules) {
        Parsed phone = parse(value, rules);
        rules = rules == null ? Rules.mozambique() : rules;
        List<Integer> groups = rules.groupSizes().isEmpty()
            ? (phone.countryCode().equals("258") && phone.national().length() == 9
                ? List.of(2, 3, 4)
                : List.of(phone.national().length()))
            : rules.groupSizes();
        StringBuilder formatted = new StringBuilder();
        int offset = 0;
        for (int group : groups) {
            if (offset >= phone.national().length()) break;
            if (!formatted.isEmpty()) formatted.append(' ');
            int end = Math.min(offset + group, phone.national().length());
            formatted.append(phone.national(), offset, end);
            offset = end;
        }
        if (offset < phone.national().length()) formatted.append(' ').append(phone.national().substring(offset));
        return "+" + phone.countryCode() + " " + formatted;
    }
    public static String format(String value) { return format(value, Rules.mozambique()); }
    public static boolean isValid(String value, Rules rules) {
        try { parse(value, rules); return true; } catch (IllegalArgumentException exception) { return false; }
    }
    public static boolean isValid(String value) { return isValid(value, Rules.mozambique()); }
}
