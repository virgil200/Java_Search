import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PublicRecordsSearchServer
 *
 * A small Java-only public-records due-diligence portal using the JDK's built-in
 * HttpServer. It intentionally does NOT scrape websites, bypass access controls,
 * collect private identifiers, or claim that a person has a criminal record. It
 * returns direct links to official/public sources where a human reviewer can
 * verify records lawfully.
 */
public class PublicRecordsSearchServer {
    private static final Path PUBLIC_DIR = Paths.get("public").toAbsolutePath().normalize();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<Source> SOURCES = List.of(
            // Philippines - courts / case law
            new Source("ph-sc-jurisprudence", "PH", "court", "Philippine Supreme Court Jurisprudence",
                    "Official Supreme Court website for decisions and jurisprudence.",
                    "https://sc.judiciary.gov.ph/jurisprudence/", "official", "manual",
                    "Search by case title, docket/G.R. number, party name, or keywords. Supreme Court decisions are not a complete criminal-record check."),
            new Source("ph-elibrary", "PH", "court", "Supreme Court E-Library",
                    "Judiciary e-library for laws, decisions, rules, and legal references.",
                    "https://elibrary.judiciary.gov.ph/", "official", "manual",
                    "Full search may require authorized access. Use for legal research, not as a sole background-check source."),
            new Source("ph-lawphil", "PH", "court", "LawPhil Project",
                    "Free Philippine legal database operated by Arellano Law Foundation; useful for jurisprudence cross-checking.",
                    "https://lawphil.net/search.html", "public legal repository", "manual",
                    "Secondary repository. Verify important findings against official court records."),

            // Philippines - business
            new Source("ph-sec-efast", "PH", "business", "Philippines SEC eFAST Public Search",
                    "Public search for SEC-registered corporations and filings where available.",
                    "https://fast.sec.gov.ph/search", "official", "manual",
                    "Search exact or partial company name/registration number. Corporate records may require fees for certified copies."),
            new Source("ph-sec-express", "PH", "business", "SEC Express System",
                    "Official SEC document request service for certified company records.",
                    "https://secexpress.ph/", "official", "manual",
                    "Use for formal due diligence and certified copies; fees may apply."),
            new Source("ph-dti-bnrs", "PH", "business", "DTI Business Name Registration Search",
                    "Business Name Registration System search for sole proprietorship business names.",
                    "https://bnrs.dti.gov.ph/search", "official", "manual",
                    "DTI business-name registration is not the same as a mayor's permit, tax registration, or proof of legitimacy."),

            // Philippines - credentials / professional licenses
            new Source("ph-prc", "PH", "credentials", "PRC License Verification",
                    "Official Professional Regulation Commission verification for licensed professionals.",
                    "https://verification.prc.gov.ph/", "official", "manual",
                    "Verify profession, full name, license number, and status when legally permitted."),
            new Source("ph-ched", "PH", "credentials", "CHED Higher Education Institution Directory",
                    "Commission on Higher Education resources for checking recognized institutions.",
                    "https://ched.gov.ph/", "official", "manual",
                    "Degree confirmation normally requires the person's consent and direct verification with the school registrar."),

            // United States - courts / case law
            new Source("us-courtlistener", "US", "court", "CourtListener Search",
                    "Free Law Project search for U.S. court opinions, RECAP dockets, judges, and legal data.",
                    "https://www.courtlistener.com/search/?q={q}", "public legal repository", "query",
                    "Not every criminal case or sealed/expunged matter appears. Verify with the relevant court."),
            new Source("us-pacer", "US", "court", "PACER Federal Court Records",
                    "Official U.S. federal court records access system.",
                    "https://pacer.uscourts.gov/", "official", "manual",
                    "Account and fees may be required. Federal records only; state and local records are separate."),
            new Source("us-state-courts", "US", "court", "State Court Websites Directory",
                    "National Center for State Courts directory for state court record portals.",
                    "https://www.ncsc.org/information-and-resources/state-court-websites", "official directory", "manual",
                    "Use the correct state/county court portal. Rules and public access vary by jurisdiction."),

            // United States - business
            new Source("us-sec-edgar", "US", "business", "SEC EDGAR Company Search",
                    "Official SEC filing search for public companies and regulated filings.",
                    "https://www.sec.gov/edgar/search/#/q={q}", "official", "query",
                    "Best for public companies and SEC-regulated filings; not all private businesses file with EDGAR."),
            new Source("us-sos-directory", "US", "business", "U.S. State Corporate Registration Directory",
                    "National Association of Secretaries of State directory for state business registries.",
                    "https://www.nass.org/business-services/corporate-registration", "official directory", "manual",
                    "Search the Secretary of State registry for the entity's formation state."),

            // United States - credentials
            new Source("us-clearinghouse", "US", "credentials", "National Student Clearinghouse DegreeVerify",
                    "Common U.S. service for education verification.",
                    "https://www.studentclearinghouse.org/verify/", "credential verification service", "consent",
                    "Usually requires permissible purpose and/or consent. Do not infer a false credential from absence of a public profile."),
            new Source("us-state-license", "US", "credentials", "U.S. Professional License Boards",
                    "Many U.S. professions are verified by state licensing boards.",
                    "https://www.usa.gov/professional-licenses", "official directory", "manual",
                    "Use the exact board for the profession and state; verify name variants and license numbers."),

            // Global business / public profiles
            new Source("global-opencorporates", "GLOBAL", "business", "OpenCorporates Company Search",
                    "Large open database of companies across many jurisdictions.",
                    "https://opencorporates.com/companies?q={q}", "public data aggregator", "query",
                    "Useful for discovery, but verify important results with the source registry."),
            new Source("global-orcid", "GLOBAL", "credentials", "ORCID Public Researcher Profiles",
                    "Public researcher IDs and self-maintained academic/professional profiles.",
                    "https://orcid.org/orcid-search/search?searchQuery={q}", "public profile registry", "query",
                    "Public profile data is not the same as verified school credentials."),
            new Source("global-crossref", "GLOBAL", "credentials", "Crossref Metadata Search",
                    "Search public scholarly-publication metadata.",
                    "https://search.crossref.org/?q={q}", "public publication metadata", "query",
                    "Publication metadata can help corroborate academic work but does not verify degrees."),
            new Source("global-sanctions", "GLOBAL", "web", "OpenSanctions Search",
                    "Open database that consolidates sanctions, PEP, and watchlist data from public sources.",
                    "https://www.opensanctions.org/search/?q={q}", "public data aggregator", "query",
                    "High-risk compliance data requires careful identity matching and official-source verification."),
            new Source("global-wikidata", "GLOBAL", "web", "Wikidata Search",
                    "Structured open knowledge-base search for notable public entities and people.",
                    "https://www.wikidata.org/w/index.php?search={q}", "open knowledge base", "query",
                    "Useful for context only; not an official credential or court-record source.")
    );

