package com.ithows.aida;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ithows.aida.model.SourceRef;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON / JSONL 소스 공통 파싱. 기존 schema-inspector.ts / sqlite-loader.ts의 JSON 처리 포팅.
 *
 * <p>Jackson을 사용해 객체 키 <b>삽입 순서를 보존</b>한다(org.json 20171018은 HashMap 기반이라 순서 비결정적).
 * 표 형태로 정규화: 행 목록 + 필드 목록. JSON 객체(비배열)는 key/value 2열로 변환.</p>
 */
public final class JsonSource {

    private static final ObjectMapper M = new ObjectMapper();

    private JsonSource() {}

    public static final class Table {
        public final List<String> fields;
        public final List<Map<String, Object>> rows;
        public final boolean isKeyValue;
        Table(List<String> f, List<Map<String, Object>> r, boolean kv) { fields = f; rows = r; isKeyValue = kv; }
    }

    public static Table readJson(SourceRef s) throws Exception {
        File f = new File(s.configStr("filePath", ""));
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + f.getPath());
        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
        JsonNode node = M.readTree(text);

        String rootPath = s.configStr("rootPath", "");
        if (rootPath != null && !rootPath.isEmpty()) {
            for (String key : rootPath.split("\\.")) {
                node = node.get(key);
                if (node == null) throw new Exception("rootPath 탐색 실패: " + rootPath);
            }
        }

        if (node.isArray()) {
            return arrayToTable(node);
        } else if (node.isObject()) {
            List<String> fields = new ArrayList<>();
            fields.add("key");
            fields.add("value");
            List<Map<String, Object>> rows = new ArrayList<>();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String k = it.next();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("key", k);
                row.put("value", scalar(node.get(k)));
                rows.add(row);
            }
            return new Table(fields, rows, true);
        }
        List<String> fields = new ArrayList<>();
        fields.add("value");
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("value", scalar(node));
        rows.add(row);
        return new Table(fields, rows, false);
    }

    public static Table readJsonl(SourceRef s) throws Exception {
        File f = new File(s.configStr("filePath", ""));
        if (!f.exists()) throw new Exception("파일이 존재하지 않습니다: " + f.getPath());
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        List<JsonNode> nodes = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) nodes.add(M.readTree(t));
        }
        return nodesToTable(nodes);
    }

    private static Table arrayToTable(JsonNode arr) {
        List<JsonNode> nodes = new ArrayList<>();
        for (JsonNode n : arr) nodes.add(n);
        return nodesToTable(nodes);
    }

    private static Table nodesToTable(List<JsonNode> nodes) {
        Set<String> fieldSet = new LinkedHashSet<>();
        for (JsonNode n : nodes) {
            if (n.isObject()) {
                Iterator<String> it = n.fieldNames();
                while (it.hasNext()) fieldSet.add(it.next());
            }
        }
        if (fieldSet.isEmpty()) fieldSet.add("value");
        List<String> fields = new ArrayList<>(fieldSet);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode n : nodes) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (n.isObject()) {
                for (String fld : fields) {
                    JsonNode v = n.get(fld);
                    row.put(fld, v == null || v.isNull() ? null : scalar(v));
                }
            } else {
                row.put("value", scalar(n));
            }
            rows.add(row);
        }
        return new Table(fields, rows, false);
    }

    /** 컨테이너 → JSON 문자열, 그 외 → 텍스트 값. */
    private static Object scalar(JsonNode v) {
        if (v == null || v.isNull()) return null;
        if (v.isObject() || v.isArray()) return v.toString();
        return v.asText();
    }
}
