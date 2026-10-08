package tz.market.api;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Sawa na admin/recycle_bin.php: orodha, rejesha, ondoa kabisa. Admin pekee. */
@RestController
@RequestMapping("/api/recycle-bin")
public class RecycleBinController {
    // module -> {table, pk, label expression}
    private static final Map<String, String[]> TABLES = new LinkedHashMap<>();
    static {
        TABLES.put("vendors", new String[]{"vendors", "vendor_id", "CONCAT_WS(' ', first_name, middle_name, last_name)"});
        TABLES.put("stalls", new String[]{"stalls", "stall_id", "stall_number"});
        TABLES.put("licenses", new String[]{"licenses", "license_id", "license_number"});
        TABLES.put("allocations", new String[]{"allocations", "allocation_id", "CONCAT('Ugawaji #', allocation_id)"});
        TABLES.put("announcements", new String[]{"announcements", "announcement_id", "title"});
    }
    private final JdbcTemplate jdbc;
    public RecycleBinController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestAttribute("user") AuthUser user) {
        admin(user);
        List<Map<String, Object>> out = new ArrayList<>();
        TABLES.forEach((module, t) -> out.addAll(jdbc.queryForList(
            "SELECT '" + module + "' AS module, " + t[1] + " AS id, " + t[2] + " AS label, deleted_at FROM "
                + t[0] + " WHERE is_deleted=1 ORDER BY deleted_at DESC")));
        out.sort((a, b) -> String.valueOf(b.get("deleted_at")).compareTo(String.valueOf(a.get("deleted_at"))));
        return out;
    }

    @PostMapping("/{module}/{id}/restore")
    @Transactional
    public Map<String, Object> restore(@PathVariable String module, @PathVariable long id, @RequestAttribute("user") AuthUser user) {
        admin(user);
        String[] t = table(module);
        if (module.equals("allocations")) {
            List<Map<String, Object>> a = jdbc.queryForList("SELECT stall_id FROM allocations WHERE allocation_id=?", id);
            if (!a.isEmpty()) jdbc.update("UPDATE stalls SET status='Occupied' WHERE stall_id=?", a.get(0).get("stall_id"));
        }
        String extra = (module.equals("announcements") || module.equals("vendors")) ? ", deleted_by=NULL" : "";
        jdbc.update("UPDATE " + t[0] + " SET is_deleted=0, deleted_at=NULL" + extra + " WHERE " + t[1] + "=?", id);
        return Map.of("message", "Imerejeshwa.");
    }

    @DeleteMapping("/{module}/{id}")
    public Map<String, Object> purge(@PathVariable String module, @PathVariable long id, @RequestAttribute("user") AuthUser user) {
        admin(user);
        String[] t = table(module);
        jdbc.update("DELETE FROM " + t[0] + " WHERE " + t[1] + "=? AND is_deleted=1", id);
        return Map.of("message", "Imeondolewa kabisa.");
    }

    private String[] table(String module) {
        String[] t = TABLES.get(module);
        if (t == null) throw new ApiException(404, "Module haipo.");
        return t;
    }
    private void admin(AuthUser u) { if (u.roleId() != 1) throw new ApiException(403, "Admin pekee."); }
}
