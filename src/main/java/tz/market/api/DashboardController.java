package tz.market.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JdbcTemplate jdbc;
    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public Map<String, Object> stats() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("vendors", n("SELECT COUNT(*) FROM vendors WHERE is_deleted=0"));
        r.put("vendorTypes", n("SELECT COUNT(*) FROM vendor_types"));
        r.put("stalls", n("SELECT COUNT(*) FROM stalls WHERE is_deleted=0"));
        r.put("occupied", n("SELECT COUNT(*) FROM stalls WHERE status='Occupied' AND is_deleted=0"));
        r.put("vacant", n("SELECT COUNT(*) FROM stalls WHERE status IN ('Vacant','Available') AND is_deleted=0"));
        r.put("payments", n("SELECT COUNT(*) FROM payments"));
        r.put("revenue", jdbc.queryForObject("SELECT IFNULL(SUM(amount),0) FROM payments WHERE status='Paid'", Number.class));
        r.put("activeLicenses", n("SELECT COUNT(*) FROM licenses WHERE status='Active' AND is_deleted=0"));
        r.put("expiringLicenses", n("SELECT COUNT(*) FROM licenses WHERE status='Active' AND is_deleted=0 AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)"));
        List<Map<String, Object>> monthly = new ArrayList<>(jdbc.queryForList(
            "SELECT DATE_FORMAT(payment_date,'%Y-%m') AS month, SUM(amount) AS total FROM payments "
                + "WHERE status='Paid' GROUP BY month ORDER BY month DESC LIMIT 6"));
        Collections.reverse(monthly);
        r.put("monthlyRevenue", monthly);
        return r;
    }

    private int n(String sql) { Integer v = jdbc.queryForObject(sql, Integer.class); return v == null ? 0 : v; }
}
