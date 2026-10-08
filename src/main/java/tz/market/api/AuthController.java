package tz.market.api;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;
    private final TokenService tokens;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public AuthController(JdbcTemplate jdbc, TokenService tokens) {
        this.jdbc = jdbc;
        this.tokens = tokens;
    }

    /**
     * LOGIN
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");

        if (username.isEmpty() || password.isEmpty()) {
            throw new ApiException(400, "Jaza username na password.");
        }

        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM users WHERE username=? AND is_deleted=0 LIMIT 1",
            username
        );

        if (rows.isEmpty()) {
            throw new ApiException(401, "Username haijapatikana.");
        }

        Map<String, Object> u = rows.get(0);

        String status = String.valueOf(u.get("status"));

        if ("Pending".equalsIgnoreCase(status)) {
            throw new ApiException(
                403,
                "Akaunti yako bado inasubiria kuthibitishwa na Admin."
            );
        }

        if ("Inactive".equalsIgnoreCase(status)) {
            throw new ApiException(
                403,
                "Akaunti yako imezuiliwa. Wasiliana na Admin."
            );
        }

        if ("Blocked".equalsIgnoreCase(status)) {
            throw new ApiException(
                403,
                "Akaunti yako imezuiwa."
            );
        }

        if ("Archived".equalsIgnoreCase(status)) {
            throw new ApiException(
                403,
                "Akaunti hii imehifadhiwa na haiwezi kuingia."
            );
        }

        String stored = String.valueOf(u.get("password"));

        boolean ok =
            password.equals(stored) ||
            bcrypt.matches(password, stored);

        if (!ok) {
            throw new ApiException(401, "Password sio sahihi.");
        }

        int roleId = ((Number) u.get("role_id")).intValue();

        String name =
            (str(u.get("first_name")) + " " + str(u.get("last_name")))
                .trim();

        AuthUser user = new AuthUser(
            ((Number) u.get("user_id")).longValue(),
            roleId,
            str(u.get("username")),
            name
        );

        return Map.of(
            "token", tokens.create(user),
            "user", Map.of(
                "userId", user.userId(),
                "roleId", roleId,
                "username", user.username(),
                "name", name
            )
        );
    }

    /**
     * VENDOR SELF REGISTRATION
     *
     * Hii endpoint haihitaji login.
     *
     * Inatengeneza:
     * 1. users -> Pending
     * 2. vendors -> Pending
     */
    @PostMapping("/vendor-register")
    @Transactional
    public Map<String, Object> vendorRegister(
            @RequestBody Map<String, Object> body
    ) {

        String fullName = str(body.get("fullName")).trim();
        String nationalId = str(body.get("nationalId")).trim();
        String phone = str(body.get("phone")).trim();
        String email = str(body.get("email")).trim();
        String address = str(body.get("address")).trim();
        String businessName = str(body.get("businessName")).trim();
        String businessType = str(body.get("businessType")).trim();
        String emergencyContact = str(body.get("emergencyContact")).trim();
        String username = str(body.get("username")).trim();
        String password = str(body.get("password"));

        if (fullName.isEmpty()) {
            throw new ApiException(400, "Jina kamili linahitajika.");
        }

        if (nationalId.isEmpty()) {
            throw new ApiException(400, "National ID inahitajika.");
        }

        if (phone.isEmpty()) {
            throw new ApiException(400, "Phone Number inahitajika.");
        }

        if (businessName.isEmpty()) {
            throw new ApiException(400, "Business Name inahitajika.");
        }

        if (username.isEmpty()) {
            throw new ApiException(400, "Username inahitajika.");
        }

        if (password.isEmpty()) {
            throw new ApiException(400, "Password inahitajika.");
        }

        if (password.length() < 6) {
            throw new ApiException(
                400,
                "Password lazima iwe na angalau characters 6."
            );
        }

        /*
         * Angalia username kama tayari ipo.
         */
        Integer usernameCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username=?",
            Integer.class,
            username
        );

        if (usernameCount != null && usernameCount > 0) {
            throw new ApiException(
                409,
                "Username hiyo tayari imetumika."
            );
        }

        /*
         * Angalia National ID kama tayari ipo.
         */
        Integer nationalIdCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM vendors WHERE national_id=?",
            Integer.class,
            nationalId
        );

        if (nationalIdCount != null && nationalIdCount > 0) {
            throw new ApiException(
                409,
                "National ID hiyo tayari imesajiliwa."
            );
        }

        /*
         * Gawanya full name kuwa first / middle / last.
         */
        String[] names = fullName.split("\\s+");

        String firstName = names.length >= 1 ? names[0] : "";
        String middleName = "";
        String lastName = "";

        if (names.length == 2) {
            lastName = names[1];
        } else if (names.length >= 3) {

            lastName = names[names.length - 1];

            StringBuilder middle = new StringBuilder();

            for (int i = 1; i < names.length - 1; i++) {
                if (middle.length() > 0) {
                    middle.append(" ");
                }

                middle.append(names[i]);
            }

            middleName = middle.toString();
        }

        /*
         * Vendor type.
         *
         * Frontend ya sasa inatuma jina kama:
         * Vegetables, Fish, Meat, etc.
         *
         * Kama aina haijapatikana, tunatumia vendor type
         * ya kwanza iliyopo kwenye database.
         */
        Long vendorTypeId = findVendorTypeId(businessType);

        /*
         * Space type ya default.
         *
         * Kwa registration ya mwanzo tunatumia
         * Permanent Stall ikiwa ipo.
         *
         * Admin anaweza kuibadilisha baadaye.
         */
        Long spaceTypeId = findDefaultSpaceTypeId();

        /*
         * Tengeneza USER.
         *
         * role_id = 10 = Vendor
         * status = Pending
         */
        String hashedPassword = bcrypt.encode(password);

        jdbc.update(
            """
            INSERT INTO users
                (
                    first_name,
                    middle_name,
                    last_name,
                    phone,
                    username,
                    password,
                    role_id,
                    status
                )
            VALUES
                (?, ?, ?, ?, ?, ?, 10, 'Pending')
            """,
            firstName,
            middleName.isEmpty() ? null : middleName,
            lastName,
            phone,
            username,
            hashedPassword
        );

        Long userId = jdbc.queryForObject(
            "SELECT user_id FROM users WHERE username=? LIMIT 1",
            Long.class,
            username
        );

        if (userId == null) {
            throw new ApiException(
                500,
                "User account haikuweza kutengenezwa."
            );
        }

        /*
         * Tengeneza VENDOR.
         *
         * status = Pending
         */
        jdbc.update(
            """
            INSERT INTO vendors
                (
                    user_id,
                    first_name,
                    middle_name,
                    last_name,
                    national_id,
                    phone,
                    email,
                    address,
                    business_name,
                    emergency_contact,
                    vendor_type_id,
                    space_type_id,
                    status
                )
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')
            """,
            userId,
            firstName,
            middleName.isEmpty() ? null : middleName,
            lastName,
            nationalId,
            phone,
            emptyToNull(email),
            emptyToNull(address),
            businessName,
            emptyToNull(emergencyContact),
            vendorTypeId,
            spaceTypeId
        );

        return Map.of(
            "success", true,
            "message",
            "Usajili umefanikiwa. Taarifa zako zimetumwa kwa Admin kwa ajili ya kuhakikiwa.",
            "status",
            "Pending"
        );
    }

    /**
     * Tafuta vendor type kwa jina.
     */
    private Long findVendorTypeId(String businessType) {

        if (!businessType.isEmpty()) {

            List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT vendor_type_id
                FROM vendor_types
                WHERE LOWER(type_name)=LOWER(?)
                LIMIT 1
                """,
                businessType
            );

            if (!rows.isEmpty()) {
                return ((Number) rows.get(0).get("vendor_type_id"))
                    .longValue();
            }
        }

        /*
         * Kama frontend imetuma type ambayo haipo,
         * tumia type ya kwanza iliyopo.
         */
        Long id = jdbc.queryForObject(
            "SELECT vendor_type_id FROM vendor_types ORDER BY vendor_type_id LIMIT 1",
            Long.class
        );

        if (id == null) {
            throw new ApiException(
                500,
                "Hakuna Vendor Type iliyopo kwenye database."
            );
        }

        return id;
    }

    /**
     * Default space type.
     */
    private Long findDefaultSpaceTypeId() {

        List<Map<String, Object>> rows = jdbc.queryForList(
            """
            SELECT space_type_id
            FROM space_types
            WHERE LOWER(space_name)=LOWER('Permanent Stall')
            LIMIT 1
            """
        );

        if (!rows.isEmpty()) {
            return ((Number) rows.get(0).get("space_type_id"))
                .longValue();
        }

        Long id = jdbc.queryForObject(
            "SELECT space_type_id FROM space_types ORDER BY space_type_id LIMIT 1",
            Long.class
        );

        if (id == null) {
            throw new ApiException(
                500,
                "Hakuna Space Type iliyopo kwenye database."
            );
        }

        return id;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}