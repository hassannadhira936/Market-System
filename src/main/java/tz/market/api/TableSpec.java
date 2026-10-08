package tz.market.api;

import java.util.List;
import java.util.Map;

/** Maelezo ya table moja ya database yako halisi (majina ya columns yanatoka kwenye PHP yako). */
public record TableSpec(
    String name, String table, String alias, String pk,
    String columns, String from,
    List<String> fields,      // columns zinazoruhusiwa kuandikwa
    List<String> createOnly,  // zinaandikwa wakati wa kuongeza tu
    List<String> required,
    List<String> unique,
    List<String> search,
    Map<String, Object> defaults,
    boolean soft,             // is_deleted / deleted_at (recycle bin)
    boolean deletedBy) {}     // table ina deleted_by
