package com.ithows.aida.shapefile;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * dBASE(.dbf) 속성 테이블 리더. Shapefile의 속성 데이터만 읽는다(지오메트리 제외).
 *
 * <p>인코딩은 .cpg가 없으면 euc-kr 기본(한국 Shapefile 관행).</p>
 */
public final class DbfReader {

    public final List<String> fields = new ArrayList<>();
    private final List<int[]> fieldSpec = new ArrayList<>(); // [offsetInRecord, length]
    public long recordCount;
    private int headerLength;
    private int recordLength;
    private final File dbf;
    private final Charset charset;

    private DbfReader(File dbf, Charset cs) { this.dbf = dbf; this.charset = cs; }

    public static DbfReader open(File shpOrDbf, String encoding) throws Exception {
        File dbf = resolveDbf(shpOrDbf);
        if (!dbf.exists()) throw new Exception("DBF 파일이 없습니다: " + dbf.getPath());
        Charset cs;
        try { cs = Charset.forName(encoding == null || encoding.isEmpty() ? "EUC-KR" : encoding); }
        catch (Exception e) { cs = Charset.forName("EUC-KR"); }
        DbfReader r = new DbfReader(dbf, cs);
        r.readHeader();
        return r;
    }

    private static File resolveDbf(File f) {
        String p = f.getPath();
        if (p.toLowerCase().endsWith(".shp")) p = p.substring(0, p.length() - 4) + ".dbf";
        return new File(p);
    }

    private void readHeader() throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(dbf, "r")) {
            byte[] h = new byte[32];
            raf.readFully(h);
            recordCount = u32(h, 4);
            headerLength = (int) u16(h, 8);
            recordLength = (int) u16(h, 10);

            int offsetInRecord = 1; // 1바이트 삭제 플래그
            long pos = 32;
            while (pos < headerLength - 1) {
                raf.seek(pos);
                byte[] fd = new byte[32];
                int read = raf.read(fd);
                if (read < 1 || (fd[0] & 0xFF) == 0x0D) break;
                StringBuilder name = new StringBuilder();
                for (int i = 0; i < 11; i++) {
                    int c = fd[i] & 0xFF;
                    if (c == 0) break;
                    name.append((char) c);
                }
                int len = fd[16] & 0xFF;
                fields.add(name.toString());
                fieldSpec.add(new int[]{offsetInRecord, len});
                offsetInRecord += len;
                pos += 32;
            }
        }
    }

    /** 앞 limit개 레코드를 읽어 행 목록 반환(limit<=0 이면 전체). */
    public List<Map<String, Object>> readRecords(int limit) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(dbf, "r")) {
            long count = (limit <= 0) ? recordCount : Math.min(limit, recordCount);
            byte[] rec = new byte[recordLength];
            for (long i = 0; i < recordCount && rows.size() < count; i++) {
                raf.seek((long) headerLength + i * recordLength);
                int read = raf.read(rec);
                if (read < recordLength) break;
                if ((rec[0] & 0xFF) == 0x2A) continue; // 삭제 레코드
                Map<String, Object> row = new LinkedHashMap<>();
                for (int fi = 0; fi < fields.size(); fi++) {
                    int[] spec = fieldSpec.get(fi);
                    String val = new String(rec, spec[0], spec[1], charset).trim();
                    row.put(fields.get(fi), val);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static long u16(byte[] b, int o) { return (b[o] & 0xFFL) | ((b[o + 1] & 0xFFL) << 8); }
    private static long u32(byte[] b, int o) {
        return (b[o] & 0xFFL) | ((b[o + 1] & 0xFFL) << 8) | ((b[o + 2] & 0xFFL) << 16) | ((b[o + 3] & 0xFFL) << 24);
    }
}