    public static void main(String[] args) throws IOException {
        int port = determinePort(args);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/sources", new SourcesHandler());
        server.createContext("/health", exchange -> sendJson(exchange, 200,
                "{\"status\":\"ok\",\"time\":\"" + jsonEscape(Instant.now().toString()) + "\"}"));
        server.setExecutor(null);
        System.out.println("Public records due-diligence portal running at http://localhost:" + port);
        System.out.println("Press Ctrl+C to stop.");
        server.start();
    }

    private static int determinePort(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // Fall through to env/default.
            }
        }
        String fromEnv = System.getenv("PORT");
        if (fromEnv != null && !fromEnv.isBlank()) {
            try {
                return Integer.parseInt(fromEnv.trim());
            } catch (NumberFormatException ignored) {
                // Fall through.
            }
        }
        return 8080;
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCommonHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed", "text/plain; charset=utf-8");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            Path requested = PUBLIC_DIR.resolve(path.substring(1)).normalize();
            if (!requested.startsWith(PUBLIC_DIR) || Files.isDirectory(requested) || !Files.exists(requested)) {
                sendText(exchange, 404, "Not found", "text/plain; charset=utf-8");
                return;
            }
            String contentType = contentTypeFor(requested);
            byte[] body = Files.readAllBytes(requested);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCommonHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"POST required\"}");
                return;
            }

            Map<String, List<String>> form = parseForm(readRequestBody(exchange));
            String name = value(form, "name");
            String country = value(form, "country").toUpperCase(Locale.ROOT);
            String purpose = value(form, "purpose");
            Set<String> scopes = new LinkedHashSet<>(form.getOrDefault("scope", List.of()));
            boolean consent = "yes".equalsIgnoreCase(value(form, "consent"));
            boolean notMinor = "yes".equalsIgnoreCase(value(form, "notMinor"));
            boolean fairUse = "yes".equalsIgnoreCase(value(form, "fairUse"));

            List<String> errors = validate(name, country, purpose, scopes, consent, notMinor, fairUse);
            if (!errors.isEmpty()) {
                sendJson(exchange, 422, errorJson(errors));
                return;
            }

            List<Map<String, String>> resultCards = findSources(name, country, scopes);
            String response = searchJson(name, country, purpose, scopes, resultCards);
            sendJson(exchange, 200, response);
        }
    }

    static class SourcesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCommonHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"GET required\"}");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("{\"sources\":[");
            for (int i = 0; i < SOURCES.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(SOURCES.get(i).toJson(null));
            }
            sb.append("]}");
            sendJson(exchange, 200, sb.toString());
        }
    }

    private static List<String> validate(String name, String country, String purpose, Set<String> scopes,
                                         boolean consent, boolean notMinor, boolean fairUse) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.trim().length() < 3) {
            errors.add("Enter at least 3 characters for a person or entity name.");
        }
        if (name != null && name.length() > 120) {
            errors.add("Name/entity query is too long. Use a shorter search phrase.");
        }
        if (name != null && looksLikeSensitiveIdentifier(name)) {
            errors.add("Do not enter SSNs, passport numbers, bank numbers, passwords, or other sensitive private identifiers.");
        }
        if (!Set.of("PH", "US", "GLOBAL").contains(country)) {
            errors.add("Choose a supported jurisdiction: Philippines, United States, or Global.");
        }
        if (scopes.isEmpty()) {
            errors.add("Select at least one search category.");
        }
        Set<String> allowedScopes = Set.of("court", "business", "credentials", "web");
        for (String scope : scopes) {
            if (!allowedScopes.contains(scope)) {
                errors.add("Unsupported scope: " + scope);
            }
        }
        if (purpose == null || purpose.trim().length() < 8) {
            errors.add("Describe the lawful purpose briefly, for example: vendor due diligence, self-check, or consented employment verification.");
        }
        if (!consent) {
            errors.add("Confirm you have the person's consent or another lawful, legitimate purpose.");
        }
        if (!notMinor) {
            errors.add("This prototype does not support searches about minors.");
        }
        if (!fairUse) {
            errors.add("Agree to use results only for lawful verification and to confirm findings with official sources.");
        }
        return errors;
    }

    private static boolean looksLikeSensitiveIdentifier(String text) {
        String compact = text.replaceAll("[\\s-]", "");
        if (compact.matches(".*\\d{9,}.*")) return true;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("ssn") || lower.contains("passport") || lower.contains("password")
                || lower.contains("bank account") || lower.contains("credit card");
    }

    private static List<Map<String, String>> findSources(String query, String country, Set<String> scopes) {
        return SOURCES.stream()
                .filter(source -> scopes.contains(source.category))
                .filter(source -> source.country.equals("GLOBAL") || country.equals("GLOBAL") && source.country.equals("GLOBAL") || source.country.equals(country))
                .map(source -> source.asMap(query))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String searchJson(String name, String country, String purpose, Set<String> scopes,
                                     List<Map<String, String>> results) {
        String requestId = generateRequestId();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendField(sb, "requestId", requestId).append(',');
        appendField(sb, "searchedAt", Instant.now().toString()).append(',');
        appendField(sb, "query", name.trim()).append(',');
        appendField(sb, "country", country).append(',');
        appendField(sb, "purpose", purpose.trim()).append(',');
        sb.append("\"scopes\":[");
        int scopeIndex = 0;
        for (String scope : scopes) {
            if (scopeIndex++ > 0) sb.append(',');
            sb.append('"').append(jsonEscape(scope)).append('"');
        }
        sb.append("],");
        appendField(sb, "importantNotice", "This portal provides source links and discovery leads only. It does not determine guilt, identity, eligibility, school attendance, or business legitimacy. Verify matches with official records and follow local privacy, employment, credit-reporting, and anti-discrimination laws.").append(',');
        sb.append("\"warnings\":[");
        List<String> warnings = warningsFor(scopes);
        for (int i = 0; i < warnings.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(jsonEscape(warnings.get(i))).append('"');
        }
        sb.append("],");
        sb.append("\"results\":[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(mapToJson(results.get(i)));
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private static List<String> warningsFor(Set<String> scopes) {
        List<String> warnings = new ArrayList<>();
        if (scopes.contains("court")) {
            warnings.add("Court and criminal-case indexes are incomplete and jurisdiction-specific. A name match is not proof of identity or guilt.");
        }
        if (scopes.contains("credentials")) {
            warnings.add("Degree and credential checks usually require consent and confirmation from the issuing school, board, or registrar.");
        }
        if (scopes.contains("business")) {
            warnings.add("Business registry presence does not prove trustworthiness. Check permits, tax registration, licensing, sanctions, litigation, and current status.");
        }
        if (scopes.contains("web")) {
            warnings.add("Open-web and aggregator data can contain false positives. Use multiple identifiers and official-source confirmation.");
        }
        warnings.add("Do not use this tool to harass, stalk, doxx, discriminate, or bypass paid/controlled official access systems.");
        return warnings;
    }

    private static String errorJson(List<String> errors) {
        StringBuilder sb = new StringBuilder("{\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(jsonEscape(errors.get(i))).append('"');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (i++ > 0) sb.append(',');
            appendField(sb, entry.getKey(), entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static StringBuilder appendField(StringBuilder sb, String key, String value) {
        sb.append('"').append(jsonEscape(key)).append("\":\"").append(jsonEscape(value == null ? "" : value)).append('"');
        return sb;
    }

    private static String generateRequestId() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder("prs-");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static Map<String, List<String>> parseForm(String body) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return values;
        for (String pair : body.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals >= 0 ? pair.substring(0, equals) : pair;
            String value = equals >= 0 ? pair.substring(equals + 1) : "";
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        return values;
    }

    private static String value(Map<String, List<String>> form, String key) {
        List<String> values = form.get(key);
        if (values == null || values.isEmpty()) return "";
        return values.get(values.size() - 1).trim();
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static void addCommonHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Cache-Control", "no-store");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static void sendText(HttpExchange exchange, int statusCode, String text, String contentType) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String contentTypeFor(Path path) {
        String file = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (file.endsWith(".html")) return "text/html; charset=utf-8";
        if (file.endsWith(".css")) return "text/css; charset=utf-8";
        if (file.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (file.endsWith(".svg")) return "image/svg+xml";
        if (file.endsWith(".json")) return "application/json; charset=utf-8";
        return "application/octet-stream";
    }

    private static String jsonEscape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    static class Source {
        final String id;
        final String country;
        final String category;
        final String title;
        final String description;
        final String urlTemplate;
        final String reliability;
        final String mode;
        final String notes;

        Source(String id, String country, String category, String title, String description,
               String urlTemplate, String reliability, String mode, String notes) {
            this.id = id;
            this.country = country;
            this.category = category;
            this.title = title;
            this.description = description;
            this.urlTemplate = urlTemplate;
            this.reliability = reliability;
            this.mode = mode;
            this.notes = notes;
        }

        Map<String, String> asMap(String query) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("country", country);
            map.put("category", category);
            map.put("title", title);
            map.put("description", description);
            map.put("reliability", reliability);
            map.put("mode", mode);
            map.put("url", urlFor(query));
            map.put("notes", notes);
            map.put("nextStep", nextStepForMode(mode));
            return map;
        }

        String toJson(String query) {
            return mapToJson(asMap(query == null ? "" : query));
        }

        private String urlFor(String query) {
            String encoded = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8);
            return urlTemplate.replace("{q}", encoded);
        }

        private static String nextStepForMode(String mode) {
            if ("query".equals(mode)) {
                return "Open the link, review possible matches, and confirm identity with multiple facts before relying on a result.";
            }
            if ("consent".equals(mode)) {
                return "Use only with consent/permissible purpose, then request official verification from the listed service or institution.";
            }
            return "Open the source and search manually using exact names, aliases, case numbers, license numbers, or entity registration numbers.";
        }
    }
}
