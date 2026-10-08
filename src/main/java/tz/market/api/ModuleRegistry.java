package tz.market.api;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ModuleRegistry {
    private static final String FULL = "CONCAT_WS(' ', v.first_name, v.middle_name, v.last_name)";

    private final Map<String, TableSpec> modules = Map.of(
        "vendors", new TableSpec("vendors", "vendors", "v", "vendor_id",
            "v.*, vt.type_name, sp.space_name, " + FULL + " AS full_name",
            "vendors v LEFT JOIN vendor_types vt ON v.vendor_type_id = vt.vendor_type_id "
                + "LEFT JOIN space_types sp ON v.space_type_id = sp.space_type_id",
            List.of("first_name", "middle_name", "last_name", "national_id", "phone", "address",
                "emergency_contact", "vendor_type_id", "space_type_id", "status"),
            List.of(), List.of("first_name", "last_name", "phone", "vendor_type_id", "space_type_id"),
            List.of(), List.of("v.first_name", "v.last_name", "v.phone", "v.national_id"),
            Map.of("profile_photo", "", "middle_name", "", "address", "", "emergency_contact", "", "national_id", "", "status", "Pending"),
            true, true),

        "stalls", new TableSpec("stalls", "stalls", "s", "stall_id",
            "s.*, z.zone_name, stt.stall_type_name",
            "stalls s LEFT JOIN zones z ON s.zone_id = z.zone_id "
                + "LEFT JOIN stall_types stt ON s.stall_type_id = stt.stall_type_id",
            List.of("stall_number", "zone_id", "stall_type_id", "size", "status"),
            List.of("stall_number"), List.of("stall_number", "zone_id", "stall_type_id", "size"),
            List.of("stall_number"), List.of("s.stall_number", "z.zone_name"),
            Map.of("status", "Vacant"), true, false),

        "allocations", new TableSpec("allocations", "allocations", "a", "allocation_id",
            "a.*, " + FULL + " AS vendor_name, s.stall_number",
            "allocations a LEFT JOIN vendors v ON a.vendor_id = v.vendor_id "
                + "LEFT JOIN stalls s ON a.stall_id = s.stall_id",
            List.of("vendor_id", "stall_id", "allocation_date", "end_date", "status", "remarks"),
            List.of(), List.of("vendor_id", "stall_id", "allocation_date"),
            List.of(), List.of("v.first_name", "v.last_name", "s.stall_number"),
            Map.of("status", "Active", "remarks", ""), true, false),

        "licenses", new TableSpec("licenses", "licenses", "l", "license_id",
            "l.*, " + FULL + " AS vendor_name",
            "licenses l LEFT JOIN vendors v ON l.vendor_id = v.vendor_id",
            List.of("vendor_id", "license_number", "issue_date", "expiry_date", "status"),
            List.of(), List.of("vendor_id", "license_number", "issue_date", "expiry_date"),
            List.of("license_number"), List.of("l.license_number", "v.first_name", "v.last_name"),
            Map.of("status", "Active"), true, false),

        "payments", new TableSpec("payments", "payments", "p", "payment_id",
            "p.*, " + FULL + " AS vendor_name, pt.payment_name",
            "payments p LEFT JOIN vendors v ON p.vendor_id = v.vendor_id "
                + "LEFT JOIN payment_types pt ON p.payment_type_id = pt.payment_type_id",
            List.of("vendor_id", "payment_type_id", "amount", "receipt_number", "payment_date", "status"),
            List.of(), List.of("vendor_id", "payment_type_id", "amount", "receipt_number", "payment_date"),
            List.of("receipt_number"), List.of("p.receipt_number", "v.first_name", "v.last_name"),
            Map.of("status", "Paid"), false, false),

        "announcements", new TableSpec("announcements", "announcements", "an", "announcement_id",
            "an.*, TRIM(CONCAT_WS(' ', u.first_name, u.last_name)) AS author",
            "announcements an LEFT JOIN users u ON an.created_by = u.user_id",
            List.of("title", "message"),
            List.of(), List.of("title", "message"),
            List.of(), List.of("an.title", "an.message"),
            Map.of("image", ""), true, true),

        "vendor-types", new TableSpec("vendor-types", "vendor_types", "vt", "vendor_type_id",
            "vt.*", "vendor_types vt",
            List.of("type_name", "description"),
            List.of(), List.of("type_name"),
            List.of("type_name"), List.of("vt.type_name"),
            Map.of("description", ""), false, false)
    );

    public TableSpec get(String name) {
        TableSpec m = modules.get(name);
        if (m == null) throw new ApiException(404, "TableSpec haipo: " + name);
        return m;
    }
}
