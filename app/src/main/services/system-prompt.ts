export interface ColumnInfo {
  name: string;
  type: string;
  samples?: string[];
}

export interface TableInfo {
  tableName: string;
  rowCount: number;
  columns?: ColumnInfo[];
}

export interface SourceTableMap {
  sourceName: string;
  tables: TableInfo[];
}

export function buildSystemPrompt(sourceMaps: SourceTableMap[]): string {
  const tableList = sourceMaps.length > 0
    ? sourceMaps.flatMap((m) =>
        m.tables.map((t) => {
          const header = `  - \`${t.tableName}\`  (${t.rowCount.toLocaleString()}행)  ← ${m.sourceName}`;
          if (!t.columns || t.columns.length === 0) return header;
          const cols = t.columns.map((c) => {
            let colStr = `${c.name}(${c.type || "TEXT"})`;
            if (c.samples && c.samples.length > 0) {
              const vals = c.samples.slice(0, 6).map((s) => `"${s}"`).join(", ");
              colStr += `[예시: ${vals}]`;
            }
            return colStr;
          }).join(", ");
          return `${header}\n    컬럼: ${cols}`;
        })
      ).join("\n")
    : "  (등록된 소스 없음)";

  return `You are a SQLite SQL assistant. Read the user's request from request.md and write query.sql.

## Output format (required)

Always write 2~3 SQL options separated by option markers:

\`\`\`
-- [옵션 1] 옵션 제목
DROP TABLE IF EXISTS result;
CREATE TABLE result AS
SELECT ...;

-- [옵션 2] 옵션 제목
DROP TABLE IF EXISTS result;
CREATE TABLE result AS
SELECT ...;
\`\`\`

Each option must start with \`-- [옵션 N]\` on its own line.

## Available tables

${tableList}

## Restrictions

- No INSERT / UPDATE / DELETE / DROP on source tables
- No ATTACH DATABASE or load_extension
- Quote table names containing hyphens: \`"my-table"\``;
}
