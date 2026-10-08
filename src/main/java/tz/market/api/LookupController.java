package tz.market.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

/** Orodha za dropdown (vendor types, zones, n.k.) zote {id, label}. */
@RestController
@RequestMapping("/api/lookups")
public class LookupController {
    private final JdbcTemplate jdbc;
    public LookupController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Map<String, List<Map<String, Object>>> all() {
        Map<String, List<Map<String, Object>>> r = new LinkedHashMap<>();
        r.put("vendorTypes", jdbc.queryForList("SELECT vendor_type_id AS id, type_name AS label FROM vendor_types ORDER BY type_name"));
        r.put("spaceTypes", jdbc.queryForList("SELECT space_type_id AS id, space_name AS label FROM space_types ORDER BY space_name"));
        r.put("stallTypes", jdbc.queryForList("SELECT stall_type_id AS id, stall_type_name AS label FROM stall_types ORDER BY stall_type_name"));
        r.put("zones", jdbc.queryForList("SELECT zone_id AS id, zone_name AS label FROM zones ORDER BY zone_name"));
        r.put("paymentTypes", jdbc.queryForList("SELECT payment_type_id AS id, payment_name AS label FROM payment_types"));
        r.put("vendors", jdbc.queryForList(
            "SELECT vendor_id AS id, CONCAT_WS(' ', first_name, middle_name, last_name) AS label, status FROM vendors WHERE is_deleted=0 ORDER BY first_name"));
        r.put("stalls", jdbc.queryForList(
            "SELECT stall_id AS id, stall_number AS label, status FROM stalls WHERE is_deleted=0 ORDER BY stall_number"));
        return r;
    }
}
