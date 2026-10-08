package tz.market.api;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** CRUD ya kawaida kwa vendors, stalls, allocations, licenses, payments, announcements, vendor-types. */
@RestController
@RequestMapping("/api/data/{module}")
public class ModuleController {
    private final JdbcTemplate jdbc;
    private final ModuleRegistry registry;

    public ModuleController(JdbcTemplate jdbc, ModuleRegistry registry) { this.jdbc = jdbc; this.registry = registry; }

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable String module, @RequestParam(required = false) String q) {
        TableSpec m = registry.get(module);
        StringBuilder sql = new StringBuilder("SELECT " + m.columns() + " FROM " + m.from() + " WHERE " + base(m));
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank() && !m.search().isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < m.search().size(); i++) {
                sql.append(i > 0 ? " OR " : "").append(m.search().get(i)).append(" LIKE ?");
                args.add("%" + q.trim() + "%");
            }
            sql.append(")");
        }
        sql.append(" ORDER BY ").append(m.alias()).append(".").append(m.pk()).append(" DESC");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable String module, @PathVariable long id) {
        TableSpec m = registry.get(module);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT " + m.columns() + " FROM " + m.from() + " WHERE " + base(m) + " AND " + m.alias() + "." + m.pk() + "=?", id);
        if (rows.isEmpty()) throw new ApiException(404, "Rekodi haijapatikana.");
        return rows.get(0);
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@PathVariable String module, @RequestBody Map<String, Object> body,
                                      @RequestAttribute("user") AuthUser user) {
        TableSpec m = registry.get(module);
        Map<String, Object> data = clean(m, body, true);
        for (String r : m.required())
            if (data.get(r) == null || data.get(r).toString().isBlank())
                throw new ApiException(400, "Sehemu '" + r + "' inahitajika.");
        m.defaults().forEach(data::putIfAbsent);
        if (m.name().equals("announcements")) data.put("created_by", user.userId());
        checkUnique(m, data, null);
        beforeCreate(m, data);

        List<String> cols = new ArrayList<>(data.keySet());
        String sql = "INSERT INTO " + m.table() + " (" + String.join(",", cols) + ") VALUES ("
            + String.join(",", Collections.nCopies(cols.size(), "?")) + ")";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < cols.size(); i++) ps.setObject(i + 1, data.get(cols.get(i)));
            return ps;
        }, kh);
        long id = kh.getKey() == null ? 0 : kh.getKey().longValue();

        // Kama add.php ya allocations: stall inakuwa Occupied
        if (m.name().equals("allocations") && "Active".equals(data.get("status")))
            jdbc.update("UPDATE stalls SET status='Occupied' WHERE stall_id=?", data.get("stall_id"));
        return Map.of("id", id, "message", "Imehifadhiwa kikamilifu.");
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable String module, @PathVariable long id, @RequestBody Map<String, Object> body) {
        TableSpec m = registry.get(module);
        one(module, id);
        Map<String, Object> data = clean(m, body, false);
        if (data.isEmpty()) throw new ApiException(400, "Hakuna mabadiliko.");
        for (String r : m.required())
            if (data.containsKey(r) && (data.get(r) == null || data.get(r).toString().isBlank()))
                throw new ApiException(400, "Sehemu '" + r + "' inahitajika.");
        checkUnique(m, data, id);

        List<String> cols = new ArrayList<>(data.keySet());
        List<Object> args = new ArrayList<>();
        StringBuilder set = new StringBuilder();
        for (String c : cols) { set.append(set.length() > 0 ? "," : "").append(c).append("=?"); args.add(data.get(c)); }
        args.add(id);
        jdbc.update("UPDATE " + m.table() + " SET " + set + " WHERE " + m.pk() + "=?", args.toArray());
        return Map.of("message", "Mabadiliko yamehifadhiwa kikamilifu.");
    }

    /** Futa = kwenda recycle bin (kama PHP). Admin pekee (role_id = 1). */
    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable String module, @PathVariable long id, @RequestAttribute("user") AuthUser user) {
        if (user.roleId() != 1) throw new ApiException(403, "Admin pekee ndiye anaweza kufuta.");
        TableSpec m = registry.get(module);
        Map<String, Object> row = one(module, id);
        if (m.soft()) {
            String sql = "UPDATE " + m.table() + " SET is_deleted=1, deleted_at=NOW()" + (m.deletedBy() ? ", deleted_by=?" : "")
                + " WHERE " + m.pk() + "=?";
            if (m.deletedBy()) jdbc.update(sql, user.userId(), id); else jdbc.update(sql, id);
            // Kama vendors/index.php: akaunti ya user ya vendor pia inafichwa
            if (m.name().equals("vendors") && row.get("user_id") != null)
                jdbc.update("UPDATE users SET is_deleted=1, deleted_at=NOW(), deleted_by=? WHERE user_id=?", user.userId(), row.get("user_id"));
            // Kama delete_allocation.php: stall inarudi Available
            if (m.name().equals("allocations"))
                jdbc.update("UPDATE stalls SET status='Available' WHERE stall_id=?", row.get("stall_id"));
            return Map.of("message", "Imehamishiwa kwenye Recycle Bin.");
        }
        jdbc.update("DELETE FROM " + m.table() + " WHERE " + m.pk() + "=?", id);
        return Map.of("message", "Imefutwa.");
    }

    // ---------------------------------------------------------------- helpers

    private String base(TableSpec m) { return m.soft() ? m.alias() + ".is_deleted=0" : "1=1"; }

    private Map<String, Object> clean(TableSpec m, Map<String, Object> body, boolean creating) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String f : m.fields()) {
            if (!body.containsKey(f)) continue;
            if (!creating && m.createOnly().contains(f)) continue;
            Object v = body.get(f);
            if (v instanceof String s) {
                s = s.trim();
                v = (s.isEmpty() && (f.endsWith("_date") || f.endsWith("_id"))) ? null : s;
            }
            out.put(f, v);
        }
        return out;
    }

    private void checkUnique(TableSpec m, Map<String, Object> data, Long selfId) {
        for (String col : m.unique()) {
            if (!data.containsKey(col)) continue;
            String sql = "SELECT COUNT(*) FROM " + m.table() + " WHERE " + col + "=?" + (selfId != null ? " AND " + m.pk() + "<>?" : "");
            Integer n = selfId != null ? jdbc.queryForObject(sql, Integer.class, data.get(col), selfId)
                                       : jdbc.queryForObject(sql, Integer.class, data.get(col));
            if (n != null && n > 0) throw new ApiException(409, "'" + data.get(col) + "' tayari ipo (" + col + ").");
        }
    }

    /** Sheria za biashara kutoka PHP yako. */
    private void beforeCreate(TableSpec m, Map<String, Object> data) {
        if (m.name().equals("allocations")) {
            Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM allocations WHERE stall_id=? AND status='Active' AND is_deleted=0", Integer.class, data.get("stall_id"));
            if (n != null && n > 0) throw new ApiException(409, "Stall hii tayari ina ugawaji unaoendelea (Active).");
        }
        if (m.name().equals("licenses") && "Active".equals(data.get("status"))) {
            Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM licenses WHERE vendor_id=? AND status='Active' AND is_deleted=0", Integer.class, data.get("vendor_id"));
            if (n != null && n > 0) throw new ApiException(409, "Vendor huyu tayari ana leseni Active.");
        }
    }
}
